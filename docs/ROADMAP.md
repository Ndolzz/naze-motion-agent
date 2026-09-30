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
| 15 | More provider presets (Groq, OpenRouter, Ollama) | done |
| 16 | History delete/clear + real accessibility status in Settings | done |
| 17 | Plan timeline replay in detail + destructive action confirmation | done |
| 18 | Real dashboard summary + history search | done |
| 19 | Preflight check before run | done |
| 20 | Run confirmation + auto launch target + export run log | done |
| 21 | Run again from history + getting started guide | done |
| 22 | Configurable safety settings (ExecutionProfile) | done |
| 23 | Allowed applications allowlist | done |
| 24 | Reconnect detection + rerun offer, config export/import, multi-adapter registry | done |

Phase 7 note: core/access is an Android library holding MotionAccessibilityService, AndroidAccessibilityDriver, AccessibilityTargetResolver, and AccessibilityObservationProvider. The service is user enabled in system settings; a disconnect surfaces as a typed failure, never a crash. The service manifest entry lives in the core/access library manifest and is merged into the app at build time.

Phase 5 note: the engine runs observe, execute, verify, recover with bounded retry and recovery, timeout enforcement, cancellation checks at every boundary, and a structured event log. The state table allows VERIFYING to COMPLE TED so a finished plan reaches a terminal state legally.

Phase 6 note: FakeAutomationDriver and FakeTargetResolver live in the engine module as deterministic doubles for tests and previews.

Phase 10 note: screens render from mock state only. No fake AI behavior. Engine wiring into the screens arrives with Phase 11.

Phase 11 note: core/agent adds MotionAgent, the end to end orchestrator that chains planner, target adapter, and execution engine into one typed run, with JVM end to end tests over the fake doubles. The app replaces its mock state with a real AgentRuntime over AndroidAccessibilityDriver, AccessibilityTargetResolver, AlightMotionAdapter, and the AiPlanner backed by LocalTemplateProvider, a deterministic on device template provider behind the same provider interface. The dashboard runs real instructions, the status bar reflects the real accessibility connection, and the execution console streams the structured engine log.

Phase 12 note: API keys are entered inside the app on the Settings screen and stored in app private storage on the device; nothing is baked into the build. ApiKeyStore keeps one key, model, and optional base URL per provider, several providers can be configured at once, and one is selected as active. NetworkAiProvider in core/ai implements AIProvider for OpenAI compatible endpoints (including custom base URLs), Anthropic, and Google Gemini; its request, endpoint, header, and response handling are pure functions covered by JVM tests. The planner falls back to LocalTemplateProvider whenever the selected provider has no complete configuration, so a missing key never breaks a run silently.

Phase 13 note: every finished run is persisted to a local Room database (instruction, outcome, reason, action counts, duration, end time, and the structured engine log). The History screen and the workflow detail screen render from that real data instead of mock entries; the detail screen replays the actual technical log of the run.

Phase 14 note: the execution console timeline is now the real validated plan. Motio nAgent exposes the plan through an onPlan callback before execu tion, the runtime maps every action to a short label, and the structured engine log drives the state of each step (active, success, recovering, failed) live. The Settings screen gains a Test button that verifies the current key, model, and base URL with one real planning call before saving; the result message is shown inline. MockData is no longer used by the execution console.

Phase 15 note: the provider catalog in Settings grows to cover Groq, OpenRouter, and self hosted Ollama on top of OpenAI, Anthropic, Google Gemini, and custom OpenAI compatible endpoints. All three use the OpenAI compatible chat endpoint already covered by NetworkAiProvider, each with a default base URL and model and an optional per provider hint shown in Settings. Cleartext HTTP is permitted only for localhost, 127.0.0.1, and the emulator host alias 10.0.2.2 through a network security config, so a local Ollama works while every other connection must still be HTTPS.

Phase 16 note: the workflow detail screen can delete a single persisted run and the History screen can clear all of them, both through new DAO operations, with the history list updating live from the Room flow. The Settings Automation card shows the real MotionAccessibilityService connection state (refreshed while the screen is visible) and a button that opens the system accessibility settings directly.

Phase 17 note: the plan steps and their final states are persisted with every run, so the workflow detail screen replays the real action timeline of the finished run through NazeActionTimeline, on top of the raw technical log. Destructive actions (Clear all, Delete run) require an explicit confirmation dialog. The history database moves to schema version 2 with a destructive migration, since run history is disposable local diagnostics.

Phase 18 note: the dashboard Agent tab now renders a real summary computed from the persisted run history (total runs, completed ru ns, success rate) and lists the three most recent runs, each ope ning its workflow detail. The History screen gains a search field filtering runs by instruction text with a dedicated no matches state. MockData is fully removed from the dashboard; every screen renders from the real Room data.

Phase 19 note: before every run, AgentRuntime checks that MotionAccessibilityService is connected and that Alight Motion is installed on the device. A run is refused with a clear reason when either check fails, and the dashboard shows the reason as a dismissible error banner under the RUN button, instead of starting an execution that would fail halfway through. The preflight never mutates state: it only reads the connection flag and the package manager.

Phase 20 note: pressing RUN asks for an explicit confirmation before the agent takes over the device, matching the confirmation pattern already used for destructive actions. After the preflight passes, the runtime sends the Alight Motion launch intent so the run always starts on a ready screen (launching an already open app just focuses it). The workflow detail screen gains an Export log button that writes a plain text run report to the app cache and shares it through the system sheet via FileProvider; the share target receives only a temporary read grant for that one file, and the accessibility service stays declared solely in the core/access library manifest.

Phase 21 note: the workflow detail screen gains a Run again button that re-issues the stored instruction through the normal start flow (preflight, confirmation dialog, auto launch of the target app), so repeating a past run is one tap instead of retyping the instruction. While the accessibility service is not connected, the dashboard renders a numbered getting started guide (enable MotionAccessibilityService, open Alight Motion, run the first instruction) instead of leaving the status area empty, and the guide disappears as soon as the service connects.

Phase 22 note: the safety limits are no longer hardcoded in the engine. ExecutionProfile in core/domain defines the caps for a run (action timeout, retry limit per action, recovery attempts, recovery backoff base) as upper bounds, with validation at construction and a clamped factory for raw user input. The engine applies the profile to every action (effective timeout and retry count are the minimum of the action's own policy and the profile) and builds its bounded recovery from the profile when no manager is injected, so tests can still inject deterministic doubles. The Settings Automation card edits the values, SafetySettingsStore persists them on the device only, and AgentRuntime passes the loaded profile to every run.

Phase 23 note: the applications the agent may open are now an explicit allowlist instead of a hardcoded assumption. AllowedApps in core/domain is pure data with structural package name validation (lowercase dot separated segments), deduplication, a bounded size, and a default of Alight Motion only. AllowedAppsStore persists the list on the device, the Settings screen edits it (add with validation, remove, empty list allowed and honestly explained), and the AgentRuntime preflight refuses a run whose target is not on the list with a clear reason. Allowing an app does not create an adapter for it: today the agent still drives Alight Motion only, and the editor says so.

Phase 24 note: the multi-adapter foundation lands the TargetAdapterRegistry in core/adapter: one place that knows every target application, its package name, its display name, and its adapter class. The app runtime resolves its target through the registry, so adding a second target application later means one new adapter plus one registry entry, with no hardcoded package names left in the app layer. Reconnect detection watches the accessibility link while a run is live; a drop followed by a reconnect keeps the interrupted instruction as a one tap re-run offer instead of losing it. ConfigPorter rounds the phase out with export/import of the full on-device configuration as a single JSON document; the Settings UI buttons for it are the next step, the porter itself is wired and testable now.
