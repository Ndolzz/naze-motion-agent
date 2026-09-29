# Changelog

## [0.2.0] PHASE 4
### Added
- AutomationDriver interface (find, click, swipe, typeText, launchApp, and more).
- TargetResolver abstraction with explicit resolution priority.
- ActionValidator with per type rules and path traversal protection.
- 14 action handlers using the strategy pattern (no giant conditionals).
- ActionDispatcher with registry based routing and typed rejection of
  unknown or invalid actions.
- 15 unit tests covering valid, invalid, unknown, cancellation, disconnect,
  and target not found paths.
- CI now runs core action tests.

## [0.1.0] PHASE 0 to 3
### Added
- Repository skeleton, docs index, architecture overview.
- Phase 1 specifications: PRODUCT, ARCHITECTURE, DOMAIN, ACTION,
  STATE_MACHINE, ACCESSIBILITY, AI_AGENT, ERROR, SECURITY, TEST + ROADMAP.
- Domain model: AgentTask, Action, ActionPlan, ActionResult, Observation,
  VerificationResult, ExecutionContext, RetryPolicy, TargetApplication.
- Explicit agent state machine with legal transition validation.
- Unit tests for domain invariants and state transitions.
