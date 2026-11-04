package com.apexfission.android.attestra.auth.ui.onboarding.id

import com.apexfission.android.attestra.auth.library.R
import androidx.compose.ui.res.stringResource

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.attestra.auth.ui.onboarding.common.BodyText
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

/**
 * Screen that shows the result of an unsuccessful identity check and offers retry options.
 */
@Composable
fun IdentityUnsuccessfulScreen(
    onBack: () -> Unit, onDashboard: () -> Unit, onRetry: () -> Unit,
    canRetry: Boolean, explanation: String? = null,
) {
    OnboardingFrame(
        title = stringResource(R.string.auth_we_couldn_t_complete_your_identity_check), icon = Icons.Default.WarningAmber,
        step = stringResource(R.string.auth_id_check), onBack = onBack,
        primaryLabel = if (canRetry) stringResource(R.string.auth_try_again) else stringResource(R.string.auth_go_to_dashboard),
        onPrimary = if (canRetry) onRetry else onDashboard,
        secondaryLabel = if (canRetry) stringResource(R.string.auth_go_to_dashboard) else null,
        onSecondary = onDashboard,
    ) {
        OnboardingCard {
            BodyText(explanation ?: stringResource(R.string.auth_your_identity_check_could_not_be_completed_you_can_return_to_your))
            BodyText(stringResource(R.string.auth_your_account_remains_available_try_again_only_if_another_attempt))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun IdentityUnsuccessfulScreenPreview() {
    AttestraAuthTheme {
        IdentityUnsuccessfulScreen({}, {}, {}, true)
    }
}
