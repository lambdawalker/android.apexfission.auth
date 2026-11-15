# Camera disclosure and permission implementation plan

> Implement inline with test-driven checks and verify the resulting branch before claiming completion.

**Goal:** After the user accepts the ID-capture disclosure, open Android's camera permission prompt without another application explanation/Continue screen.

**Architecture:** Add an explicit `IdentityCameraAfterDisclosure` entry that gates the existing camera. Only the live capture host uses it, when its policy-checked controller actually requests camera content. The existing `IdentityCamera` API and standalone demo keep their current permission explanation. Reuse the pinned permission library for denied/blocked recovery, and Android's result launcher for the first request.

**Tech stack:** Existing Kotlin/Compose, Activity Result API, lifecycle observers and pinned permissions library. No dependency or toolchain changes.

**Scope and alternatives:** Requesting permission in the generic onboarding host would also prompt during restored processing or custom non-camera integrations. Updating the shared permission library would require a separate release. The explicit camera entry is the smallest change that avoids both. The user's approved friction audit supplies the UX requirement; these implementation choices are local defaults.

## Constraints and review focus

- Keep server disclosure/version checks, capture admission and final onboarding authority unchanged.
- Never instantiate camera content without a current grant; no-camera devices retain their existing fallback.
- Request once per entry, not on recomposition, denial, rotation or foreground resume.
- Keep retry, cancel and explicit Settings recovery after denial. Recheck the grant when returning from Settings.
- A saved review or already-processing capture must not request camera access.
- No live service requests, camera initialization or permission launches in screenshot fixtures.

## Tasks

- [x] Test the permission-routing decision first: initial request, existing grant, in-flight, denied, rotated/restored, background and no-camera cases. Observe the first-request regression fail against the old explanation-first decision.
- [x] Add the smallest route implementation and native Compose adapter; wire only `LiveCaptureFlow` to the new opt-in camera entry. Preserve all existing camera entry signatures.
- [x] Add light/dark recovery previews, coverage mapping and screenshot scenarios using the same production recovery visual. Update capture guidance with source/deployment limits.
- [ ] Run local Kotlin decision checks and repository Python checks; run full Gradle unit/build/device compilation and screenshot/docs validation in CI. Generate source inventory with the pinned CLI, inspect changed screenshots, and remove one-time maintenance jobs before delivery.

## Evidence

Record actual test outputs and CI status in the PR. A host screenshot is not evidence of native permission-dialog behavior or real-camera correctness.

Local evidence: 41 Kotlin routing cases passed; 8 screenshot-tool tests and 3 resource tests passed; coverage is 56 composables / 50 scenarios / 92 images. The convention checker invokes Gradle and cannot complete in the offline local container; CI must run it with the pinned toolchain. No native permission-dialog result is claimed.
