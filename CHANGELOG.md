# Changelog

## [0.3.0] PHASE 10 UI PREVIEW
### Added
- Precision Studio design system: NazeColors, NazeTypography, NazeSpacing,
  NazeShapes, NazeAnimations tokens.
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
