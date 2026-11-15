# UI Common source migration

This branch replaces Demo Common directly with [UI Common](https://github.com/lambdawalker/android.ui.common).
`0.0.0-SNAPSHOT` is an unpublished substitution key, not a released dependency. **Do not merge or publish
this migration until the first UI Common release is confirmed and the version catalog is updated.**

For source integration, check out the exact UI Common revision from the integration workflow and run:

```sh
./gradlew :app:assembleDebug -PuiCommonCheckout=/absolute/path/to/android.ui.common
```

The integration workflow pins the shared source by commit. The normal release/build workflows remain
unchanged and need a confirmed dependency before they can pass without this explicit checkout.
Remove the temporary substitution when switching to the published artifact; do not add a global
mavenLocal fallback or a floating Git dependency.

CatalogScreen replaces DemoScreen, ContentCard replaces the content overload of DemoCard, ActionCard
replaces its action overload, and SectionHeading replaces CardTitle. Settings labels/navigation belong
to each app. UI Common inherits the host MaterialTheme. No compatibility wrappers are retained.

Auth also delegates email confirmation and sign-in cards to CodeVerificationCard. The reusable field
and normalization tests now live in UI Common. Auth retains backend calls, validation, timers, navigation,
email autofill hints and the existing outer Verify/Resend actions. UI Common is an implementation
dependency of :auth and an explicit dependency of :app for catalog components.

Existing accepted screenshot metadata remains historical evidence for its original source. Re-render
and review affected consumer screens before accepting new baselines; do not rewrite historical
acceptance hashes to make the migration appear validated.

Pinned implementation: `589b1925bb90ba5db45d4a22c5abcf7bffafe7c9` ([UI Common PR #2](https://github.com/lambdawalker/android.ui.common/pull/2)).
