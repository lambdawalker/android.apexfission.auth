package com.apexfission.android.auth.identity.capture

import androidx.compose.runtime.Composable

/** Compile-only consumer fixture: trailing lambdas must still mean cancellation. */
@Suppress("unused")
@Composable
private fun legacyCameraTrailingLambda() {
    IdentityCamera(onCaptured = { _, _ -> }, onError = {}) { /* cancel */ }
}
