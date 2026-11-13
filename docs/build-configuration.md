# App build configuration

`app/build.gradle.kts` declares the app's identity, SDK levels, dependencies and
Attestra-specific bindings. Reusable application conventions come from the
published `com.apexfission.android-repo-setup.application` plugin in
`gradle.android-repo-setup`.

The shared plugin owns two generic mechanisms:

| Convention | Responsibility |
|---|---|
| `endpoints` | Reads public runtime properties, validates them, and binds them to `BuildConfig` fields and/or manifest placeholders |
| `signing` | Validates an optional persistent Android keystore and configures debug/release signing plus the optional `signedDebug` variant |

Auth declares its Attestra property names, defaults, validation rules, manifest
placeholder, default signing alias, and debug-source reuse in `app/build.gradle.kts`.
The encrypted setup-vault format remains private to the CLI and is not parsed by
Gradle.

## Endpoint settings

Every public setting uses the same precedence: **Gradle project property → selected exported file → declared binding default**. Gradle project properties include `-P` and normal Gradle properties files. An explicit empty value remains an override; required export fields still reject blanks.

`attestraConfigFile` is a Gradle property pointing to a backend-generated UTF-8 `.properties` file. Relative paths resolve from the repository root. The file must exist and retain the existing required environment/API/host/region/Cognito/capture fields.

| Property | Default without export/override |
|---|---|
| `attestraEnvironment` | `dev` |
| `attestraIndexUrl` | `https://index.attestrabond.com/v1/environments` |
| `attestraApiBaseUrl` | Empty: discover using the index |
| `attestraLinkHost` | `<environment>.attestrabond.com` |
| `attestraAwsRegion` | Empty |
| `attestraCognitoUserPoolId` | Empty |
| `attestraCognitoClientId` | Empty |
| `attestraCaptureEnabled` | `false` |
| `attestraCaptureDocumentType` | `sample_card` |

Do not store secrets in this exported file: these values become app configuration. A nonblank API URL bypasses index discovery. Existing URL, environment and boolean validation is preserved.

```powershell
.\gradlew.bat :app:assembleDebug '-PattestraEnvironment=qa'
.\gradlew.bat :app:assembleDebug '-PattestraConfigFile=D:/config/qa.properties'
```

`attestraCaptureEnabled` and `attestraCaptureDocumentType` remain accepted for export compatibility. Live app behavior comes from [runtime settings](runtime-configuration.md), including in fixed API mode.

## Signing settings

Signing inputs use **process environment variable → root `local.properties` → default**, independently for each setting. Use the same uppercase names in both sources. An explicitly empty environment value overrides the file; an empty keystore path disables registered signing. Signing values are never read from `-P`, `gradle.properties`, or the endpoint export.

| Environment variable / local.properties key | Behavior |
|---|---|
| `ANDROID_KEYSTORE_PATH` | Enables registered signing; relative paths resolve from the repository root |
| `ANDROID_KEYSTORE_PASSWORD` | Required when the path is supplied |
| `ANDROID_KEY_ALIAS` | Defaults to `attestra` |
| `ANDROID_KEY_PASSWORD` | Defaults to the store password |

For Android Studio, add the following to the repository-root `local.properties`, preserving any existing `sdk.dir` line. This file is already ignored by Git. Use forward slashes in Windows paths (or double each backslash); Java properties interpret single backslashes as escapes.

```properties
ANDROID_KEYSTORE_PATH=C:/Users/D/.projects/android.apexfission.auth/secrets/attestra-setup-1234567890abcdef.jks
ANDROID_KEYSTORE_PASSWORD=your-keystore-password
ANDROID_KEY_ALIAS=attestra
# Optional if different from the store password:
# ANDROID_KEY_PASSWORD=your-key-password
```

File-stored passwords are plaintext; you can keep only the path/alias here and supply passwords through the environment instead. Never commit the file. Resync Studio after changes. Registered signing also requires `org.gradle.configuration-cache=false` in your user Gradle properties for IDE builds; `--no-configuration-cache` applies to command-line builds.

Without a nonblank keystore path, debug keeps its normal Android debug key and release remains unsigned. When the complete signing identity is valid, debug and release use `registered`. A missing file, missing or incorrect password, invalid alias, unreadable keystore, or configuration-cache conflict does not block Gradle sync. Registered signing is simply unavailable for that sync and the shared plugin logs which input failed, where that value came from, and what to correct. Keep keystores outside the repository and obtain secrets from a trusted local secret provider/session. No passwords are committed by this change.

```powershell
# After supplying the signing variables in the current process:
.\gradlew.bat --no-configuration-cache :app:assembleDebug '-PattestraEnvironment=dev'
```

Studio must inherit these variables when launched; typing them in the embedded terminal doesn't update the IDE process. The Gradle plugin deliberately does not open the encrypted setup vault; the CLI owns that secret store. Register the chosen certificate in the environment's assetlinks.json before testing App Links/passkeys.

## Adding another feature

Add Attestra-specific public bindings to the `androidRepoSetup { endpoints { ... } }`
configuration in `app/build.gradle.kts`. If a mechanism is useful across Android
applications, generalize it in `gradle.android-repo-setup` instead of creating a
new project-local precompiled convention script. Keep credentials out of endpoint
bindings because those values are compiled into the APK.

CI runs `scripts/verification/verify_conventions.py`: default/index mode, exported
values, explicit overrides, invalid inputs, signing-cache rejection and a real APK
signed with a disposable certificate. This does not deploy infrastructure or
register that certificate with any environment.

## Signed debug variant

When the configured keystore exists and its store password, alias and key password unlock a private key with a certificate, sync creates **signedDebug**. Select it in Android Studio's Build Variants window, or run:

```powershell
.\gradlew.bat --no-configuration-cache :app:assembleSignedDebug '-PattestraEnvironment=dev'
```

It is debuggable, shares the debug capture-mock source and dependencies, and resolves library dependencies through their debug variants. It keeps the same application ID so approved App Links/passkey associations still match. It does not select a server environment by itself.

Without a usable signing identity the variant is absent. Invalid configured credentials do not fail sync and do not create `signedDebug`; correct the warning's identified input and resync to make the variant appear. The normal debug variant keeps its standard Android debug signing and release keeps its existing signing state while registered signing is unavailable. Validation supports Java-detectable JKS/PKCS12 files and checks credentials locally without uploading them.
