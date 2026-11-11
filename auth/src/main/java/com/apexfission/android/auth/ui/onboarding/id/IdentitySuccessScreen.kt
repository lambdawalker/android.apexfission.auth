package com.apexfission.android.auth.ui.onboarding.id

import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import com.apexfission.android.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.onboarding.common.StatusRow
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

/**
 * Screen that confirms the identity check was successful and the onboarding process is complete.
 */
@Composable
fun IdentitySuccessScreen(onBack: () -> Unit, onDashboard: () -> Unit, passkeyAdded: Boolean) {
    OnboardingFrame(
        title = stringResource(R.string.auth_identity_check_complete), icon = Icons.Default.TaskAlt, step = stringResource(R.string.auth_id_check), onBack = onBack,
        primaryLabel = stringResource(R.string.auth_go_to_dashboard), onPrimary = onDashboard,
    ) {
        OnboardingCard {
            BodyText(stringResource(R.string.auth_your_identity_check_is_complete_you_can_now_continue_to_your_acco))
            StatusRow(stringResource(R.string.auth_email_verified), stringResource(R.string.auth_confirmed_on_this_account))
            if (passkeyAdded) StatusRow(stringResource(R.string.auth_passkey_added), stringResource(R.string.auth_added_to_your_account))
            StatusRow(stringResource(R.string.auth_identity_check_complete), stringResource(R.string.auth_approved_result_received_from_the_identity_service))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun IdentitySuccessScreenPreview() {
    AttestraAuthTheme {
        IdentitySuccessScreen({}, {}, true)
    }
}
