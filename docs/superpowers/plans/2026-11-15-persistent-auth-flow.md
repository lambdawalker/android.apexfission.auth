# Persistent Auth Flow Implementation Plan

> Execute inline with superpowers:executing-plans; verify before completion.

**Goal:** Deliver the approved sign-in and optional account-security experience without repeated input or replacement loading/error pages.
**Architecture:** Transient, owner-bound returning-task controller; presentation-only Compose states; policy-driven account actions. Existing transports remain responsible for authentication, and the server remains responsible for completion.
**Tech Stack:** Kotlin, Compose, existing Ktor/Credential Manager and shared android.ui.common components.
**Spec:** ../specs/2026-11-15-persistent-auth-flow.md

## Global constraints
API 28 minimum, compile SDK 37, Java 17 bytecode. No dependency or release changes. Credentials remain in memory only. Existing library entry points and trailing-lambda integrations remain supported. Previews are offline.

## Review focus
Duplicate taps; late responses after account/environment exit; failed or throttled resend; ambiguous server acceptance followed by failed status refresh; a required step accidentally becoming skippable.

## Tasks
- [ ] Add controller regression tests, observe failure, implement transient state and guarded explicit operations. Cover code/password retry, cancellation, cooldown, challenge replacement, stale completion and cleanup.
- [ ] Add transport classification tests and preserve status metadata without response bodies. Implement one live sign-in surface with retained address/input and inline progress/errors.
- [ ] Keep passkey enrollment/recovery on one security screen; preserve required policy and explicit native action. Add required-only routing and account optional-action tests.
- [ ] Wire host, recovery, safe session restore and account actions. Preserve existing public wrappers; update source guides and central contract.
- [ ] Add offline light/dark and narrow/error fixtures plus Compose interaction tests. Register composable/scenario coverage, regenerate both source documentation outputs with the pinned CLI.
- [ ] Run local checks, full Android CI and review screenshot candidates before accepting baselines. Open focused PRs; no new-feature merge or production deployment.
