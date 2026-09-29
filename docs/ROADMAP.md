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
| 9 | AI planner + provider abstraction + output validation | planned |
| 10 | Precision Studio UI with mock state | done (mock preview) |
| 11 | End to end flow | planned |

Phase 8 note: core/adapter is a pure JVM module holding
TargetApplicationAdapter and AlightMotionAdapter. The adapter knows the
target package (com.alightmotion.motion) and a best-effort vocabulary of
known UI queries; all interaction is delegated to the AutomationDriver so
the engine stays app agnostic (NMA-ARCH-007).

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
