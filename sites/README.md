# Documentation maintenance

The shared Astro migration requires the corrective CLI source revision
`1da53392b262c7c3129d2bd645dfd7b34ead81e1` from
[shared tooling PR #37](https://github.com/lambdawalker/gradle.android-repo-setup/pull/37).
The published v0.1.21 asset lacks the page, translation and compatibility mappings
used here. The launcher builds the exact pinned source with Cargo; a later
verified release may replace this source pin.


The shared Android repository CLI owns Markdown staging, Astro/Starlight rendering,
English/Spanish navigation, link rewriting, raw Markdown and discovery output.
Canonical consumer guides remain in `docs/agents/`. The `site.pages` mapping in
`.project/docs/config.yaml` also includes the existing root, operational and
identity guides; those sources are not copies of consumer guides. A small current
`sites/astro/components/ScopeMarkdownContent.astro` presentation wrapper keeps the
three synthetic preview images readable in the horizontally scrollable table.

## Build and preview

Install Node.js 24 with npm. The pinned launchers obtain the documentation CLI;
full commit pins build the exact reviewed tooling source with Rust. A commit pin
used for migration fixes is distinct from the published v0.1.21 binary.

```sh
python3 sites/test_build.py
sh ./docs.sh check
sh ./docs.sh build --check
python3 sites/test_build.py --output
sh ./docs.sh serve
```

On Windows use `docs.cmd` for the same commands. `build --check` validates generated
links and anchors before replacing `sites/dist/`. `serve` previews the real project
subpath on loopback. Never commit generated `sites/dist` or CLI cache files.
The former local Python-Markdown/Pygments renderer and browser script are removed;
Python remains necessary for independent screenshot and source-contract checks.

## Hosting and repository identity

The intended public base is `https://lambdawalker.github.io/android.apexfission.auth/`,
matching the actual repository. It is **not claimed live**. The old renderer used
`android.attestra.auth` for canonical and source links; that stale repository identity
is no longer used for new output. Existing human and raw paths are preserved within
the actual project base. Hosting or redirecting the old project base itself requires
owner configuration at that old host; this migration does not claim it is deployed.

The Documentation workflow builds read-only on PRs and main pushes and uploads an
Pages artifact. The shared generated workflow deploys only from main, through the
`github-pages` environment, with Pages/id-token permissions confined to deployment,
deployment serialization and the current-main SHA guard. It uses the CLI's standard
deployment policy rather than the old `DOCS_PAGES_ENABLED` switch; configure Pages
and environment protections before enabling hosting. A documentation retry cannot publish an Android package or redeploy AWS.

## Versions, translations and compatibility paths

Auth has no confirmed published artifact. `sites/versions.json` records that existing
Development-only publication boundary; the generated `versions.json` exposes two
language scopes for `auth/development`. Shared history validation owns any future
immutable archive support; never infer publication from a sample APK version.

Spanish consumer prose remains in `docs/es/agents/`, with installation in
`docs/es/IMPORT.md`. `sites/translations.json` retains canonical hashes, actual
translation paths and review provenance. Shared configuration names that manifest
and maps translated paths explicitly. Relative links resolve from the actual
translated source and stay in the selected language/scope. Review meaning and
executable fences before updating any hash. Missing/stale translations show English
from the same selected Development revision with an explicit notice. Operational
guides using that fallback are not described as fully translated.

Current human entry: `/en/auth/development/` (Spanish: `/es/auth/development/`).
Scoped raw guides use `/en/auth/development/raw/<guide>.md`. Stable root entries
include `/agents/index.md`, `/IMPORT.md`, `/llms.txt` and `/versions.json`.
`site.aliases` preserves `/en/main/<source-path>.html`, `/es/main/<source-path>.html`
and `/raw/<language>/main/<source-path>.md` for every existing canonical source.
Raw aliases contain the same scoped Markdown as their shared canonical endpoints.
The three reviewed screenshot copies under `sites/astro/public/assets/` preserve old
`/assets/docs/assets/screenshots/` image URLs. Source tests enforce byte-for-byte
identity with canonical reviewed images; accepting screenshots remains explicit.

## Maintenance checks

```sh
sh ./docs.sh sources update
sh ./docs.sh sources check
python3 scripts/screenshots/suite.py coverage
python3 scripts/screenshots/suite.py verify-docs
python3 -m unittest discover -s scripts/screenshots -p 'test_*.py'
python3 sites/test_build.py
sh ./docs.sh build --check
python3 sites/test_build.py --output
```

`sites/test_build.py` checks Auth's source set, reviewed translation hashes, fence
identity and publication boundary. Its `--output` mode verifies the shared output,
legacy raw/human aliases, Spanish scope and fallback metadata. It does not implement
a renderer. Shared tooling owns general Markdown and navigation validation.

Generated declaration inventory is a retrieval aid, not a replacement for semantic
API contracts. Update [coverage](../docs/agents/coverage.md) and the appropriate
consumer guide when changing APIs. The [screenshot runbook](../docs/screenshots.md)
separates candidates, reviewed acceptance, regression validation and curated media.
An image/input hash mismatch fails independent screenshot validation; a site build
must never accept replacement screenshot baselines automatically.

Source behavior owns APIs; `IMPORT.md` owns source installation/release status;
`design.attestra` owns system requirements; the Go backend owns HTTP contracts.
