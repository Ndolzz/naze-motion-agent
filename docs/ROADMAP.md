# ROADMAP

| Phase | Scope | Status |
|---|---|---|
| 0 | Repository, docs, architecture | done |
| 1 | All specifications | done |
| 2 | Domain model (core/domain) | done |
| 3 | State machine + transition validation (core/state) | done |
| 4 | Action system: dispatcher, handlers, validator | done |
| 5 | Execution engine + cancellation + structured logging | next |
| 6 | FakeAutomationDriver | planned |
| 7 | Accessibility service, AndroidAccessibilityDriver, ObservationProvider | planned |
| 8 | AlightMotionAdapter | planned |
| 9 | AI planner + provider abstraction + output validation | planned |
| 10 | Precision Studio UI with mock state | done (mock preview) |
| 11 | End to end flow | planned |

Phase 10 note: screens render from mock state only. No fake AI behavior.
Execution engine wiring replaces the mock when Phase 5 lands.
