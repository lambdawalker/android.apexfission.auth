# Publish Attestra auth

The shared `android-repo-setup` CLI owns setup, workflow generation, destination
configuration, release identities, reservations, confirmation and recovery.
Only `:auth` is published; `:app` remains the runnable demo. See [confirmed
installation](../IMPORT.md); no release is inferred from a local build.

## Tooling and ownership

Run `sh ./setup.sh` or `setup.bat` from any working directory. The generated
launcher locates this repository and installs the pinned tool. The immutable pin
in `.project/setup/config.yaml` matches the standalone documentation tool's source:
released v0.1.21 cannot read Auth's expanded Astro page/alias configuration.
Source pins require Git and Rust/Cargo. The separately installed Gradle publishing
plugins use v0.1.20, matching the Demo Common reference integration.

- `.project/setup/config.yaml`: repository, module, app, toolchain and verification contract.
- `.project/setup/repositories.yml`: destination IDs and GitHub environments.
- `gradle.properties`: Central identity and owner-selected public license metadata.
- `docs/templates/IMPORT.md.template`: installation prose; the CLI renders confirmed records.
- `.project/verification/checks.json`: immutable-source preflight commands.
- `scripts/signing/`: Auth APK/association verification and headless CI certificate fixtures.
- `scripts/screenshots/`: Auth scenario coverage, manual acceptance and visual review.
- `scripts/geography/`, `scripts/verification/`: Auth-specific dataset and build checks.

The generated Documentation workflow uses the shared main-only Pages policy and
`github-pages` environment, replacing the old `DOCS_PAGES_ENABLED` switch. Review
that hosting policy before merging.

Generic release engines and interactive Python wizards have been removed. Retained
helpers implement Attestra behavior the shared CLI does not supply. Historical
release tags retain their original source and scripts.

## Before the first public release

Choose and commit the actual project license; set `POM_LICENSE_NAME` and
`POM_LICENSE_URL`. They remain deliberately blank. Public/local publication rejects
missing license metadata. An isolated unsigned verification publication uses
explicit fixture POM metadata; it grants no license or publication authorization.
Review the proposed Central identity `io.github.lambdawalker:attestra-auth` and
verify namespace ownership. JitPack uses the configured repository name.

Run setup interactively only when ready to configure remote environments:

```sh
sh ./setup.sh setup-github-environments
sh ./setup.sh signing
```

The publishing wizard preserves existing values and protection rules. Central uses
`MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `SIGNING_IN_MEMORY_KEY` and optional
`SIGNING_IN_MEMORY_KEY_PASSWORD`. Public JitPack is secret-free. The `delayed-docs`
environment must have the 15-minute timer; inspect existing environments because
setup must not silently weaken their protections. No environment setup or package
publication is performed as part of this migration.

## Generate and verify configuration

```sh
sh ./setup.sh bootstrap check
sh ./setup.sh workflows --check
sh ./setup.sh publishing-config check
sh ./setup.sh module-release verify
sh ./setup.sh setup check
```

`setup check` also evaluates Android modules with Gradle. After reviewing a tooling
upgrade, run `workflows --overwrite`, `publishing-config generate` and
`bootstrap generate --overwrite`, then commit the generated changes. Keep Auth's
custom `verify.yml`, `signed-build.yml`, `auth-screenshots.yml` and screenshot
candidate workflow: they cover backend environments, public signing associations,
device flows and explicit baseline acceptance beyond generic CLI checks.

## Publish and recover

Use **Publish** from `main`, select the destination, and leave the
version blank for shared identity selection. Changed build inputs require a newer
version; destinations reuse the exact immutable source for the same version.
The shared engine reserves before upload and keeps uncertain upload journals.
Central upload is followed by the separate environment delay and public artifact
confirmation. JitPack confirmation checks the tagged source. Model assets are
compared against the pinned model AAR before publication; model classes are not
added to the runtime.

Use **SUB - Finalize Attestra Auth release** to finish a previously uploaded version without
uploading again. Use **Reconcile failed JitPack attempts** to preview stale attempts
before applying any proven-failure reconciliation. Never delete pending markers
merely because a job failed. Follow the [shared recovery contract](https://github.com/lambdawalker/gradle.android-repo-setup/blob/main/docs/release-reconciliation.md).

Confirmed metadata and installation guides advance only after public verification.
Documentation generation and deployment do not publish Maven artifacts.
