package com.apexfission.android.attestra.auth.ui.onboarding.passkey

import com.apexfission.android.attestra.auth.library.R
import androidx.compose.ui.res.stringResource

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.attestra.auth.ui.onboarding.common.BodyText
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

/**
 * Screen indicating that the user's device does not support creating passkeys.
 */
@Composable
fun PasskeyUnsupportedScreen(onBack: () -> Unit, onDoLater: () -> Unit, onCheckAgain: () -> Unit, required: Boolean = false) {
    OnboardingFrame(
        title = stringResource(R.string.auth_passkey_setup_isn_t_available_here), icon = Icons.Default.KeyOff,
        step = "3/5", onBack = onBack,
        primaryLabel = if (required) stringResource(R.string.auth_check_again) else stringResource(R.string.auth_do_this_later), onPrimary = if (required) onCheckAgain else onDoLater,
        secondaryLabel = if (required) null else stringResource(R.string.auth_check_again), onSecondary = onCheckAgain,
    ) {
        OnboardingCard {
            BodyText(if (required) stringResource(R.string.auth_a_passkey_is_required_try_a_supported_device_or_password_manager) else stringResource(R.string.auth_try_a_supported_device_or_password_manager_you_can_add_a_passkey))
            BodyText(stringResource(R.string.auth_check_again_if_this_device_s_passkey_support_has_changed))
            Text(stringResource(R.string.auth_you_can_use_email_sign_in_while_no_passkey_is_registered))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun PasskeyUnsupportedScreenPreview() {
    AttestraAuthTheme {
        PasskeyUnsupportedScreen({}, {}, {})
    }
}
