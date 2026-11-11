package com.apexfission.android.auth.ui.onboarding.passkey

import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import com.apexfission.android.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

/**
 * Screen that handles the failure or cancellation of a passkey creation attempt.
 */
@Composable
fun PasskeyFailedScreen(onBack: () -> Unit, onRetry: () -> Unit, onDoLater: () -> Unit, cancelled: Boolean, required: Boolean = false) {
    OnboardingFrame(
        title = stringResource(R.string.auth_passkey_setup_incomplete), icon = Icons.Default.SyncProblem,
        step = "3/5", onBack = onBack,
        primaryLabel = stringResource(R.string.auth_try_again), onPrimary = onRetry,
        secondaryLabel = if (required) null else stringResource(R.string.auth_do_this_later), onSecondary = onDoLater,
    ) {
        OnboardingCard {
            BodyText(if (cancelled) stringResource(R.string.auth_passkey_setup_wasn_t_finished) else stringResource(R.string.auth_we_couldn_t_add_your_passkey_please_try_again))
            BodyText(if (required) stringResource(R.string.auth_a_passkey_is_required_to_finish_setup_your_email_remains_confirme) else stringResource(R.string.auth_you_can_add_a_passkey_later_your_email_remains_confirmed))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun PasskeyFailedScreenPreview() {
    AttestraAuthTheme {
        PasskeyFailedScreen({}, {}, {}, false)
    }
}
