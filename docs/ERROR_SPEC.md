# ERROR SPEC

## Purpose
Define the typed error taxonomy so failures are precise and recoverable.

## Scope
All layers.

## Requirements
- NMA-ERR-001: All failures SHALL be represented as typed `AgentError` values
  with an error code, message, and optional cause.
- NMA-ERR-002: V1 error codes SHALL include:
  PLANNING_FAILED, INVALID_PLAN, INVALID_ACTION, UNKNOWN_ACTION_TYPE,
  TARGET_NOT_FOUND, TIMEOUT, VERIFICATION_FAILED, RETRY_EXHAUSTED,
  RECOVERY_EXHAUSTED, PACKAGE_NOT_ACTIVE, ACCESSIBILITY_DISCONNECTED,
  SERVICE_NOT_ENABLED, ILLEGAL_STATE_TRANSITION, CANCELLED, TARGET_APP_NOT_FOUND,
  COORDINATE_FALLBACK_REJECTED, INTERNAL_ERROR.
- NMA-ERR-003: Errors SHALL be safe to log (no secrets).
- NMA-ERR-004: Cancellation SHALL be reported as CANCELLED, not as a failure.
- NMA-ERR-005: No failure SHALL trigger unbounded retry or recovery
  (limits per NMA-EXEC-005, NMA-REC-002).

## Constraints
Error codes are part of the stable contract (RULE 10).

## Inputs / Outputs
Produced by all layers; consumed by engine, recovery, and UI.

## State
Not applicable.

## Failure Conditions
An error type that cannot be represented in the taxonomy SHALL map to
INTERNAL_ERROR and be logged.

## Acceptance Criteria
Every failure path in tests yields a typed error.
