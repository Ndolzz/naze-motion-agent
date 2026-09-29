# ACTION SPEC

## Purpose
Define the action vocabulary, schema, validation, and dispatch model.

## Scope
V1 action types; dispatch architecture is implemented in Phase 4.

## Requirements
- NMA-ACTION-001: The system SHALL represent every automation command using a
  validated structured Action object; malformed actions SHALL be rejected
  before execution.
- NMA-ACTION-002: V1 action types SHALL be: OPEN_APP, WAIT, TAP, LONG_PRESS,
  SWIPE, SCROLL, TYPE_TEXT, PRESS_BACK, SCREENSHOT, FIND_ELEMENT,
  CREATE_PROJECT, ADD_MEDIA, ADD_TEXT, EXPORT.
- NMA-ACTION-003: The action set SHALL be extensible without modifying a
  giant conditional (strategy/handler pattern: Action → ActionDispatcher →
  ActionHandler → AutomationDriver).
- NMA-ACTION-004: Every action SHALL declare a timeout (default 5000 ms).
- NMA-ACTION-005: Every action SHALL declare a RetryPolicy (default
  maxAttempts = 2).
- NMA-ACTION-006: Every action MAY declare a verification rule; important
  actions SHOULD.
- NMA-ACTION-007: Action targets SHALL be semantic (resource-id,
  content-description, text, normalized text) with coordinate fallback only
  when semantic interaction is unavailable; fallback SHALL be explicit,
  logged, bounded, and optional.
- NMA-ACTION-008: Target resolution priority SHALL be: resource-id,
  content-description, exact text, normalized text, accessibility hierarchy,
  semantic relationship, coordinate fallback.
- NMA-ACTION-009: An invalid or unknown action type SHALL be rejected, never
  guessed.

## Constraints
Action schema backward compatibility per SDD RULE 10.

## Inputs
JSON action objects from the AI planner (after parsing).

## Outputs
Validated Action domain objects; executed ActionResults.

## State
Actions are immutable; execution state is tracked by the engine.

## Failure Conditions
Schema validation failure, unknown type, target unresolvable, timeout,
verification failure.

## Acceptance Criteria
- Valid, invalid, and unknown action fixtures are covered by tests
  (TEST_SPEC, NMA-TEST-002..004).
