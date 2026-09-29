# STATE MACHINE SPEC

## Purpose
Define the explicit agent state machine and legal transitions.

## Scope
`core/state` (Phase 3). Used by the execution engine (Phase 5).

## Requirements
- NMA-STATE-001: Agent states SHALL be exactly: IDLE, PLANNING, VALIDATING,
  READY, EXECUTING, OBSERVING, VERIFYING, RECOVERING, COMPLETED, FAILED,
  CANCELLED.
- NMA-STATE-002: Legal transitions SHALL be:
  IDLE→PLANNING; PLANNING→VALIDATING; PLANNING→FAILED; PLANNING→CANCELLED;
  VALIDATING→READY; VALIDATING→FAILED; VALIDATING→CANCELLED;
  READY→EXECUTING; READY→CANCELLED;
  EXECUTING→OBSERVING; EXECUTING→FAILED; EXECUTING→CANCELLED;
  OBSERVING→VERIFYING; OBSERVING→FAILED; OBSERVING→CANCELLED;
  VERIFYING→EXECUTING; VERIFYING→RECOVERING; VERIFYING→FAILED;
  VERIFYING→CANCELLED;
  RECOVERING→OBSERVING; RECOVERING→FAILED; RECOVERING→CANCELLED;
  COMPLETED→IDLE; FAILED→IDLE; CANCELLED→IDLE.
- NMA-STATE-003: Any transition not listed SHALL be illegal and SHALL be
  rejected by a transition validator with a typed error.
- NMA-STATE-004: Cancellation SHALL be reachable from every non-terminal,
  non-CANCELLED state.
- NMA-STATE-005: Terminal states (COMPLETED, FAILED, CANCELLED) SHALL only
  reset to IDLE.
- NMA-STATE-006: State changes SHALL emit a structured event
  (StateTransitioned, see observability spec).

## Constraints
The transition table SHALL be explicit (no dynamic transition invention).

## Inputs
Engine-driven transition requests.

## Outputs
New state or IllegalStateTransitionError.

## State
Single current state per agent instance.

## Failure Conditions
Illegal transition request.

## Acceptance Criteria
- All legal transitions pass; all illegal transitions are rejected with typed
  error; tests enumerate the full table.
