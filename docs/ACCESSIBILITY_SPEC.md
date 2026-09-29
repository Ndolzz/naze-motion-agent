# ACCESSIBILITY SPEC

## Purpose
Define the automation/observation contract over Android AccessibilityService.

## Scope
Phase 7 implementation (interface defined by spec now).

## Requirements
- NMA-ACCESS-001: Automation SHALL go through the `AutomationDriver` interface:
  find, click, longClick, swipe, scroll, typeText, pressBack, launchApp,
  captureScreen, getCurrentPackage, getAccessibilityTree.
- NMA-ACCESS-002: `AndroidAccessibilityDriver` SHALL implement AutomationDriver
  on top of a MotionAccessibilityService (AccessibilityService subclass).
- NMA-ACCESS-003: A `FakeAutomationDriver` SHALL exist for JVM testing of the
  execution engine.
- NMA-ACCESS-004: `AccessibilityNodeInfo` SHALL be the primary interaction
  mechanism; coordinate gestures are a fallback only (per NMA-ACTION-007/008).
- NMA-ACCESS-005: `ObservationProvider` SHALL abstract observation; the first
  implementation SHALL be `AccessibilityObservationProvider`. Vision-based
  observation SHALL be pluggable later and SHALL NOT be a foundation dependency.
- NMA-ACCESS-006: The accessibility service SHALL expose its connection state;
  a disconnect during execution SHALL be a typed failure, not a crash.
- NMA-ACCESS-007: The accessibility layer SHALL NOT contain AI or
  target-app-specific logic.

## Constraints
No root. Service must be user-enabled in system settings.

## Inputs
Driver calls from action handlers.

## Outputs
Node results, gesture results, observations.

## State
Service connected/disconnected; device screen state.

## Failure Conditions
Service not enabled, node not found, gesture dispatch failure, timeout.

## Acceptance Criteria
Execution engine tests pass fully against FakeAutomationDriver (no Android).
