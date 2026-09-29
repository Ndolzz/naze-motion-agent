# ROADMAP

| Phase | Scope | Status |
|---|---|---|
| 0 | Repository, docs, architecture | done |
| 1 | All specifications | done |
| 2 | Domain model (core/domain) | done |
| 3 | State machine + transition validation (core/state) | done |
| 4 | Action system: dispatcher, handlers, validator | done |
| 5 | Execution engine + cancellation + structured logging | done |
| 6 | FakeAutomationDriver | done |
| 7 | Accessibility service, AndroidAccessibilityDriver, ObservationProvider | done |
| 8 | AlightMotionAdapter | done |
| 9 | AI planner + provider abstraction + output validation | done |
| 10 | Precision Studio UI with mock state | done (mock preview) |
| 11 | End to end flow | done |

Phase 7 note: core/access is an Android library holding
MotionAccessibilityService, AndroidAccessibilityDriver,
AccessibilityTargetResolver, and AccessibilityObservationProvider. The
service is user enabled in system settings; a disconnect surfaces as a
typed failure, never a crash.

Phase 5 note: the engine runs observe, execute, verify, recover with bounded
retry and recovery, timeout enforcement, cancellation checks at every boundary,
and a structured event log. The state table allows VERIFYING to COMPLETED
so a finished plan reaches a terminal state legally.

Phase 6 note: FakeAutomationDriver and FakeTargetResolver live in the engine
module as deterministic doubles for tests and previews.

Phase 10 note: screens render from mock state only. No fake AI behavior.
Engine wiring into the screens arrives with Phase 11.

Phase 11 note: core/agent adds MotionAgent, the end to end orchestrator that
chains planner, target adapter, and execution engine into one typed run,
with JVM end to end tests over the fake doubles. The app replaces its mock
state with a real AgentRuntime over AndroidAccessibilityDriver,
AccessibilityTargetResolver, AlightMotionAdapter, and the AiPlanner backed by
LocalTemplateProvider, a deterministic on device template provider behind the
same provider interface (network model providers arrive with a later phase).
The dashboard runs real instructions, the status bar reflects the real
accessibility connection, and the execution console streams the structured
engine log.
