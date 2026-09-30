package com.naze.motion.core.engine

import com.naze.motion.core.action.ActionDispatcher
import com.naze.motion.core.action.AutomationDriver
import com.naze.motion.core.action.TargetResolver
import com.naze.motion.core.domain.Action
import com.naze.motion.core.domain.ActionPlan
import com.naze.motion.core.domain.ActionResult
import com.naze.motion.core.domain.AgentCancellationToken
import com.naze.motion.core.domain.AgentError
import com.naze.motion.core.domain.ErrorCode
import com.naze.motion.core.domain.ExecutionContext
import com.naze.motion.core.domain.ExecutionProfile
import com.naze.motion.core.domain.Observation
import com.naze.motion.core.domain.TargetApplication
import com.naze.motion.core.domain.VerificationResult
import com.naze.motion.core.domain.VerificationType
import com.naze.motion.core.state.AgentState
import com.naze.motion.core.state.AgentStateMachine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Phase 5 execution engine: observe, execute, verify, recover.
 * Runs a validated ActionPlan against an AutomationDriver while driving the
 * agent state machine through its legal transitions only. Cancellation is
 * checked before every action, every attempt, and every recovery step.
 * Every step emits a structured log event (NMA-ACTION-011).
 *
 * Preconditions: the state machine must be in READY (planning and
 * validation are done by the caller) and the driver must be connected.
 */
class ExecutionEngine(
    private val dispatcher: ActionDispatcher = ActionDispatcher.withDefaultHandlers(),
    private val recoveryManager: RecoveryManager? = null,
    private val logger: EngineLog = EngineLog(),
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val profile: ExecutionProfile = ExecutionProfile(),
) {

    /**
     * Effective recovery manager: an injected manager wins (tests inject
     * deterministic doubles); otherwise the safety profile drives the
     bounded recovery attempts and backoff (Phase 22).
     */
    private val recovery: RecoveryManager = recoveryManager
        ?: RecoveryManager(
            maxAttempts = profile.recoveryMaxAttempts,
            backoffMs = { attempt -> profile.recoveryBackoffMs(attempt) },
        )

    fun log(): EngineLog = logger

    suspend fun execute(
        taskId: String,
        plan: ActionPlan,
        driver: AutomationDriver,
        resolver: TargetResolver,
        target: TargetApplication,
        cancellationToken: AgentCancellationToken,
        stateMachine: AgentStateMachine = AgentStateMachine(AgentState.READY),
    ): ExecutionSummary {
        val startedAtMs = clock()
        val observationSource = DriverObservationSource(driver)
        val completed = mutableListOf<String>()
        var previousObservation: Observation? = null

        if (stateMachine.current != AgentState.READY) {
            val message = "engine requires READY state but found " + stateMachine.current
            logger.error("EXECUTION_REJECTED", message, taskId)
            return rejected(taskId, startedAtMs, completed, ErrorCode.ILLEGAL_STATE_TRANSITION, message)
        }
        if (!driver.isConnected()) {
            logger.error("EXECUTION_FAILED", "driver disconnected before start", taskId,
                detail = mapOf("code" to ErrorCode.ACCESSIBILITY_DISCONNECTED.name))
            return rejected(taskId, startedAtMs, completed,
                ErrorCode.ACCESSIBILITY_DISCONNECTED, "driver disconnected before start")
        }
        if (cancellationToken.isCancelled) {
            return cancelled(taskId, stateMachine, startedAtMs, completed, "cancelled before start")
        }

        logger.info("EXECUTION_STARTED", "executing plan with " + plan.actions.size + " actions", taskId,
            detail = mapOf("targetPackage" to target.packageName, "actionCount" to plan.actions.size.toString()))

        for (action in plan.actions) {
            if (cancellationToken.isCancelled) {
                return cancelled(taskId, stateMachine, startedAtMs, completed,
                    "cancelled before action " + action.id)
            }
            alignToExecuting(stateMachine)
            logger.info("ACTION_STARTED", action.type.name + " " + action.id, taskId, action.id)

            val outcome = executeWithAttempts(action, driver, resolver, taskId, completed, cancellationToken)
            if (outcome is ActionResult.Failure) {
                if (outcome.error.code == ErrorCode.CANCELLED) {
                    return cancelled(taskId, stateMachine, startedAtMs, completed,
                        "cancelled during action " + action.id)
                }
                return failed(taskId, stateMachine, startedAtMs, completed, action.id, outcome.error)
            }
            completed.add(action.id)
            logger.info("ACTION_COMPLETED", "action " + action.id + " succeeded", taskId, action.id)

            stateMachine.transitionTo(AgentState.OBSERVING)
            logger.debug("OBSERVATION_CAPTURED", "observing screen state", taskId, action.id)
            val observation = observationSource.observe(target)
            stateMachine.transitionTo(AgentState.VERIFYING)

            val verification = if (observation == null) {
                VerificationResult(VerificationType.ACTION_COMPLETED, false,
                    "observation unavailable: no accessibility tree")
            } else {
                val prev = previousObservation
                previousObservation = observation
                Verifier.verify(action.verification, observation, prev)
            }
            if (verification.passed) {
                logger.info("VERIFICATION_PASSED", "type " + verification.type.name, taskId, action.id)
                continue
            }
            logger.warning("VERIFICATION_FAILED", verification.reason ?: "verification failed", taskId, action.id)

            stateMachine.transitionTo(AgentState.RECOVERING)
            logger.info("RECOVERY_STARTED", "bounded recovery for action " + action.id, taskId, action.id)
            val recovery = recovery.attempt(
                isCancelled = { cancellationToken.isCancelled },
                recheck = {
                    if (cancellationToken.isCancelled) {
                        VerificationResult(VerificationType.ACTION_COMPLETED, false, "cancelled during recovery")
                    } else {
                        val fresh = observationSource.observe(target)
                        val prev = previousObservation
                        previousObservation = fresh
                        if (fresh == null) {
                            VerificationResult(VerificationType.ACTION_COMPLETED, false,
                                "observation unavailable during recovery")
                        } else {
                            Verifier.verify(action.verification, fresh, prev)
                        }
                    }
                },
            )
            if (cancellationToken.isCancelled) {
                return cancelled(taskId, stateMachine, startedAtMs, completed,
                    "cancelled during recovery of action " + action.id)
            }
            if (recovery.succeeded) {
                logger.info("RECOVERY_COMPLETED",
                    "recovered after " + recovery.attemptsUsed + " attempt(s)", taskId, action.id)
                continue
            }
            return failed(taskId, stateMachine, startedAtMs, completed, action.id,
                AgentError(ErrorCode.RECOVERY_EXHAUSTED, recovery.lastReason ?: "recovery exhausted"))
        }

        alignToVerifying(stateMachine)
        stateMachine.transitionTo(AgentState.COMPLETED)
        logger.info("EXECUTION_COMPLETED", "plan completed", taskId)
        return ExecutionSummary(ExecutionOutcome.COMPLETED, taskId, completed, null, null,
            clock() - startedAtMs, logger.snapshot)
    }

    /**
     * Executes one action honoring its RetryPolicy and timeout.
     * Only transient error codes consume retry attempts; permanent
     * errors fail fast (NMA-DOMAIN-004).
     */
    private suspend fun executeWithAttempts(
        action: Action,
        driver: AutomationDriver,
        resolver: TargetResolver,
        taskId: String,
        completedActionIds: List<String>,
        cancellationToken: AgentCancellationToken,
    ): ActionResult {
        // Phase 22: the safety profile caps retries; the action policy
        // can never push the run above the configured limit.
        val maxAttempts = profile.attemptsCapFor(action)
        var lastFailure: ActionResult.Failure? = null
        for (attemptNo in 1..maxAttempts) {
            if (cancellationToken.isCancelled) {
                return ActionResult.Failure(action.id,
                    AgentError(ErrorCode.CANCELLED, "cancelled before attempt " + attemptNo), attemptNo, 0L)
            }
            val attemptStart = clock()
            val context = ExecutionContext(taskId, cancellationToken, attemptStart, completedActionIds)
            val timeoutMs = profile.timeoutCapFor(action)
            val dispatched = withTimeoutOrNull(timeoutMs) {
                dispatcher.dispatch(action, driver, resolver, context)
            }
            val result: ActionResult = when {
                dispatched == null -> ActionResult.Failure(action.id,
                    AgentError(ErrorCode.TIMEOUT, "action exceeded timeout " + timeoutMs + " ms"),
                    attemptNo, clock() - attemptStart)
                dispatched.isFailure -> {
                    val dispatchError =
                        (dispatched.exceptionOrNull() as? ActionDispatcher.DispatchException)?.error
                            ?: AgentError(ErrorCode.INTERNAL_ERROR,
                                dispatched.exceptionOrNull()?.message ?: "dispatch failed")
                    ActionResult.Failure(action.id, dispatchError, attemptNo, clock() - attemptStart)
                }
                else -> dispatched.getOrThrow()
            }
            if (result is ActionResult.Success) return result
            lastFailure = result as ActionResult.Failure
            logger.warning("ACTION_ATTEMPT_FAILED", result.error.message, taskId, action.id,
                mapOf("code" to result.error.code.name, "attempt" to attemptNo.toString()))
            if (result.error.code !in RETRYABLE) return result
            if (attemptNo < maxAttempts) {
                logger.info("ACTION_RETRYING", "retrying attempt " + (attemptNo + 1), taskId, action.id)
            }
        }
        return lastFailure ?: ActionResult.Failure(action.id,
            AgentError(ErrorCode.INTERNAL_ERROR, "no attempts executed"), maxAttempts, 0L)
    }

    /** Walks the machine back into EXECUTING through legal transitions only. */
    private fun alignToExecuting(machine: AgentStateMachine) {
        when (machine.current) {
            AgentState.READY -> machine.transitionTo(AgentState.EXECUTING)
            AgentState.VERIFYING -> machine.transitionTo(AgentState.EXECUTING)
            AgentState.RECOVERING -> {
                machine.transitionTo(AgentState.OBSERVING)
                machine.transitionTo(AgentState.VERIFYING)
                machine.transitionTo(AgentState.EXECUTING)
            }
            else -> {}
        }
    }

    /** Walks the machine into VERIFYING before the plan end transition. */
    private fun alignToVerifying(machine: AgentStateMachine) {
        when (machine.current) {
            AgentState.RECOVERING -> {
                machine.transitionTo(AgentState.OBSERVING)
                machine.transitionTo(AgentState.VERIFYING)
            }
            AgentState.OBSERVING -> machine.transitionTo(AgentState.VERIFYING)
            AgentState.READY, AgentState.EXECUTING -> {
                machine.transitionTo(AgentState.OBSERVING)
                machine.transitionTo(AgentState.VERIFYING)
            }
            else -> {}
        }
    }

    private fun rejected(
        taskId: String,
        startedAtMs: Long,
        completed: List<String>,
        code: ErrorCode,
        message: String,
    ): ExecutionSummary {
        // The machine stays untouched: there is no legal transition from its
        // current state to FAILED for these preconditions.
        return ExecutionSummary(ExecutionOutcome.FAILED, taskId, completed, null,
            AgentError(code, message), clock() - startedAtMs, logger.snapshot)
    }

    private fun cancelled(
        taskId: String,
        machine: AgentStateMachine,
        startedAtMs: Long,
        completed: List<String>,
        reason: String,
    ): ExecutionSummary {
        machine.transitionTo(AgentState
.CANCELLED)
        logger.warning("EXECUTION_CANCELLED", reason, taskId)
        return ExecutionSummary(ExecutionOutcome.CANCELLED, taskId, completed, null,
            AgentError(ErrorCode.CANCELLED, reason), clock() - startedAtMs, logger.snapshot)
    }

    private fun failed(
        taskId: String,
        machine: AgentStateMachine,
        startedAtMs: Long,
        completed: List<String>,
        actionId: String,
        error: AgentError,
    ): ExecutionSummary {
        machine.transitionTo(AgentState.FAILED)
        logger.error("EXECUTION_FAILED", error.message, taskId, actionId,
            mapOf("code" to error.code.name))
        return ExecutionSummary(ExecutionOutcome.FAILED, taskId, completed, actionId,
            error, clock() - startedAtMs, logger.snapshot)
    }

    companion object {
        /** Transient codes that may consume a retry attempt. */
        private val RETRYABLE = setOf(
            ErrorCode.TARGET_NOT_FOUND,
            ErrorCode.TIMEOUT,
            ErrorCode.PACKAGE_NOT_ACTIVE,
            ErrorCode.INTERNAL_ERROR,
        )
    }
}
