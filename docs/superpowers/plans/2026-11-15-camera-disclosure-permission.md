# Camera disclosure and permission implementation plan

**Goal:** After the user accepts the ID-capture disclosure, open Android's camera permission prompt without another application explanation/Continue screen.

**Architecture:** Add an explicit `IdentityCameraAfterDisclosure` entry that gates the existing camera. Only the live capture host uses it, when its policy-checked controller actually requests camera content. The existing `IdentityCamera` API and standalone demo keep their current permission explanation. Reuse the pinned permission library for denied/blocked recovery, and Android's result launcher for the first request.

**Tech stack:** Existing Kotlin/Compose, Activity Result API, lifecycle observers and pinned permissions library. No dependency or toolchain changes.

**Scope and alternatives:** Requesting permission in the generic onboarding host would also prompt during restored processing or custom non-camera integrations. Updating the shared permission library would require a separate release. The explicit camera entry avoids both. The user's approved friction audit supplies the UX requirement; these implementation choices are local defaults.

## Constraints and review focus

- Keep server disclosure/version checks, capture admission and final onboarding authority unchanged.
- Never instantiate camera content without a current grant; no-camera devices retain their existing fallback.
- Request automatically once per entry, not repeatedly on recomposition, denial, rotation or foreground resume.
- Keep explicit retry, cancel and Settings recovery after denial. Recheck the grant when returning from Settings.
- A saved review or already-processing capture must not request camera access.
- No live service requests, camera initialization or permission launches in screenshot fixtures.

## Completed tasks

- [x] Test initial request, existing grant, in-flight, denied, restored, background and no-camera routing. Verify a mutation restoring the explanation-first decision fails.
- [x] Add the pure route and native Compose adapter; wire only the live camera slot. Preserve existing camera entry signatures.
- [x] Add light/dark recovery scenarios using the production visual, JVM tests and Android recovery-action tests. Update feature guidance.
- [x] Generate both source metadata outputs using the pinned CLI. Run Android unit/build and instrumentation compilation, convention checks, and emulator interaction tests.
- [x] Inspect all four new screenshots; verify the previous 88 PNGs are unchanged; intentionally accept 92 baselines with traceable provenance.
- [x] Remove one-time preparation/acceptance workflows before delivery. Normal CI still validates without updating baselines.

## Verification evidence

- Local Kotlin routing runner: 41 cases pass. Changing the production initial route back to recovery causes the expected failure. Screenshot-tool tests: 8 pass. Resource tests: 3 pass.
- CI source `cbc9f98a89ec40de74875233e865cc220dba1f08`, workflow run `43746993933`: 190 auth and 34 app unit tests pass with no failures/errors/skips; debug/release APK builds and both instrumentation packages compile; repository convention checks pass.
- Emulator workflow `43747242117`: document-review and camera-recovery interaction tests, plus existing app entry/capture/security tests, pass on Android API 34. These exercise the recovery UI, not the real system permission dialog or camera hardware.
- Screenshot review: 56 composables, 50 scenarios, 92 images. Retry and Settings in light/dark mode were visually inspected for readable actions, clipping and expected state. All 88 prior PNGs match byte-for-byte; only four new recovery images were added. Candidate input hash: `4d4ca41d69add1b508dfc47cd23df005ba054039ecec00581b4d271dabc2f5d6`.
- Acceptance run `43747626260` verifies exactly those additions, validates input/pixel hashes, and restores/validates the accepted documentation provenance. No image baseline is silently accepted during ordinary validation.

The local container cannot run the pinned Gradle stack offline; the full Android checks above ran in CI. Native prompt dismissal, rotation during the actual prompt, manufacturer-specific denial behavior, permission changes in Settings and physical camera capture remain real-device smoke checks. No backend deployment or package publication was performed.
