# Contributing — Naze Motion Agent

## Method: Spec-Driven Development
1. No requirement without an ID (`NMA-<AREA>-###`) in docs/*_SPEC.md.
2. No code without a spec (RULE 1).
3. Spec change → design change → implementation change → test change (RULE 3).
4. Never break the action schema unless the spec explicitly changes (RULE 10).

## Commits
Small, measurable, conventional:
`docs:`, `feat:`, `test:`, `fix:`, `chore:`, `refactor:`.
No giant commits.

## Hard rules
- Domain layer: no Android imports (no Context, View, AccessibilityNodeInfo).
- AI layer: no direct access to AccessibilityService or Context.
- Accessibility layer: no knowledge of AI providers.
- Target-specific logic (Alight Motion) never enters core/.
- No giant classes, no God objects, no infinite retry/loop (see SECURITY_SPEC).
