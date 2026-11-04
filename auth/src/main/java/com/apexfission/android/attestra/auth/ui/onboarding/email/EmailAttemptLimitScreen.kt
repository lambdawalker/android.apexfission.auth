package com.apexfission.android.attestra.auth.ui.onboarding.email

import com.apexfission.android.attestra.auth.library.R
import androidx.compose.ui.res.stringResource

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.attestra.auth.ui.onboarding.common.BodyText
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

/**
 * Screen that shows an error when the user has exceeded the number of allowed code attempts.
 */
@Composable
fun EmailAttemptLimitScreen(
    email: String, onBack: () -> Unit, onRequestEmail: () -> Unit, onChangeEmail: () -> Unit,
    canRequestEmail: Boolean, retryAfter: String? = null,
) {
    OnboardingFrame(
        title = stringResource(R.string.auth_too_many_code_attempts), icon = Icons.Default.LockClock, step = "2/5", onBack = onBack,
        primaryLabel = stringResource(R.string.auth_request_a_new_email), primaryEnabled = canRequestEmail, onPrimary = onRequestEmail,
        secondaryLabel = stringResource(R.string.auth_change_email_address), onSecondary = onChangeEmail,
    ) {
        OnboardingCard {
            Text(stringResource(R.string.auth_this_code_can_no_longer_be_used), style = MaterialTheme.typography.titleSmall)
            BodyText(stringResource(R.string.auth_request_another_email_when_available_open_its_latest_link_and_use))
            if (email.isNotBlank()) Text(email, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!canRequestEmail) BodyText(retryAfter ?: stringResource(R.string.auth_please_try_again_later))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun EmailAttemptLimitScreenPreview() {
    AttestraAuthTheme {
        EmailAttemptLimitScreen("name@example.invalid", {}, {}, {}, true)
    }
}
