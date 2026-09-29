# DOMAIN SPEC

## Purpose
Define the stable domain entities and their invariants.

## Scope
Phase 2 implementation in `core/domain`.

## Requirements
- NMA-DOMAIN-001: The system SHALL represent a unit of work as `AgentTask`
  (id, userInstruction, actionPlan, status, createdAt, executionResult).
- NMA-DOMAIN-002: The system SHALL represent every automation command as a
  validated structured `Action` (id, type, target, parameters, timeoutMs,
  retryPolicy, verification).
- NMA-DOMAIN-003: `Action.timeoutMs` SHALL be > 0 and finite.
- NMA-DOMAIN-004: `RetryPolicy.maxAttempts` SHALL be >= 1 and finite (no
  infinite retry, NMA-EXEC-005).
- NMA-DOMAIN-005: `ActionPlan` SHALL be an ordered, non-empty list of Actions
  with unique ids.
- NMA-DOMAIN-006: `ActionResult` SHALL be SUCCESS or FAILURE with a typed
  `AgentError` and duration; it SHALL NOT assume success from a raw API return
  value alone.
- NMA-DOMAIN-007: `Observation` SHALL capture currentPackage, visibleText,
  contentDescriptions, screenState, timestamp at minimum.
- NMA-DOMAIN-008: `VerificationResult` SHALL record the verification type and
  pass/fail with reason.
- NMA-DOMAIN-009: Domain objects SHALL be pure Kotlin (no Android imports).
- NMA-DOMAIN-010: `TargetApplication` SHALL describe a target app by validated
  package name and friendly name.
- NMA-DOMAIN-011: `ExecutionContext` SHALL carry the task id, cancellation
  token reference, and per-action bookkeeping.

## Constraints
Immutable value objects preferred; invariants enforced at construction
(factory functions return typed errors instead of throwing where practical).

## Inputs
Validated structured data from AI parser / planner layers.

## Outputs
Typed domain objects consumed by the execution engine.

## State
Domain objects are immutable; task status lives in AgentTaskStatus.

## Failure Conditions
Invariant violation (empty plan, non-positive timeout, retry < 1, duplicate
action ids, unknown action type).

## Acceptance Criteria
- Constructors reject invalid values with typed DomainValidationError.
- Unit tests cover every invariant above.
