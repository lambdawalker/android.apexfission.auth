package com.apexfission.android.auth.identity

import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.runtime.Composable
import com.apexfission.android.auth.ui.onboarding.common.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.MaterialTheme

/**
 * Screen that informs the user that the ID capture integration is currently unavailable.
 */
@Composable
fun IdentityIntegrationUnavailable(onExit: () -> Unit) {
    OnboardingFrame(stringResource(R.string.auth_identity_check_is_not_available_yet), Icons.Default.Badge, stringResource(R.string.auth_optional_id_check), onExit,
        primaryLabel = stringResource(R.string.auth_return_to_account), onPrimary = onExit) {
        OnboardingCard { BodyText(stringResource(R.string.auth_you_can_continue_using_your_account_identity_verification_will_be)) }
    }
}

@Preview
@Composable
private fun IdentityIntegrationUnavailablePreview() {
    MaterialTheme {
        IdentityIntegrationUnavailable(onExit = {})
    }
}
