# ARCHITECTURE SPEC

## Purpose
Define module boundaries, dependency rules, and extension points.

## Scope
All modules of naze-motion-agent, including future phases.

## Requirements
- NMA-ARCH-001: The system SHALL be organized into modules: core (domain,
  state, action, execution, observation, recovery, logging, errors),
  automation, ai, target, data, presentation.
- NMA-ARCH-002: The domain layer SHALL NOT depend on Android UI types
  (View, Activity, Context, AccessibilityNodeInfo).
- NMA-ARCH-003: The Presentation layer SHALL depend on Application/Execution,
  which SHALL depend on Domain. Infrastructure SHALL implement Domain
  interfaces.
- NMA-ARCH-004: The AI layer SHALL NOT access AccessibilityService, Activity,
  or Android Context.
- NMA-ARCH-005: The accessibility layer SHALL NOT know AI provider details.
- NMA-ARCH-006: Automation SHALL be driven through the AutomationDriver
  interface so a FakeAutomationDriver can test the execution engine on the JVM.
- NMA-ARCH-007: Target-specific behavior SHALL live behind the
  TargetApplicationAdapter interface (AlightMotionAdapter first).
- NMA-ARCH-008: All execution SHALL be observable via structured events.
- NMA-ARCH-009: No module SHALL contain a God Object; execution logic SHALL be
  split into state machine, dispatcher, engine, recovery.
- NMA-ARCH-010: The project SHALL build with Gradle; core modules SHALL be
  pure Kotlin/JVM to remain device-free testable.

## Constraints
See PRODUCT_SPEC constraints (no Flutter, no root, etc.).

## Inputs / Outputs
Not applicable (structural spec).

## State
See STATE_MACHINE_SPEC.

## Failure Conditions
Dependency rule violation, e.g. domain importing Android types — this is a
build/test failure and MUST be treated as an error.

## Acceptance Criteria
- `core/domain` and `core/state` compile without Android dependencies.
- Unit tests pass without a device or emulator.
