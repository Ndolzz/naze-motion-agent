package com.naze.motion.core.agent

import com.naze.motion.core.action.AutomationDriver
import com.naze.motion.core.action.TargetResolver
import com.naze.motion.core.adapter.TargetApplicationAdapter
import com.naze.motion.core.ai.AiPlanner
import com.naze.motion.core.ai.PlanningError
import com.naze.motion.core.ai.PlanningRequest
import com.naze.motion.core.domain.ActionPlan
import com.naze.motion.core.domain.AgentCancellationToken
import com.naze.motion.core.domain.TargetApplication
import com.naze.motion.core.engine.ExecutionEngine
import com.naze.motion.core.engine.ExecutionOutcome

/**
 * MotionAgent (Phase 11): the end to end orchestrator. Chains planner ->
 * target adapter -> execution engine into a single typed run. The agent
 * itself stays dumb: it owns no Android types and no app specific logic, so
 * the full pipeline is testable on the JVM with fake doubles (NMA-ARCH-007,
 * NMA-TEST-002). The optional onPlan callback lets the caller surface the
 * validated plan (for a live timeline) before execution starts.
 */
class MotionAgent(
    private val planner: AiPlanner,
    private val adapter: TargetApplicationAdapter,
    private val engine: ExecutionEngine = ExecutionEngine(),
    private val nowMs: () -> Long = System::currentTimeMillis,
) {

    /** Structured engine log; attach a listener before run for UI streaming. */
    fun log() = engine.log()

    suspend fun run(
        instruction: String,
        driver: AutomationDriver,
        resolver: TargetResolver,
        cancellationToken: AgentCancellationToken,
        onPlan: ((ActionPlan) -> Unit)? = null,
        observationSummary: String = "",
    ): AgentResult {
        val request = try {
            PlanningRequest(instruction.trim(), adapter.packageName, observationSummary)
        } catch (e: IllegalArgumentException) {
            return AgentResult.InvalidInstruction(e.message ?: "invalid instruction")
        }

        val plan = planner.plan(request)
        if (plan.isFailure) {
            val error = plan.exceptionOrNull() as? PlanningError
                ?: PlanningError.ProviderError("planning failed")
            return AgentResult.PlanningFailed(error)
        }

        val validatedPlan = plan.getOrThrow()
        onPlan?.invoke(validatedPlan)

        if (!adapter.open()) {
            return AgentResult.TargetUnavailable(adapter.packageName)
        }

        val taskId = "task-" + nowMs()
        val target = TargetApplication(adapter.packageName, adapter.displayName)
        val summary = engine.execute(
            taskId,
            validatedPlan,
            driver,
            resolver,
            target,
            cancellationToken,
        )
        return when (summary.outcome) {
            ExecutionOutcome.COMPLETED -> AgentResult.Completed(summary)
            ExecutionOutcome.FAILED -> AgentResult.Failed(summary)
            ExecutionOutcome.CANCELLED -> AgentResult.Cancelled(summary)
        }
    }
}
