# Persistent sign-in and account setup

The user approved the two sign-in/passkey mockup boards on 2026-11-15. Preserve the current theme and shared UI components; the boards define interaction, not replacement branding or platform chrome.

## Intended experience
- Returning sign-in is one task surface. Email stays visible when selecting code/password/passkey, while progress, validation errors and connection failures appear in place.
- Retain codes/passwords in transient memory across recoverable requests. Do not serialize them into saved instance state, secure storage, logs or analytics. Clear on success, method/account/environment change and exit.
- Submission is explicit; do not automatically retry authentication, send another email, or create a passkey. Prevent overlapping requests and ignore stale results after a task is abandoned.
- Only explicit resend requests rotate the email challenge; provide a local cooldown and respect rate-limit responses. Preserve leading zeroes and paste/autofill semantics.
- Passkey opening, saving, cancellation, unsupported and retryable failure remain on the security task screen. Retain the native Credential Manager prompt and required-policy enforcement.
- Required account facts and server-confirmed completion remain authoritative. Optional incomplete security/capture actions are accessible from the completed account screen rather than forced sequential skip pages. Legacy policy routing API remains available to other consumers.
- Sign-in recovery opens the actual sign-in form with context. A temporary session refresh failure must not erase a saved session.

## Approaches
Keep existing screen replacements (small but preserves the friction), hoist a handful of ad-hoc flags into the large host (smallest diff but difficult race testing), or introduce a small transient returning-task controller and presentation-only task screens. Use the third option. Keep backend transport and existing public screen wrappers intact; no backend/provider/API contract change.

## Non-goals
No auto-submit, automatic retries, changed authentication requirements, new releases, deployment, ID-capture sequencing change, or claimed identity verification. No new dependencies or toolchain changes. Retain API 28, compile SDK 37, Java 17 bytecode and all committed tool pins.
