# Architecture — Naze Motion Agent

## Pipeline

    USER INTENT → AI PLANNER → STRUCTURED ACTION PLAN → PLAN VALIDATION
    → ACTION EXECUTOR → ANDROID ACCESSIBILITY → ALIGHT MOTION
    → OBSERVATION → VERIFICATION → RECOVERY / NEXT ACTION

## Layers (dependency direction)

    Presentation (Compose UI)
         ↓
    Application / Execution
         ↓
    Domain  ←  Infrastructure (accessibility driver, AI providers, storage)

Rules:
- Domain depends on nothing Android. It is pure Kotlin.
- AI providers never touch AccessibilityService; they emit structured plans only.
- Accessibility layer knows nothing about AI providers.
- Target apps (Alight Motion, later CapCut/VN/…) live behind
  `TargetApplicationAdapter` in `target/`, outside `core/`.

## Module layout (planned)

    app/                 Android application (Compose UI, service wiring)
    core/domain          entities, value objects, invariants
    core/state           agent state machine
    core/action          dispatcher, handlers, validators        (Phase 4)
    core/execution       AgentExecutionEngine                    (Phase 5)
    core/observation     ObservationEngine + providers          (Phase 5/7)
    core/recovery        RecoveryManager                         (Phase 5)
    core/logging         structured events                      (Phase 5)
    core/errors          typed failure taxonomy                  (Phase 1 spec)
    automation/          AutomationDriver interface + Android impl (Phase 7)
    ai/                  AIProvider, parser, schema/safety validators (Phase 9)
    target/alightmotion  AlightMotionAdapter                    (Phase 8)
    data/                local storage, history                  (later)
    presentation/        dashboard, execution, settings         (Phase 10)

## Key abstractions (interfaces, defined by spec)
- `AutomationDriver` — find/click/swipe/typeText/launchApp/… (fake-testable)
- `ObservationProvider` — AccessibilityObservationProvider first
- `AIProvider` — OpenAI/Gemini/Mistral/OpenRouter/Local swappable
- `TargetApplicationAdapter` — AlightMotionAdapter first
- `AgentCancellationToken` — global Emergency Stop

## Current state (Phase 0–3)
Only `core/domain` and `core/state` are implemented. Everything above marked
"(Phase N)" is specified but not implemented — do not implement before its phase.
