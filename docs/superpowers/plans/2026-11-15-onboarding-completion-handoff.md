# Onboarding Completion Handoff Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Separate successful onboarding completion from cancellation and route the demo to a congratulations page.
**Architecture:** Keep server-authoritative routing unchanged. Split the completed-account screen's back and primary actions, expose an optional success callback on `EmailOnboardingHost`, and let the demo own post-success navigation.
**Tech Stack:** Kotlin, Jetpack Compose, existing Attestra auth UI components and screenshot tooling.
**Spec:** ../specs/2026-11-15-onboarding-completion-handoff.md

## Global Constraints
API 28 minimum, compile SDK 37, Java 17 bytecode. No dependency/toolchain changes. Server `onboarding_complete` remains authoritative. Existing consumers retain source-compatible behavior. No identity-verification claim.

## Review Focus
- Back from the completed account screen must never report success.
- Continue must report success exactly once per user action.
- The success callback must not be reachable before server-confirmed completion.
- Existing callers without the callback must retain their prior exit behavior.
- Demo congratulations navigation must clear any consumed/pending verification link and be safely escapable.

---

### Task 1: Separate completed-account back and success actions

**Files:**
- Modify: `auth/src/androidTest/java/com/apexfission/android/auth/signin/AuthTaskScreensTest.kt`
- Modify: `auth/src/main/java/com/apexfission/android/auth/ui/onboarding/account/AccountTaskScreen.kt`

**Interfaces:**
- Produces: `AccountTaskScreen(..., onContinue: () -> Unit, ..., onBack: () -> Unit = {})`

- [ ] Write a failing Compose test proving the top/back action does not invoke `onContinue`.
- [ ] Run the targeted device test and verify the current implementation fails that assertion.
- [ ] Add a distinct `onBack` callback and wire the frame back action to it.
- [ ] Verify both back and Continue behavior.

### Task 2: Expose the successful-completion callback

**Files:**
- Modify: `auth/src/main/java/com/apexfission/android/auth/email/EmailOnboardingHost.kt`
- Modify: `docs/agents/migration.md`
- Modify: `docs/runtime-configuration.md`

**Interfaces:**
- Produces: a completion-aware `EmailOnboardingHost(..., onOnboardingCompleted: () -> Unit, ...)` overload while preserving the original overload unchanged
- Consumes: server-confirmed `OnboardingStep.COMPLETE` routing already used for the completed account screen.

- [ ] Preserve the original overload unchanged; add a completion-aware overload whose required callback precedes the final composable parameters.
- [ ] Wire completed-account Continue to success and back to the existing exit/defer path.
- [ ] Document the semantic distinction and source-compatible default.
- [ ] Run JVM/build checks.

### Task 3: Add the demo congratulations destination

**Files:**
- Modify: `app/src/main/java/com/apexfission/android/auth/MainActivity.kt`
- Create: `app/src/main/java/com/apexfission/android/auth/OnboardingCongratulationsScreen.kt`
- Modify: `app/src/androidTest/java/com/apexfission/android/auth/OnboardingEntryScreenTest.kt`
- Update screenshot fixtures/coverage if required.

**Interfaces:**
- Consumes: `EmailOnboardingHost.onOnboardingCompleted`
- Produces: demo-only `EntryScreen.Congratulations`

- [ ] Add the demo destination and congratulations UI with no identity-verification claim.
- [ ] Wire success to the destination and clear incoming-link state.
- [ ] Add an Android UI test for the congratulations page and return action.
- [ ] Register visual coverage and review any changed screenshot candidates.

### Task 4: Documentation and final verification

- [ ] Regenerate API/source documentation with the pinned docs CLI.
- [ ] Run unit tests, Android build/instrumentation APK compilation, documentation/tooling/translation checks, and screenshot validation.
- [ ] Inspect changed screenshots before baseline acceptance.
- [ ] Open a focused PR; no deployment or publication.
