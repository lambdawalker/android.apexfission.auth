package com.apexfission.android.auth.ui.onboarding.email

import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import com.apexfission.android.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

/**
 * Screen that handles the case where email verification succeeded but session creation failed.
 */
@Composable
fun EmailSessionRecoveryScreen(onBack: () -> Unit, onSignIn: () -> Unit) {
    OnboardingFrame(
        title = stringResource(R.string.auth_email_verified), icon = Icons.Default.MarkEmailRead, step = "2/5", onBack = onBack,
        primaryLabel = stringResource(R.string.auth_sign_in_to_continue_285), onPrimary = onSignIn,
    ) {
        OnboardingCard {
            BodyText(stringResource(R.string.auth_your_email_was_confirmed_but_this_device_could_not_finish_signing))
            BodyText(stringResource(R.string.auth_sign_in_with_an_email_code_to_continue_to_passkey_setup_you_do_no))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun EmailSessionRecoveryScreenPreview() {
    AttestraAuthTheme {
        EmailSessionRecoveryScreen({}, {})
    }
}
