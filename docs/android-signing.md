# Signing identities and domain associations

Each developer testing verified App Links (including email-verification onboarding) or real passkeys needs an approved signing identity and keeps a personal private signing key. Dev/QA can approve multiple developer fingerprints for the same package; teammates do not need to share keys or passwords. Ordinary UI development can retain its normal debug key. Only public certificate fingerprints are approved for each environment. No build automatically grants trust to a newly encountered certificate.

See [QA & Testing](qa-testing.md) for variant selection, device verification and troubleshooting.

## Guided setup

Run the shared setup CLI from an interactive terminal:

```sh
sh ./setup.sh signing
```

On Windows run `setup.bat signing`. The `signing.sh` / `signing.bat` aliases
forward to the same command. Source pins require Rust/Cargo and Git on first use.
Choose the APK signing-key operation, reuse your existing keystore/alias to retain
its certificate, and use the separate GitHub operation only when configuring CI.
The shared wizard reviews changes before applying them and keeps private setup
state in its encrypted vault.

The CLI exports public package/fingerprint JSON beside the vault under
`~/.projects/<repo-name>/secrets/setup-<id>.vault.associations.json` (or the explicit
state/vault override). Import the emitted public JSON through Attestra's association
manager for the intended backend environment. The signing environment does not
select a backend environment. This replaces the old Python wizard and its
`~/.attestra/associations` export convention; existing keys remain usable.

Use the generated key's actual path, store password, alias, and key password with
the shared `ANDROID_KEYSTORE_*` inputs below. The encrypted setup vault remains
owned by the CLI; Gradle consumes only the values supplied to the current build.
PKCS12 generally requires identical store/key passwords; use the existing
keystore's real values rather than changing its signing identity.

The remaining `scripts/signing/signing.py verify-apk` command checks the approved
certificate, package, and both published association relations. Its headless
`generate`/`export` commands are retained for automated certificate fixtures and
legacy scripts; interactive setup belongs to the shared CLI.

## Build using your persistent key

Signing is provided by the shared `com.apexfission.android-repo-setup.application` Gradle plugin. Auth configures only its alias and signed-debug behavior in `app/build.gradle.kts`; the reusable keystore handling lives in `gradle.android-repo-setup`. See [build configuration](build-configuration.md) for input sources and precedence.

Set these inputs in your process environment or repository-root `local.properties` (same uppercase names; environment values win per setting). See the [local.properties example](build-configuration.md#signing-settings).

Set `ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, and optionally `ANDROID_KEY_ALIAS` (Auth defaults it to `attestra`) and `ANDROID_KEY_PASSWORD` (defaults to the store password). You can supply passwords through your local secret manager/session, or the ignored local.properties file; the latter stores them as plaintext. Never check them in or put them in shell history. Then:

```powershell
.\gradlew.bat --no-configuration-cache :app:assembleDebug '-PattestraEnvironment=dev'
```

After Gradle validates the keystore credentials, the **signedDebug** variant also appears in Android Studio. Build it with `:app:assembleSignedDebug --no-configuration-cache`; see [signed debug variant](build-configuration.md#signed-debug-variant).

Both debug and release builds use the selected key when its path is supplied. Without it, normal debug signing and the existing unsigned release behavior remain. Disable the Gradle configuration cache when supplying signing secrets. The fingerprint comes from the certificate and stays the same across builds; a different key may require reinstalling the app, losing local app data.

## GitHub signed builds

The manual **Build approved Android APK** workflow uses a GitHub environment (`dev`, `qa`, etc.) and uploads an APK only after signature/package/public association verification succeeds. Protect environments that can access signing secrets and restrict permitted branches to trusted reviewed code. It is not triggered by pull requests.

Environment secrets:

- `ANDROID_KEYSTORE_BASE64`: base64 of the persistent CI keystore; encoding is not encryption, so store it as a secret.
- `ANDROID_KEYSTORE_PASSWORD` and optional `ANDROID_KEY_PASSWORD`.

Environment variables:

- `ANDROID_KEY_ALIAS` (defaults to `attestra`).
- `ANDROID_SIGNING_SHA256`: reviewed SHA-256 certificate fingerprint.
- `ATTESTRA_LINK_HOST`: exact environment app hostname, such as `qa.attestrabond.com`.
- Optional `ATTESTRA_INDEX_URL`; defaults to the shared index.

CI disables Gradle caching for this job and removes the downloaded keystore on exit. It does not upload keys to AWS, update approved fingerprints or publish to Play. For Play-distributed applications, register the **Play app signing** certificate, not merely the upload certificate; this APK workflow verifies directly distributed APKs.

Local verification uses Android SDK Build Tools (`ANDROID_HOME`, or explicit `--apksigner` and `--aapt` paths):

```powershell
python scripts/signing/signing.py verify-apk --apk app/build/outputs/apk/debug/app-debug.apk --expected-sha256 YOUR_APPROVED_FINGERPRINT --url https://dev.attestrabond.com/.well-known/assetlinks.json
```

The tool checks the APK signature, application ID and both association relations on the public HTTPS endpoint. It does not certify a successful Credential Manager ceremony or instant device-side re-verification. Test real passkey registration/sign-in and App Links on a device after deployment.

## Backend ownership

[Backend association hosting](https://github.com/lambdawalker/go.attestra.aws.auth/blob/feat/android-domain-associations/docs/android-associations.md) owns DNS, HTTPS, public files, revocation updates and teardown. Removing a fingerprint withdraws its association; it does not stop its owner signing APKs or revoke existing sessions. Private key rotation and backend session revocation remain separate procedures.

## Startup association warning

The demo app checks its actual installed signing certificates against the build-time link host's public `assetlinks.json` on startup. An unapproved package/certificate or missing relation produces a long toast naming the affected feature (passkeys, verified App Links, or both). This runs asynchronously and does not block onboarding. The check and notification survive activity rotation without repeating; a new activity ViewModel lifetime runs a fresh check.

Network, HTTP, or malformed-response failures produce an **approval unknown** toast instead of claiming that the certificate is unapproved. Successful website matching remains advisory: it does not establish Android's device-side verification state, confirm relying-party compatibility, or guarantee a successful passkey ceremony. Use the README’s device App Links checks if routing still fails. The check does not upload keys, fingerprints, or email tokens.
