# Changelog

## [0.21.0] PHASE 21
### Added
- Run again from history: the workflow detail screen gains a Run again button that immediately starts a new agent run with the same stored instruction, going through the exact same flow as a fresh run (preflight check, confirmation dialog, and automatic launch of the target app).
- Getting started guide: while MotionAccessibilityService is not connected, the dashboard shows a numbered three step guide (enable the accessibility service, open Alight Motion, run the first instruction) so a new user knows exactly what to do before the first run; the guide disappears as soon as the service connects.
### Changed
- app version 0.21.0.

## [0.20.0] PHASE 20
### Added
- Export run log: the workflow detail screen gains an Export log button
that writes a plain text report of the run (instruction, outcome, reason, action counts, duration, end time, plan timeline, and the full technical log) to the app cache and hands it to the system share sheet through a FileProvider, with only a temporary read grant for the chosen target.
- Run confirmation dialog: pressing RUN now asks for an explicit
confirmation before the agent takes over the device, consistent with the existing confirmation pattern for destructive actions.
- Auto launch of the target app: after the preflight check passes, the
runtime brings Alight Motion to the front (launch intent) so every run starts on a ready screen; launching an already open app simply focuses it.
### Changed
- app version 0.20.0.

## [0.19.0] PHASE 19
### Added
- Preflight check before every run: AgentRuntime verifies that
MotionAccessibilityService is connected and that Alight Motion is installed on the device before the run starts, and refuses the run with a clear reason instead of failing halfway through execution.
- Dismissible preflight banner on the dashboard: when a run is refused,
the reason appears as an error card under the RUN button and can be dismissed once the environment is fixed.
### Changed
- app version 0.19.0.

## [0.18.0] PHASE 18
### Added
- Real dashboard summary: the Agent tab now computes live stats from the
persisted run history (total runs, completed runs, and a success rate) instead of hardcoded mock numbers, and lists the three most recent real runs with an open action that jumps straight to the workflow detail.
- History search: the History screen gains a search field that filters
persisted runs by instruction text, with a dedicated no matches empty state when nothing fits.
### Changed
- MockData is fully removed from the dashboard; every screen now renders
from the real Room history.
- app
version 0.18.0.

## [0.17.0] PHASE 17
### Added
- Plan timeline replay: the validated plan steps and their final states are
now persisted with every run, and the workflow detail screen renders the real action timeline of the finished run instead of only the raw log.
- Confirmation dialogs: Clear all on the History screen and Delete run on
the detail screen both ask before deleting, since these actions cannot be undone.
### Changed
- History database schema version 2 with a plan steps column; the table is
recreated on upgrade because run history is disposable local diagnostics.
- app version 0.17.0.

## [0.16.0] PHASE 16
### Added
- History management: a single persisted run can be deleted from the
workflow detail screen, and the whole history can be cleared with one button on the History screen (AgentRunDao deleteById and clearAll, AgentRuntime deleteRun and clearHistory).
- Real accessibility service status in Settings: the Automation card shows
the live connection state of MotionAccessibilityService (Service connected or Service off) instead of a hardcoded Off, refreshed while the screen is visible.
- Open system accessibility settings button in Settings: launches the
device accessibility settings screen so the user can enable the service without leaving the app.
### Changed
- app version 0.16.0.

## [0.15.0] PHASE 15
### Added
- More network provider presets: Groq, OpenRouter, and self hosted Ollama
join OpenAI, Anthropic, Google Gemini, and custom OpenAI compatible endpoints in the Settings provider catalog. Every preset ships a default base URL and model and uses the OpenAI compatible chat endpoint already implemented by NetworkAiProvider.
- Per provider hints in Settings: the Ollama entry explains that the key
field is ignored by the server and which base URL to use on the Android emulator versus a physical device.
- Network security config: cleartext HTTP is permitted only for localhost,
127.0.0.1, and 10.0.2.2 so a se lf hosted Ollama on the same machine works, while all other traffic must still be HTTPS.
### Changed
- app version 0.15.0.

## [0.14.0] PHASE 14
### Added
- Live execution timeline: the console renders the real validated plan.
MotionAgent gains an optional onPlan callback invoked after planning and before execution; the runtime maps each action to a short label and the structured engine log drives per step states (active, success, recovering, failed) as events arrive.
- Test button in Settings: verifies the current key, model, and base URL
with one real planning call before saving, with an inline success or failure message (ApiKeyStore.testConfig).
### Changed
- ExecutionScreen no longer uses MockData: the timeline comes from the plan
and the technical log streams the actual engine events, with a building state while the plan is not ready yet.
- app version 0.14.0.

## [0.13.0] PHASE 13
### Added
- Persistent run history: every finished agent run is stored in a local
Room database with its instruction, outcome, failure reason, action counts, duration, end time, and the structured engine log.
- HistoryDatabase with AgentRunEntity, AgentRunDao, and a singleton
database builder; the DAO exposes the runs as a Flow.
- AgentRuntime now exposes a history StateFlow fed from the database and
inserts a record for every terminal run result.
- The History screen lists real persisted runs (empty state when none) and
the workflow detail screen shows real stats and replays the actual technical log of the selected run.
### Changed
- Navigation keeps the selected run id instead of a mock name; the detail
screen is found from the live history list.
- MockData history and recent workflow entries are no longer shown on the
History screens.
- app version 0.13.0.

## [0.12.0] PHASE 12
### Added
- In app API key management: keys are entered on the Settings screen inside
the app and stored in app private storage on the device only. They are never baked into the build and never logged.
- Multi provider support: OpenAI, Anthropic, Google Gemini, any custom
OpenAI compatible endpoint, and the on device Local templates can all be configured with a key, model, and optional base URL at the same time; one is selected as active.
- NetworkAiProvider in core/ai: an AIProvider implementation for
OPENAI_COMPATIBLE, ANTHROPIC, and GEMINI chat APIs with a system prompt pinned to the planner JSON schema, code fence stripping, and typed failures for non 2xx responses.
- ApiKeyStore in the app: per provider key/model/base URL storage, clear,
selection, and an activeProvider factory that falls back to LocalTemplateProvider when the selected provider is not fully configured.
- Settings screen redesign: provider list with saved key state, masked key
field with show/hide, model and base URL fields, Save and Clear actions.
- 17 JVM tests for NetworkAiProvider request building, endpoints, headers,
response extraction, sanitization, and config validation.
### Changed
- AgentRuntime now builds its planner from ApiKeyStore.activeProvider(), so
runs use the in app configured network provider when a key is present.
- INTERNET permission added to the app manifest for network providers.
- app version 0.12.0.

## [0.11.0] PHASE 11
### Added
- core/agent module with MotionAgent: the end to end orchestrator chaining
planner, target adapter, and execution engine into a single typed run.
- AgentResult terminal model: Completed, Failed, Cancelled, PlanningFailed,
TargetUnavailable, InvalidInstruction.
- LocalTemplateProvider: a deterministic on device provider behind the same
AIProvider interface; output passes the full planning validation pipeline.
- Eight end to end JVM tests over FakeAutomationDriver and FakeTargetResolver.
- AgentRuntime app wiring: real accessibility driver, target resolver,
AlightMotionAdapter, planner, and engine behind the dashboard and execution console. The structured engine log streams liv e into the console.
### Changed
- The dashboard RUN button now starts a real agent run instead of flipping
mock state; the status bar reflects the real accessibility connection.
- STOP AGENT and the execution close button cancel the run through the
cancellation token (Emergency Stop, NMA-SEC-008/009).
- settings.gradle.kts now includes :core:adapter, :core:ai, and :core:agent,
and CI runs their tests alongside the other core modules.
- app version 0.11.0.

## [0.7.0] PHASE 7
### Added
- core/access Android library module.
- MotionAccessibilityService with explicit connection tracking and no
automation logic inside the service.
- AndroidAccessibilityDriver implementing the full AutomationDriver contract
over AccessibilityNodeInfo with gesture fallback for coordinate taps, long presses, and swipes.
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
- FakeAutomationDriver and FakeTargetResolver deterministic dou
bles with scripted failure counters.
- Engine unit tests: happy path, machine alignment, rejection, disconnect,
cancellation, retry, recovery, recovery exhaustion, timeout, log coverage.
### Changed
- State table amendment: VERIFYING may transition to COMPLETED so the engine
can finish a plan in a terminal state.

## [0.3.0] PHASE 10 UI PREVIEW
### Added
- Precision Studio design system: NazeColors, NazeTypography, NazeSpacing,
NazeShapes, NazeAnimations tokens.
- Reusable component library: buttons, cards, sections, text field,
status label, status dot, divider, empty state, action timeline, action row, monospace log, progress.
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
