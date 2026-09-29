# Changelog

## [0.1.0-foundation] — PHASE 0–3
### Added
- Repository skeleton, docs index, architecture overview.
- Phase 1 specifications: PRODUCT, ARCHITECTURE, DOMAIN, ACTION,
  STATE_MACHINE, ACCESSIBILITY, AI_AGENT, ERROR, SECURITY, TEST + ROADMAP.
- Domain model: AgentTask, Action, ActionPlan, ActionResult, Observation,
  VerificationResult, ExecutionContext, RetryPolicy, TargetApplication.
- Explicit agent state machine with legal-transition validation
  (IDLE→PLANNING→VALIDATING→READY→EXECUTING→OBSERVING→VERIFYING→…).
- Unit tests for domain invariants and state transitions.
