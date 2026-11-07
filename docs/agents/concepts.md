# Concepts and resource ownership

Scope: Development/source. [Architecture owner](https://raw.githubusercontent.com/lambdawalker/design.attestra/main/architecture.md).

## Modules and boundaries

`:auth` owns UI, controllers, Android storage and HTTP adapters. `:app` owns
navigation, Android manifest links, environment binding and privacy-window policy.
Both app variants share the live capture runtime in `app/src/main`; there is no local mock module.
The Go backend owns authorization, durable capture/extraction and account facts.

## Email and returning sessions

`EmailFlowController` owns UI state and a request mutex. `ProofGenerator` creates
local proof A and its S256 challenge; `AuthStorage` retains pending proof and session.
A matching email link B can use A. If A is absent, code C + B is the manual path.
`ReturnAuthApi` owns HTTP exchanges, not the client's lifetime or host navigation.
`AndroidPasskeyManager` delegates credentials to Android Credential Manager;
cancellation is not successful registration. The service must acknowledge completion.

## Configuration authority

`EnvironmentIndex` resolves a trusted public environment and caches only validated
public configuration. An index failure is not permission to use an unrelated host.
`AppConfiguration` supplies runtime requirements and password placement. Required
steps cannot be skipped; disabled enrollment does not remove existing password sign-in.
The backend remains authoritative even when public settings are cached.

## Capture state versus identity assurance

`uploading` accepts front/back evidence; `finalizing` checks the files. `ready`
means evidence passed those checks, not that parsing or identity validation succeeded.
Extraction `pending` displays progress, `complete` seeds review, and `failed` opens
empty manual entry. Only the matching server `reviewed` object acknowledges saved
fields and origin. A saved form is not an identity-verification decision.

## Ownership and lifecycle

| Resource | Creator/owner | Cleanup and limits |
| --- | --- | --- |
| HTTP clients | Host or `EmailOnboardingHost` | The creator closes them; API adapters do not |
| `CaptureController` | Host, remembered per account/backend | `CaptureHost` closes it on disposal; `close()` wipes pending JPEG arrays |
| Front/back JPEG arrays | Camera callback transfers to controller | Controller retains retry data in memory, wipes it after use/exit; caller must not reuse |
| Camera result bitmaps | `IdentityCamera` consumes detector results | Copies borrowed images before background encoding; each color JPEG is bounded to 1600 px on the longest edge and 500 KiB; recycles owned copies and confirmed detector results |
| Pending password | Onboarding Compose memory | Never saved state/storage/URLs/logs; cleared on relevant exits and account changes |
| Pending proof/session | `SecureAuthStorage` | AES-GCM under per-install Android Keystore key; host must exclude backing preferences from backup |
| Capture checkpoint | `SecureCaptureCheckpointStore` | IDs and mutation intents only; namespace by account/backend, never signed URLs or images |
| Flow coroutines | Compose owner | Cancel when owner is disposed; cancellation does not prove a remote request failed |

Controllers are UI-owner objects, not a thread-safe multi-consumer service. Request
mutexes avoid overlapping mutations but do not establish arbitrary multi-threaded
use. Call them from the UI owner's coroutine scope. Do not block UI work with custom
I/O; suspend APIs use their client's engine. Storage uses synchronous preference
commits, so do not repeatedly recreate/read it for every recomposition.

`clearLocalUser()` clears this installation only. It does not revoke remote sessions,
delete server evidence or delete platform passkeys. Changing backend/account binding
requires local isolation before constructing authenticated clients.


## Host appearance and language

Place `EmailOnboardingHost`, `CaptureHost` and individual screens inside the host's
Material 3 `MaterialTheme`. Reusable screens inherit its color scheme, typography
and shapes; action and code-cell minimum heights allow text to grow. Layout spacing
remains in density-independent pixels because MaterialTheme has no spacing scale.
`AttestraAuthTheme` is an explicit branded theme for the sample and previews; callers
do not need to apply it.

Library UI resources follow the host Android configuration: English fallback,
German, Spanish, French, Italian, Japanese, Korean, Polish, Portuguese, Russian,
Ukrainian, Simplified Chinese and Traditional Chinese. Controller/password-policy
String APIs retain their existing English values for compatibility; library screens
resolve known messages at rendering time, including formatted arguments. Unrecognized host/service
text passes through unchanged. A custom UI consuming controller strings directly
owns its localization. Capture disclosures remain service-provided content.
