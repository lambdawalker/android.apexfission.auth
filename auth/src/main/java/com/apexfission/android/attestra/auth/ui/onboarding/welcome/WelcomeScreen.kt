package com.apexfission.android.attestra.auth.ui.onboarding.welcome

import com.apexfission.android.attestra.auth.library.R
import androidx.compose.ui.res.stringResource

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.attestra.auth.ui.onboarding.common.BodyText
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.attestra.auth.ui.onboarding.common.StatusRow
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

/**
 * Screen displayed at the end of the initial onboarding to welcome the user.
 * Landing screen for a signed-in user who deferred the optional ID check.
 */
@Composable
fun WelcomeScreen(passkeyAdded: Boolean, onCheckId: () -> Unit) {
    OnboardingFrame(
        title = stringResource(R.string.auth_welcome_to_attestra),
        icon = Icons.Default.VerifiedUser,
        step = stringResource(R.string.auth_your_account),
        onBack = onCheckId,
        primaryLabel = stringResource(R.string.auth_open_id_check_status),
        onPrimary = onCheckId,
    ) {
        OnboardingCard {
            BodyText(stringResource(R.string.auth_your_account_is_ready_you_can_return_to_your_identity_check_whene))
            StatusRow(stringResource(R.string.auth_email_verified), stringResource(R.string.auth_you_can_use_your_email_to_sign_in))
            StatusRow(
                if (passkeyAdded) stringResource(R.string.auth_passkey_added) else stringResource(R.string.auth_passkey_not_added),
                if (passkeyAdded) stringResource(R.string.auth_you_can_sign_in_with_your_passkey) else stringResource(R.string.auth_you_can_add_a_passkey_later),
                complete = passkeyAdded,
            )
            StatusRow(stringResource(R.string.auth_identity_check), stringResource(R.string.auth_open_your_check_to_start_resume_or_retrieve_its_latest_result), complete = false)
            BodyText(stringResource(R.string.auth_identity_verification_is_optional_for_now_some_future_actions_may))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun WelcomeScreenPreview() {
    AttestraAuthTheme { WelcomeScreen(passkeyAdded = true, onCheckId = {}) }
}
