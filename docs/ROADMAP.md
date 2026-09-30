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
| 12 | In app multi provider API keys + network providers | done |
| 13 | Persistent run history | done |
| 14 | Live execution timeline + API key test | done |

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
same provider interface. The dashboard runs real instructions, the status bar
reflects the real accessibility connection, and the execution console streams
the structured engine log.

Phase 12 note: API keys are entered inside the app on the Settings screen and
stored in app private storage on the device; nothing is baked into the build.
ApiKeyStore keeps one key, model, and optional base URL per provider, several
providers can be configured at once, and one is selected as active.
NetworkAiProvider in core/ai implements AIProvider for OpenAI compatible
endpoints (including custom base URLs), Anthropic, and Google Gemini; its
request, endpoint, header, and response handling are pure functions covered
by JVM tests. The planner falls back to LocalTemplateProvider whenever the
selected provider has no complete configuration, so a missing key never
breaks a run silently.

Phase 13 note: every finished run is persisted to a local Room database
(instruction, outcome, reason, action counts, duration, end time, and the
structured engine log). The History screen and the workflow detail screen
render from that real data instead of mock entries; the detail screen replays
the actual technical log of the run.

Phase 14 note: the execution console timeline is now the real validated plan.
MotionAgent exposes the plan through an onPlan callback before execution, the
runtime maps every action to a short label, and the structured engine log
drives the state of each step (active, success, recovering, failed) live. The
Settings screen gains a Test button that verifies the current key, model, and
base URL with one real planning call before saving; the result message is
shown inline. MockData is no longer used by the execution console.
