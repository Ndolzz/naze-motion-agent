## 0.28.0

PART 2 — Beginner-friendly onboarding & smart setup.

- First launch flow: animated splash -> Welcome screen (mark, tagline,
  three plain-language benefits) -> guided setup wizard -> main app.
  The wizard shows real progress ("1 of 4") and walks through device
  access, AI connection, connection test, and a ready summary. Skipping
  leads into the app with a clear Finish setting up reminder.
- Explain first, configure second: permissions are described in plain
  language ("Let Naze Motion control supported apps", "Show Naze Motion
  over other apps") with expandable help and a clear statement of what
  does not work without them. No developer jargon as the primary voice.
- Permission verification: the wizard polls the real accessibility and
  overlay state, so the moment the user returns from system settings
  the step reflects success or offers to try again / open settings.
- AI connection: provider cards from the real catalog (including the
  on-device local option), a masked API key field with Paste from
  clipboard that never re-displays a saved key, and a recommended
  default model per provider. No manual model IDs for beginners; the
  advanced model field stays behind an expander.
- Connection test with staged, human-readable progress ("Connecting to
  your AI...", "Checking API key...", "Testing model...") and friendly
  error mapping: invalid key, model unavailable, temporarily busy, and
  offline each get their own headline and next action. Raw provider
  messages only appear under Technical details.
- Setup persistence: onboarding state is stored, so returning users go
  straight to the app. If permissions are revoked or the AI config
  breaks later, a contextual banner explains what happened with a one
  tap Fix now instead of forcing the whole setup again.
- Beginner Mode (on by default) and a new simple Settings surface;
  the full existing settings screen lives behind Advanced settings.
  Android Back from the advanced area returns to simple settings
  instead of exiting the app.
- Agent dashboard: friendly placeholder ("Tell Naze Motion what you
  want to do..."), tappable Examples chips that only fill the input
  (a run still requires the explicit confirmation), and a Finish
  setting up card for users who skipped the wizard.
- Existing automation engine, accessibility service, overlay service,
  workflows, history, and safety systems are untouched.
- versionCode 30.

## 0.27.0

PART 1 — UI foundation, branding & navigation.

- Design system: NazePalette with a designed dark palette (canonical
  studio identity) and a purpose-built light palette (not an inversion);
  tokens now cover surfaces, borders, text tiers, primary (electric
  blue), secondary (indigo), motion (cyan), and semantic status colors.
  NazeColors became theme-state backed, so every existing screen follows
  the active palette without per-file rewrites.
- Theme: System / Dark / Light selectable from the workspace menu;
  Dark is the default. Material color scheme follows the palette.
- Typography: full hierarchy (display, headline, title, subtitle, body,
  label, caption, numeric, technical) with intentional weights and no
  baked-in text colors.
- Logo: new original Naze Motion mark — a geometric N drawn as a motion
  path with keyframe nodes — as a scalable vector used for the splash,
  the About dialog, and the adaptive launcher icon.
- Splash: animated studio splash (~1500 ms): motion path draws the N,
  keyframe nodes appear, wordmark and tagline fade in, then the splash
  fades into the app. Replaces the blank first-open screen.
- Navigation: the four-tab row is replaced by a clean top bar (product
  name, workspace label, connection status) with a single overflow
  workspace menu: Agent, Workflows, History, Settings, About, Theme.
- Android Back: fixed the instant-exit bug. Back now closes the open
  menu first, then the About dialog, then returns from a run detail to
  the workspace, and only exits at the root.
- Screen transitions: subtle fade + slide between destinations using the
  shared design-system easing.
- No functional changes: automation engine, accessibility service,
  overlay service, workflows, history, safety systems untouched.
- versionCode 29.

## 0.26.8

- Screen aware planning (Phase 28): before the planner runs, the agent
  reads the live accessibility tree of the launched target app and passes
  the visible labels and accessibility descriptions to the planner as an
  observation. The planner prompt now instructs the model to pick target
  labels from that observation, which fixes CREATE_PROJECT and friends on
  devices whose Alight Motion / CapCut UI is not in English (an Indonesian
  UI shows "Proyek Baru", not "New Project", so the old English-only
  targets never matched).
- versionCode 28.

## 0.26.7

- Overlay card fixes: the run end now clears the execution active flag, so
  the floating progress card removes itself instead of staying on screen
  forever. A Close button was added to the card so it can always be
  dismissed immediately, even while the run is still executing.
- The floating card now notices a finished run on its own render pass and
  disappears shortly after, even when the run failed during planning.
- Planner provider retries once after a short wait on transient errors
  (HTTP 429 and 5xx, such as the model overloaded 503) instead of failing
  the whole run immediately.
- versionCode 27.

## 0.26.6

- Floating progress card (Phase 27): while a run executes, a small
  draggable overlay is drawn over the target app showing the task name,
  the current step out of the plan, the last five structured log lines,
  and a Stop run button. The user no longer has to leave Alight Motion or
  Capcut to know what the agent is doing; the card disappears a few
  seconds after the run ends. Requires the display over other apps
  permission; on the first run the service opens the system grant screen
  once and the run itself is never blocked.
- versionCode 26.

## 0.26.5

- Target resolution is now robust against icon-only and container-wrapped
  buttons, which is why CREATE_PROJECT kept failing on the Alight Motion
  projects screen. Normalized text queries now also match
  contentDescription (icon-only buttons such as "+" expose their name only
  there), and a matched non clickable label node now climbs to its nearest
  clickable ancestor container before it is returned to the handler.
- versionCode 25.

## 0.26.4

- Android 11+ package visibility: declared `queries` for Alight Motion
  (full + trial) and CapCut (global + China) in the manifest. This is the
  real cause of the "not installed on this device" preflight error — the
  OS hid the packages from the app, no matter how correct the package
  names were.
- About card in Settings now reads the version dynamically from
  PackageManager instead of the stale hardcoded 0.25.0.
- versionCode 24.

# Changelog

## [0.26.3] PHASE 26 HOTFIX
### Fixed
- The installed target detection now also scans the whole installed app list for the target vendor prefix (com.alightcreative for Alight Motion, com.lemon for CapCut), so any distribution variant of either app is found even when its exact package name is not in the known candidate list. "Not installed" preflight errors can no longer be caused by an unrecognized package variant.
### Changed
- app version 0.26.3.

## [0.26.2] PHASE 26 HOTFIX
### Fixed
- The installed app preflight now recognizes every known package variant of each target: Alight Motion (com.alightcreative.motion plus the direct download trial build com.alightcreative.motion.trial) and CapCut (com.lemon.lvoverseas plus the Chinese JianYing build com.lemon.lv). The registry exposes candidatesFor, adapters accept their variants in candidatePackages and match the foreground package and launches against all of them, and the runtime resolves the installed variant before every preflight, launch, and vocabulary audit. The allowlist accepts either the selected package or the installed variant, so "not installed" no longer appears when a non Play Store variant is the one actually installed.
### Changed
- app version 0.26.2.
