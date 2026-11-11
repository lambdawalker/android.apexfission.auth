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
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

/**
 * Screen that confirms the user's card details have been securely saved.
 */
@Composable
fun CardDetailsSavedScreen(onDone: () -> Unit, onReview: () -> Unit) {
    OnboardingFrame(
        title = stringResource(R.string.auth_details_saved), icon = Icons.Default.TaskAlt, step = stringResource(R.string.auth_card_details), onBack = onDone,
        primaryLabel = stringResource(R.string.auth_done), onPrimary = onDone,
        secondaryLabel = stringResource(R.string.auth_review_details), onSecondary = onReview,
    ) {
        OnboardingCard {
            BodyText(stringResource(R.string.auth_your_card_details_have_been_saved_you_can_now_continue_to_your_ac))
            BodyText(stringResource(R.string.auth_saving_these_details_does_not_verify_your_identity))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun CardDetailsSavedScreenPreview() {
    AttestraAuthTheme { CardDetailsSavedScreen({}, {}) }
}
