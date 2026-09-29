# CI & APK Build

## CI (`.github/workflows/ci.yml`)
Runs `gradle :core:domain:test :core:state:test` on every push to main and on
every PR. Pure JVM — no emulator needed (NMA-TEST-014).

## Debug APK (`.github/workflows/build-apk.yml`)
- Trigger: push to main, or manual (workflow_dispatch).
- Requirement: Android `app` module must exist (Phase 10 per ROADMAP).
- Until then the job **skips cleanly** — it will not fail the build.
- Once `app/` and `include(":app")` exist, every push builds a debug APK,
  uploaded as artifact `naze-motion-agent-debug-apk` (14-day retention),
  downloadable from the Actions run page under **Artifacts**.

## Recommended later additions (when :app exists)
- Gradle wrapper (`gradle wrapper`) so workflows can use `./gradlew`.
- Signing: secrets `SIGNING_KEYSTORE_BASE64`, `SIGNING_KEY_ALIAS`,
  `SIGNING_KEY_PASSWORD`, `SIGNING_STORE_PASSWORD` (NMA-SEC-001: never
  commit keystores).
- Release builds on tags (`v*`).
