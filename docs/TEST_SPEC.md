# TEST SPEC

## Purpose
Define the testing strategy as part of the foundation.

## Scope
All phases; foundation minimums listed here.

## Requirements
- NMA-TEST-001: The execution engine SHALL be testable on the JVM via
  FakeAutomationDriver (no Android UI, no Alight Motion).
- NMA-TEST-002: A valid action SHALL be accepted (test).
- NMA-TEST-003: An invalid action SHALL be rejected (test).
- NMA-TEST-004: An unknown action type SHALL be rejected (test).
- NMA-TEST-005: Action timeout behavior SHALL be tested.
- NMA-TEST-006: Retry behavior and retry limits SHALL be tested.
- NMA-TEST-007: Verification failure SHALL produce ActionResult = FAILURE (test).
- NMA-TEST-008: Recovery behavior and recovery limits SHALL be tested.
- NMA-TEST-009: Cancellation SHALL stop execution immediately (test).
- NMA-TEST-010: Emergency Stop SHALL prevent any new action after cancellation
  (test).
- NMA-TEST-011: Malformed AI output SHALL be rejected (test).
- NMA-TEST-012: Illegal state transitions SHALL be rejected (test).
- NMA-TEST-013: Domain invariants SHALL be tested (per DOMAIN_SPEC).
- NMA-TEST-014: Tests SHALL run without a device or emulator for core modules.

## Constraints
Unit tests + integration tests; deterministic; no network in tests.

## Inputs
Fixtures: actions, plans, fake observations, malformed AI output.

## Outputs
Green suite; typed failures on regression.

## State
Each test isolated.

## Failure Conditions
Any test red = phase not done.

## Acceptance Criteria
Foundation suite covers items 002–013 (005–012 land with Phases 4–5; domain
and state machine tests land in Phase 2–3).
