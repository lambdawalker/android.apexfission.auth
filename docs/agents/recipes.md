# Integration recipes

Scope: Development/source. Start with [quickstart](quickstart.md), not a guessed Maven version.

## Add ID capture to onboarding

Use the complete [LiveCaptureFlow](../../app/src/main/java/com/apexfission/android/attestra/auth/LiveCaptureFlow.kt).
It remembers `CaptureClients.api()` and `.uploads()`, binds `SecureCaptureCheckpointStore`
to account/backend, supplies the current access token only for that account, closes
both clients on disposal and connects `IdentityCamera` to `CaptureHost`.

Pass the disclosure reviewed on the existing Continue screen as `acceptedPolicy`;
set `requireReviewedPolicy = true`. Never silently accept a different policy version
just to avoid another user acknowledgement. `allowNewCapture` reflects fresh runtime
settings; it does not grant server authorization.

Expected flow: Continue → permission/camera → both photos ready → upload/file checks/OCR
while photo review stays open → photo confirmation → editable results or loading
while OCR finishes → server-confirmed **Details saved**.
Cancellation, 401, stale consent, invalid evidence and offline requests have distinct
recovery paths. Existing images should resume processing instead of reopening camera.

## Add discovery without leaking credentials

Use `environmentIndexClient()` and inject `EnvironmentCache`; close the client with
its owner. Resolve the environment before creating auth adapters. Reuse the sample's
[EnvironmentViewModel](../../app/src/main/java/com/apexfission/android/attestra/auth/EnvironmentViewModel.kt)
and [backend configuration](../../app/src/main/java/com/apexfission/android/attestra/auth/BackendConfiguration.kt)
for account-binding and runtime refresh responsibilities. Never reuse a bearer-bearing
client for public discovery. See [discovery](../environment-discovery.md).

## Customize screen UI

Reuse state-driven screen composables under `ui/onboarding`; wrap them in
`AttestraAuthTheme`. Supply callbacks that invoke the appropriate controller, not
optimistic success navigation. `OnboardingFrame` accepts optional primary/secondary
labels/actions and content; omitted actions stay absent. Callback execution occurs
from Compose UI interaction; launch suspend work in the owning scope.

Use [the UI catalog](../../auth/src/main/java/com/apexfission/android/attestra/auth/ui/onboarding/OnboardingCatalog.kt)
for visual examples and [screenshots](../screenshots.md) for deterministic fixtures.
The catalog's older identity-decision pages are labeled design mockups, not live outcomes.

## Clear local data

Use the sample's reviewed local reset flow. `SecureAuthStorage.clearLocalUser()`
does not revoke tokens or remove remote passkeys/evidence. Remote evidence deletion
requires capture cancellation through the authenticated API. Do not invoke an
infrastructure reset to test a single device's sign-out path.

## Troubleshoot without sensitive logs

Filter Logcat by `CARD-OCR` for capture metadata. Use the same capture ID
and UTC time in the backend worker/dispatcher logs. A `finalizing` record has not
reached extraction; investigate validation, storage and transaction errors first.
A `ready/pending` record is in extraction. See [troubleshooting](troubleshooting.md).

## Start OCR during photo review

Use `CaptureHost(earlyCamera = { events -> … })`; forward `events.onImagesReady`,
`events.onImagesInvalidated` and `events.onConfirmed` to the corresponding
`IdentityCamera` arguments, as shown in `LiveCaptureFlow`. The original `camera`
slot remains supported for hosts uploading after confirmation. Use one path per camera.

The pinned detector supplies borrowed bitmaps synchronously. Auth copies them
before returning, bounds the longest edge to 1600 pixels, encodes color JPEGs on a worker
dispatcher and caps each side at 500 KiB. Encoded arrays transfer to the controller;
do not retain or log them. Model assets remain independently pinned to `tfmodel~v0.1.2`.

`EarlyCaptureSession` keeps photo review open while processing. Retakes cancel local
work, clear the old draft and call authenticated `POST /onboarding/id/captures/{id}/retake`.
The controller mutex serializes replacements, including canceled requests returning late.
The same capture is reused without another admission; backend upload/retake limits apply.
Persisted retake operation keys recover uncertain responses. A checkpoint also records
whether photos were confirmed. Restarting before confirmation requires recapture because
local bitmaps are gone; restarting after confirmation resumes server processing.

Auth derives the expected finalized `image_revision` from the finalize revision contract.
Only an exact `(capture_id, image_revision)` match can seed the editable result; worker
status revisions may change independently. Confirmation does not upload again. It shows
matching results immediately or the existing loading screen while processing finishes.

Deploy backend retake and `image_revision` support before enabling `earlyCamera`.
An incompatible backend leads to recovery instead of presenting stale results.
`CARD-OCR` logs method/path, HTTP status, byte counts, elapsed milliseconds and capture metadata only.
Tokens, signed URLs, images and extracted card fields stay out of logs.

Card details use the backend wire name `issued_identifier`. The Kotlin property
`personalNumber` remains source-compatible; legacy `personal_number` responses are
also readable, while review requests emit `issued_identifier`.

Timing lines use `phase` and `elapsed_ms`. Bitmap copies, JPEG encoding, upload
registration and transfers are timed per side. Finalize, retake and retry are
timed as requests. `validation_observed` and `ocr_observed` measure the time
between status updates seen by Android, including polling and network delay.
A phase that starts and finishes between polls has no separate measurement.

For server-side timing, use the capture ID from the app log to locate the
extraction worker invocation. See [CloudWatch event names and timing boundaries](https://raw.githubusercontent.com/lambdawalker/go.appexfission.aws.card-capture/main/docs/agents/index.md).
