# API selection and contracts

Scope: Development/source. Exact declarations: [generated API inventory](api-inventory.md).
Source and tests outrank prose; service serialization belongs to the
[backend](https://raw.githubusercontent.com/lambdawalker/go.attestra.aws.auth/main/docs/agents/index.md).

## Preferred integration surface

| API / import package | Use for | Preconditions, effects and errors |
| --- | --- | --- |
| `EmailOnboardingHost` · `com.apexfission.android.auth.email` | Interactive auth/onboarding | Activity-scoped Compose owner, trusted API origin, validated runtime policy and link callbacks. Owns its remembered client/storage/controller; closes its client on disposal. Default ID slot is unavailable UI. |
| `parseVerificationLink` · `...email` | Validate incoming Android URI origin | HTTPS, expected host, `/verify-email`, default/443 port. Returns null on origin mismatch; the controller still validates proof format. Never trust arbitrary intent links. |
| `EmailFlowController` · `...email` | Custom email UI | Inject `EmailVerificationGateway`, `AuthStorage`; optional proof/clock inputs. Mutex-serialized suspend mutations; sessions are stored only after backend success. `screen` is observed Compose state. Do not equate request acceptance with verification. |
| `EmailVerificationApi` / `EmailHttpClient` · `...email` | Email HTTP transport | Caller owns the client; HTTPS origin. Returns structured session or `EmailApiError`; do not retry confirmation blindly without controller recovery. |
| `ReturnAuthApi` · `...passkey` | Existing-user email/passkey/password sign-in, refresh, status | Caller supplies/owns client and session handling. Error status does not imply proof or password may be logged. Account completion comes from server status. |
| `PasskeyRegistrationApi` / `AndroidPasskeyManager` · `...passkey` | Platform passkey enrollment and sign-in | Registration needs bearer authorization; manager needs Activity context and a configured provider. `PasskeyCancelled` and `PasskeyUnsupported` are distinct from success. Backend completion must follow platform response. |
| `EnvironmentIndex` · `...discovery` | Trusted public environment discovery | Caller owns anonymous client/cache. Cache key isolates index URL/environment; see discovery guide for bounded fallback and invalidation. |
| `AppConfiguration` / `OnboardingPolicy` · `...discovery` | Runtime requirements | Validate policy and environment binding. Invalid settings block new admission. `next()` uses server facts; local skips never bypass a required step. |
| `CaptureHost` / `CaptureController` · `...capture` | Stateful two-sided capture and review | Remember controller per account/backend. Host owns clients and checkpoint namespace; UI host disposes controller. `startCaptureOnEntry` defaults false, `requireReviewedPolicy` false: live sample deliberately passes true for both and carries reviewed disclosure. |
| `CaptureApi` / `CaptureClients` · `...capture` | Capture status, mutation and private PUT adapters | Separate anonymous upload client; validate returned S3 origin/headers. API client bearer token must belong to bound account. Caller closes both clients. |
| `IdentityCamera` · `...identity.capture` | Permission-gated detector integration | Real camera/device, model assets and explicit user entry. Transfers front/back JPEG arrays to callback. No successful identity decision is emitted. |
| `SecureAuthStorage` · `...email` | Local installation data | Keystore-backed encryption; unreadable values are absent. `clearLocalUser` is local-only. Host manifest must exclude sensitive preferences from backup. |

## Capture mutation details

`continueToCapture()` resumes an existing record before creating evidence.
`capturedBoth(front, back)` takes ownership of both arrays; unfinished upload bytes
remain only in memory for retry. `finalizeCapture()` records a stable operation
intent first; `restore()` reconciles uncertain responses. `retry()` is for eligible
backend failures, not a bypass for invalid/expired evidence. `cancel()` requests
backend deletion; `close()` only disposes local resources and is not deletion.

`editCard()` changes a local draft. `saveReview()` validates nonempty bounded fields
and supported origin, sends the recorded revision, then consumes server acknowledgement.
Conflict/retry paths retain dirty values. The finish screen requires the exact stored
fields and origin, not just `id_captured`. Metadata diagnostics may include capture ID,
state, revision and HTTP status; do not attach raw exceptions or personal values.

## UI-only versus runtime API

`ui.onboarding.*` screen composables accept callbacks and state; those callbacks do
not implement authentication. `OnboardingBusinessActions` is a UI-catalog placeholder.
Do not use its empty hooks as a production integration. `IdentityDetails` and older
identity-result mockups describe UI prototypes; they are not the capture HTTP model.
`CaptureHostContent`, `CardReviewForm`, sample entry screens and configuration locals
are internal rendering/host seams, not supported external integration contracts.

`CardData`, `CaptureRecord`, `CapturePolicy`, checkpoint and request models are
serializable transport/state types. Do not construct a forged `ready` or `reviewed`
record to make UI finish. Keep untrusted card text as text; validation does not establish
identity truth. See [capture guide](../identity-capture.md) for exact state handling.

## Development document review

The live `CaptureHost` review groups personal/document information and uses a
`CardData.documentType` dropdown independently of `CapturePolicy.documentType`.
Presets are `national_id`, `passport`, `drivers_license`, `residence_permit`,
`voter_id`, and `other` (Another document); `documentName` retains a printed/custom
title. Unknown type values use generic fields. Switching types retains all other
values. Additional fields expand initially when populated, and remain accessible
for any document type. Dates remain as printed; absent values may stay blank.

`CardData` adds optional `document_type`, `document_name`, `issuing_region`,
`issuing_city`, `issuing_authority` wire keys. Existing records default to blank.
These are review data, not document/identity authenticity assertions. The backend
contract belongs to [card capture](https://github.com/lambdawalker/go.appexfission.aws.card-capture).
Deploy its matching revision in the host before sending new review keys; older
strict servers reject unknown fields. Android upgrades alone do not update AWS.

The Issued in row opens an editor for country, optional region and optional city.
The country selector offers localized offline country suggestions. Region/city
selectors search an offline GeoNames snapshot (administrative regions and cities
above 15,000 population or capitals), filtered by selected parents. The catalogue
loads off the main thread. Its source hashes, CC BY 4.0 attribution and reproducible
reduction script are retained in `assets/geonames-NOTICE.txt` and
`scripts/geography/build_catalogue.py`. It is a suggestion list, not an authority
on political boundaries or an exhaustive municipality directory. All three support
free text, including when the catalogue cannot load. Changing a parent clears its descendants in the temporary editor;
Use this location applies the whole selection, while dismissal cancels it.
Reviewed jurisdiction fields are protected together against later OCR updates.
Issuing authority is a separate optional field, and holder `origin_country`
retains its existing independent policy and validation.

The form inherits the host theme. Document images are not retained or fetched for
review thumbnails; the existing private-upload lifetime is unchanged. Rescan uses
the existing versioned retake operation. Offline screenshot scenarios cover both
the review and the location editor; new baselines require rendered review and
explicit acceptance rather than regenerating accepted images during validation.
