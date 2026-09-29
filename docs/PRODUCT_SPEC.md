# PRODUCT SPEC — Naze Motion Agent

## Purpose
Define the product: an Android-native AI agent that turns natural-language
instructions into validated, observable, verifiable automation of Alight Motion
via AccessibilityService.

## Scope
V1: single user, single device, Alight Motion as target app. AI planner,
deterministic execution engine, observation, verification, recovery.

Out of scope (V1): multi-device, scheduling, vision-based automation as
mandatory dependency, reverse-engineering Alight Motion internals.

## Requirements
- NMA-PROD-001: The system SHALL accept a natural-language instruction
  ("user intent") as the sole user input for a task.
- NMA-PROD-002: The system SHALL NOT execute UI interactions directly from
  natural language; every interaction SHALL come from a validated ActionPlan.
- NMA-PROD-003: The system SHALL treat Alight Motion as an external application
  (see NMA-ARCH-021).
- NMA-PROD-004: The user SHALL be able to stop the agent at any time
  (Emergency Stop, NMA-SEC-010).
- NMA-PROD-005: The system SHALL support adding other target applications
  without modifying the core execution engine.

## Constraints
Android native, Kotlin, Jetpack Compose UI, AccessibilityService, no root,
no Flutter/Kodular, no coordinate-first tapping.

## Inputs
User instruction (text). Optional settings (AI provider config, timeouts).

## Outputs
Final task result (COMPLETED / FAILED / CANCELLED), execution event log,
observed screen states.

## State
See STATE_MACHINE_SPEC.md.

## Failure Conditions
AI plan invalid, target app not installed, target element not found,
verification failure beyond recovery limit, user cancellation, unexpected
accessibility service disconnect.

## Acceptance Criteria
- A valid instruction produces a validated plan that either completes,
  fails with a typed error, or is cancelled — never hangs.
- No action executes without validation (NMA-ACTION-001).
