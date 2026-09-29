# AI AGENT SPEC

## Purpose
Define the AI planner role, provider abstraction, and output validation.

## Scope
Phase 9 implementation; abstractions fixed now.

## Requirements
- NMA-AI-001: The AI SHALL only map Natural Language → Intent → ActionPlan.
- NMA-AI-002: The AI SHALL NOT have direct access to AccessibilityService,
  Activity, or Android Context.
- NMA-AI-003: Providers SHALL implement the `AIProvider` interface; possible
  implementations: OpenAIProvider, GeminiProvider, MistralProvider,
  OpenRouterProvider, LocalProvider. No provider-specific logic in domain.
- NMA-AI-004: AI output SHALL pass the pipeline: LLM → JSON Parser → Schema
  Validator → Safety Validator → Action Validator → Execution.
- NMA-AI-005: Malformed, random-text, or unknown-action output SHALL be
  REJECTED — never guessed.
- NMA-AI-006: The planner SHALL emit structured output, e.g.
  `{"task": "create_video", "actions": [...]}` where each action conforms to
  ACTION_SPEC.
- NMA-AI-007: API keys SHALL NOT be stored in source code or logs (see
  SECURITY_SPEC).
- NMA-AI-008: Every provider call SHALL have a timeout and SHALL be
  cancellable.

## Constraints
Provider-agnostic domain; no vendor SDK types leak into core.

## Inputs
User instruction + context (current observation summary, target app profile).

## Outputs
Validated ActionPlan or typed PlanningError.

## State
PLANNING state in the agent state machine.

## Failure Conditions
Provider error, timeout, malformed JSON, schema violation, safety violation.

## Acceptance Criteria
Malformed-output fixtures are rejected by tests (NMA-TEST-011).
