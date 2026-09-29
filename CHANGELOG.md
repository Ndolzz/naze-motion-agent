# Changelog

## [0.9.0] PHASE 9
### Added
- core/ai pure JVM module.
- AIProvider interface and PlanningRequest: provider-agnostic planner input,
  no Android or vendor SDK types in core (NMA-AI-002/003).
- Typed PlanningError taxonomy: ProviderError, Timeout, MalformedJson,
  SchemaViolation, SafetyViolation, InvalidAction.
- PlannerJsonParser: strict JSON to domain Action mapping with schema
  rejection, including coordinate targets that lack explicit fallback
  consent.
- SafetyValidator: bounded plan size, bounded per-action timeout, coordinate
  fallback gate.
- AiPlanner: provider call bounded by timeout, then parser, safety, and
  per-action ActionValidator before an ActionPlan is produced (NMA-AI-004).
- Planner unit tests covering happy path and every rejection fixture
  (NMA-TEST-011).

## [0.7.0] PHASE 7
### Added
- core/access Android library module.
- MotionAccessibilityService with explicit connection tracking and no
  automation logic inside the service.
- AndroidAccessibilityDriver implementing the full AutomationDriver contract
  over AccessibilityNodeInfo with gesture fallback for coordinate taps,
  long presses, and swipes.
- AccessibilityTargetResolver with semantic priority resolution and explicit
  coordinate fallback marking.
- AccessibilityObservationProvider over the accessibility tree.
- Accessibility service manifest entry and service configuration resource.
- Bounded tree traversal cap so a pathological screen can never hang a read.
- ObservationProvider interface in core/engine; DriverObservationSource now
  implements it.

## [0.5.0] PHASE 5 AND 6
### Added
- Execution engine: observe, execute, verify, recover loop driven by the
  agent state machine through legal transitions only.
- Bounded retry honoring RetryPolicy per action, with timeout enforcement
  and fail fast on permanent errors.
- RecoveryManager: bounded recovery attempts with backoff and cancellation
  checks between attempts.
- Structured EngineLog: typed events, levels, task and action ids, string
  detail pairs, and a live listener sink for UI streaming.
- Verifier for every VerificationRule type against an Observation, with
  honest failure reasons when a rule cannot be proven.
- ExecutionSummary outcome model: COMPLETED, FAILED, CANCELLED.
- FakeAutomationDriver and FakeTargetResolver deterministic doubles with
  scripted failure counters.
- Engine unit tests: happy path, machine alignment, rejection, disconnect,
  cancellation, retry, recovery, recovery exhaustion, timeout, log coverage.
### Changed
- State table amendment: VERIFYING may transition to COMPLETED so the engine
  can finish a plan in a terminal state.

## [0.3.0] PHASE 10 UI PREVIEW
### Added
- Precision Studio design system: NazeColors, NazeTypography, NazeSpacing,
  NazeShapes, Na
zeAnimations tokens.
- Reusable component library: buttons, cards, sections, text field,
  status label, status dot, divider, empty state, action timeline,
  action row, monospace log, progress.
- Screens: Agent dashboard, Planning, Execution console, History,
  Workflow detail, Settings, plus error and recovery panels.
- Compact navigation: top bar plus tab row (Agent, Workflows, History, Settings).
- Geometric N mark launcher icon.
- Sealed AgentUiState model. No overlapping booleans.
- Screens render from mock data only. Engine wiring arrives with Phase 5.

## [0.2.0] PHASE 4
### Added
- AutomationDriver interface, TargetResolver abstraction, ActionValidator.
- 14 action handlers using the strategy pattern.
- ActionDispatcher with registry based routing.
- 15 unit tests. CI runs core action tests.

## [0.1.0] PHASE 0 to 3
### Added
- Repository skeleton and documentation.
- Phase 1 specifications across ten documents plus roadmap.
- Domain model and agent state machine with legal transition validation.
- Unit tests for domain invariants and state transitions.
