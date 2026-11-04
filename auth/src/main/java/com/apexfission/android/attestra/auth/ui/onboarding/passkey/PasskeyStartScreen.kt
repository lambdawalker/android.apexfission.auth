package com.apexfission.android.attestra.auth.ui.onboarding.passkey

import com.apexfission.android.attestra.auth.library.R
import androidx.compose.ui.res.stringResource

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.attestra.auth.ui.onboarding.common.BodyText
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.attestra.auth.ui.onboarding.common.StatusRow
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

/**
 * Screen that prompts the user to create a passkey for their account.
 */
@Composable
fun PasskeyStartScreen(onBack: () -> Unit, onCreatePasskey: () -> Unit, onDoLater: () -> Unit, required: Boolean = false) {
    OnboardingFrame(
        title = stringResource(R.string.auth_protect_your_account_with_a_passkey), icon = Icons.Default.Key,
        step = "3/5", onBack = onBack,
        primaryLabel = stringResource(R.string.auth_create_passkey), onPrimary = onCreatePasskey,
        secondaryLabel = if (required) null else stringResource(R.string.auth_do_this_later), onSecondary = onDoLater,
    ) {
        OnboardingCard {
            BodyText(stringResource(R.string.auth_start_passkey_setup_and_follow_the_instructions_from_your_device))
            StatusRow(stringResource(R.string.auth_faster_sign_in), stringResource(R.string.auth_use_a_passkey_on_a_supported_device))
            StatusRow(stringResource(R.string.auth_no_password_to_remember), stringResource(R.string.auth_you_can_choose_where_your_passkey_is_saved))
            BodyText(stringResource(R.string.auth_we_will_mark_the_passkey_as_added_only_when_registration_complete))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun PasskeyStartScreenPreview() {
    AttestraAuthTheme {
        PasskeyStartScreen({}, {}, {})
    }
}
