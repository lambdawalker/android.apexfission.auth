# Document review implementation plan

Goal: implement the approved document review and editable issuing jurisdiction end to end.
Architecture: additive string fields in shared OCR/review data, independent of capture policy. Compose uses generic document presets and an atomic location sheet. Host pins the resulting feature commit.
Tech stack: Kotlin/Compose/serialization, Go/Bedrock/DynamoDB.
Spec: ../specs/2026-11-12-document-review-design.md

## Global constraints
Preserve toolchain, consent and private upload lifecycle; no deploy or publication.

## Review focus
Old records omit new keys; custom types survive; blank optional values remain valid; OCR cannot overwrite edits; location sheet dismissal cannot commit changes.

## Tasks
- [x] Extend backend extraction/model/comparison and test JSON decoding, validation and round trips. Run Go test/race/vet and archive builds where toolchain permits.
- [x] Extend Android CardData, merge protection and tests; add type presets, generic fallback and atomic jurisdiction helper tests.
- [x] Replace live form with grouped fields, type dropdown, additional details and jurisdiction sheet. Add offline previews and screenshot coverage; align catalog review with the same controls.
- [x] Update integration docs, run documentation/localization checks, inspect diffs and deliver coordinated PRs with exact verification limits. Pin host dependency after the feature commit exists.

## Verification ledger
Go root and infrastructure: test -race and vet pass; capture/dispatcher/worker Lambda archives built. Android translation parity, screenshot inventory and screenshot tooling tests pass. Offline catalogue reduction and parent hierarchy tests pass. Site canonical/Spanish contract updated together.
Ruling: native execution in fresh task-branch clones; user explicitly requested implementation. No additional worktree or approval checkpoint needed.
Ruling: use an attributed offline GeoNames admin1/cities15000 snapshot plus free text rather than runtime geographic queries; smaller places and historical names require manual entry.
Ruling: preserve current private image lifetime rather than retaining document thumbnails for the review screen.
Review: independent reviewer found open-sheet reset by late OCR, missing region/city suggestion data and catalog unknown-title loss. Fixed with open-editor snapshot, offline parent-filtered catalogue and title preservation; regression tests added.
Android Gradle build and image rendering attempted with pinned Gradle/JDK; repository dependency resolution blocked by network before source compilation. Candidate generation and device tests remain pending; no accepted baselines changed. CI will exercise build and candidate workflows on the PR.
