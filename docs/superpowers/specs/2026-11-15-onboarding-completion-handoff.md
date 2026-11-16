# Onboarding completion handoff

The user approved a distinct successful-completion callback and a congratulations destination in the runnable demo on 2026-11-15.

## Intended experience
- The library exposes a dedicated success callback that is different from cancel/back/defer.
- Success remains server-authoritative: the callback is reachable only from the completed account destination after required facts and `onboarding_complete` have produced `OnboardingStep.COMPLETE`.
- The completed account screen keeps optional account actions and ID review available. Its **Continue** action invokes success; its top/back action invokes the existing exit/defer path.
- Existing consumers retain the original `EmailOnboardingHost` overload unchanged. The completion-aware overload adds `onOnboardingCompleted`; legacy callers keep the current exit behavior.
- The demo handles success by navigating to a dedicated congratulations page. That page is presentation-only, makes no identity-verification claim, and provides a clear return to the demo menu.
- A successful completion handoff clears any pending verification link from the demo navigation state.

## Approaches
1. Invoke success automatically as soon as server completion is observed. This is fastest but skips the completed account actions and makes completion navigation surprising.
2. Reuse the existing exit/defer callback for Continue. This preserves compatibility but cannot distinguish cancellation from success.
3. Add a completion-aware overload with a dedicated success callback and invoke it only from **Continue** on the server-confirmed account screen. Keep the original overload unchanged and use the existing exit/defer callback for back. **Selected** because it creates a clean semantic boundary without removing optional account actions or breaking positional/trailing-lambda callers.

## Non-goals
No change to backend completion rules, authentication proofs, optional/required policy, ID capture, passkey behavior, or HTTP APIs. No release, deployment, dependency, or toolchain change. No claim that onboarding completion means identity verification.
