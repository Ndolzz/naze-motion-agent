# SECURITY SPEC

## Purpose
Baseline security and safety rules.

## Scope
All phases.

## Requirements
- NMA-SEC-001: API keys SHALL NOT be stored in source code.
- NMA-SEC-002: API keys and tokens SHALL NOT be logged.
- NMA-SEC-003: The AI SHALL NOT be granted unrestricted filesystem access.
- NMA-SEC-004: Executable actions SHALL come from an allowlist (V1 action
  types per NMA-ACTION-002).
- NMA-SEC-005: File paths used by actions SHALL be validated.
- NMA-SEC-006: External app package names SHALL be validated against the
  TargetApplication profile.
- NMA-SEC-007: Automation commands SHALL be bounded (timeouts, retry limits,
  recovery limits).
- NMA-SEC-008: The user SHALL be able to stop the agent at any time.
- NMA-SEC-009: Emergency Stop SHALL have the highest priority: cancel current
  action, pending actions, retries, recovery; no new actions after
  cancellation; final state CANCELLED.
- NMA-SEC-010: The agent SHALL NOT modify or reverse-engineer internal data of
  Alight Motion in the foundation phase.
- NMA-SEC-011: The system SHALL have no infinite retry and no infinite
  automation loop (hard loop budget per task).

## Constraints
Fail-safe default: on any unexpected condition, abort rather than guess.

## Inputs / Outputs / State
Not applicable.

## Failure Conditions
Security violation = task abort with typed error.

## Acceptance Criteria
Cancellation tests prove Emergency Stop semantics (NMA-TEST-010).
