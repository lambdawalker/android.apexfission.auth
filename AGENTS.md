# Working on Attestra Android auth

Read [consumer agent entry](docs/agents/index.md) to integrate this library;
this file is for contributors. Apply the Android and system-integrated rules in
[agents.prompts](https://github.com/lambdawalker/agents.prompts).

- `:auth` owns Android APIs and implemented UI; `:app` owns the runnable host. Both app variants use the live capture runtime
  in `app/src/main`; offline previews must not initialize services.
- `design.attestra` owns system requirements; the Go backend owns HTTP contracts.
  Do not change intended architecture to hide an implementation defect.
- Read [IMPORT.md](IMPORT.md) before choosing dependency versions. Auth has no
  confirmed published artifact; do not invent Maven/JitPack coordinates or infer
  a release from the sample APK version. Dependency libraries have independent releases.
- Preserve API 28, compile SDK 37, Java 17 bytecode and the pinned Gradle/Kotlin/AGP
  toolchain. A new artifact publication needs separate authorization.
- Preserve local-proof + email-link confirmation, server-authoritative completion,
  capture disclosure/version consent, account/environment isolation and private uploads.
- Call controllers from their UI owner; retain and close owned clients/controllers.
  Never log passwords, tokens, image bytes or personal card fields.
- Every composable must be listed in `docs/screenshots/coverage.json`. Provide an
  offline preview or document its runtime delegation to a covered visual component.
  Never start a camera, Keystore, network request or navigation service in a preview.
- Screenshot candidates, reviewed acceptance and baseline validation are separate
  commands; ordinary validation must not overwrite accepted images.
- Canonical consumer guides are `docs/agents/`; Spanish prose lives in
  `docs/es/agents/` and records reviewed canonical hashes in `sites/translations.json`.
  Never refresh translation hashes without reviewing meaning and executable fences.
- Update API/feature coverage, relevant guides, screenshots and tests with behavior.
  Do not hand-edit generated `sites/dist/`, API inventory or candidate manifests.

## Shared tooling

Use `.project/setup/config.yaml` and generated `setup.sh` / `setup.bat` for shared
setup, signing, release and workflow operations. Keep independent documentation
configuration under `.project/docs/`. Do not restore duplicated Python release
engines or wizards. Preserve Auth-specific signed APK association verification,
backend configuration checks, device tests and manual screenshot acceptance.
The source pin matches the docs pin because released v0.1.21 rejects its expanded
Astro configuration. No package or remote environment changes are authorized by
ordinary tooling validation.

## Checks

```sh
sh ./setup.sh bootstrap check
sh ./setup.sh workflows --check
sh ./setup.sh publishing-config check
sh ./setup.sh module-release verify
python3 -m unittest discover -s scripts/signing
sh ./docs.sh sources check
python3 scripts/screenshots/suite.py coverage
python3 -m unittest discover -s scripts/screenshots -p 'test_*.py'
python3 sites/test_build.py
sh ./docs.sh build --check
python3 sites/test_build.py --output
bash gradlew :auth:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease
```

See [screenshot commands](docs/screenshots.md) and [site maintenance](sites/README.md).
Use a task branch and push a PR by default. Do not merge, publish artifacts, change
release tags, or deploy production without corresponding authorization.
