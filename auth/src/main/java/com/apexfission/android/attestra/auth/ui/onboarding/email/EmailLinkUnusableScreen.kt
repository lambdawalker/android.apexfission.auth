package com.apexfission.android.attestra.auth.ui.onboarding.email

import com.apexfission.android.attestra.auth.library.R
import androidx.compose.ui.res.stringResource

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.attestra.auth.ui.onboarding.common.BodyText
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

/**
 * Screen that shows an error when the provided email confirmation link is no longer valid.
 */
@Composable
fun EmailLinkUnusableScreen(email: String, onBack: () -> Unit, onRequestEmail: () -> Unit, onChangeEmail: () -> Unit, canRequestEmail: Boolean = true) {
    OnboardingFrame(
        title = stringResource(R.string.auth_this_confirmation_link_can_t_be_used), icon = Icons.Default.LinkOff, step = "2/5", onBack = onBack,
        primaryLabel = stringResource(R.string.auth_request_a_new_email), onPrimary = onRequestEmail, primaryEnabled = canRequestEmail,
        secondaryLabel = stringResource(R.string.auth_change_email_address), onSecondary = onChangeEmail,
    ) {
        OnboardingCard {
            BodyText(stringResource(R.string.auth_this_link_may_have_expired_or_been_replaced_request_another_email))
            if (email.isNotBlank()) Text(email, color = MaterialTheme.colorScheme.onSurfaceVariant)
            BodyText(stringResource(R.string.auth_the_old_link_and_code_cannot_be_reused))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun EmailLinkUnusableScreenPreview() {
    AttestraAuthTheme {
        EmailLinkUnusableScreen("name@example.invalid", {}, {}, {})
    }
}
