# Naze Motion Agent

Android-native AI Agent (Kotlin + Jetpack Compose) that executes natural-language
instructions inside **Alight Motion** via the Android **AccessibilityService** —
no root, no coordinate-first tapping, no Flutter/Kodular.

Core principle:

    OBSERVE → PLAN → VALIDATE → ACT → OBSERVE → VERIFY → RECOVER / CONTINUE

The agent is **state-aware** and **verification-driven**, not a macro recorder.

## Status: PHASE 0–3 (Foundation)

- [x] PHASE 0 — Repository, docs, architecture
- [x] PHASE 1 — Specifications (docs/)
- [x] PHASE 2 — Domain model (pure Kotlin, no Android deps)
- [x] PHASE 3 — Agent state machine + transition validation
- [ ] PHASE 4 — Action system (dispatcher/handlers/validator)
- [ ] PHASE 5–11 — Execution engine, fake driver, accessibility, adapters, AI planner, UI, E2E

## Build & Test

```bash
./gradlew :core:domain:test :core:state:test
```

`core/` is a **pure JVM Kotlin** module (no Android dependencies), so the domain
and state machine are testable without a device or emulator.

## Layout

See ARCHITECTURE.md and docs/ARCHITECTURE_SPEC.md.

## Specs

All requirements carry IDs (`NMA-*`). Spec is the source of truth:
no spec → no code (SDD rule 1).
