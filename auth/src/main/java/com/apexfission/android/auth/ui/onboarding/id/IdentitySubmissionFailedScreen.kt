package com.apexfission.android.auth.ui.onboarding.id

import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import com.apexfission.android.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

/**
 * Screen that handles the failure state when submitting the reviewed ID details.
 */
@Composable
fun IdentitySubmissionFailedScreen(onBack: () -> Unit, onRetry: () -> Unit, onDashboard: () -> Unit) {
    OnboardingFrame(
        title = stringResource(R.string.auth_could_not_submit_id_details), icon = Icons.Default.ErrorOutline, step = stringResource(R.string.auth_id_check), onBack = onBack,
        primaryLabel = stringResource(R.string.auth_try_again), onPrimary = onRetry,
        secondaryLabel = stringResource(R.string.auth_go_to_dashboard), onSecondary = onDashboard,
    ) {
        OnboardingCard {
            BodyText(stringResource(R.string.auth_we_could_not_confirm_that_your_details_were_submitted_check_your))
            BodyText(stringResource(R.string.auth_check_the_submission_status_before_sending_the_same_details_again))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun IdentitySubmissionFailedScreenPreview() {
    AttestraAuthTheme {
        IdentitySubmissionFailedScreen({}, {}, {})
    }
}
