package com.apexfission.android.auth.ui.onboarding.email

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.library.R
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import com.apexfission.android.auth.ui.onboarding.common.EmailOneTimeCodeField
import com.apexfission.android.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.onboarding.common.authText
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

/**
 * One verification task: the email link confirms automatically when possible,
 * while direct code entry is available without navigating to another screen.
 */
@Composable
fun EmailWaitScreen(
    email: String,
    code: String,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    onBack: () -> Unit,
    onResend: () -> Unit,
    onChangeEmail: () -> Unit,
    canResend: Boolean = true,
    resendHelp: String? = null,
    error: String? = null,
    attemptsRemaining: Int? = null,
    busyMessage: String? = null,
    onSignIn: (() -> Unit)? = null,
) {
    val busy = busyMessage != null
    OnboardingFrame(
        title = stringResource(R.string.auth_check_your_email),
        icon = Icons.Default.Mail,
        step = "",
        onBack = onBack,
        primaryLabel = stringResource(R.string.auth_verify_email),
        onPrimary = onVerify,
        primaryEnabled = !busy && code.length == 6,
        secondaryLabel = stringResource(R.string.auth_resend_email),
        onSecondary = onResend,
        secondaryEnabled = !busy && canResend,
    ) {
        OnboardingCard {
            BodyText(stringResource(R.string.auth_open_the_link_or_enter_the_code_from_the_latest_email_for))
            if (email.isNotBlank()) {
                Text(email, style = MaterialTheme.typography.titleSmall)
            }
            Text(
                stringResource(R.string.auth_confirmation_code),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            EmailOneTimeCodeField(
                value = code,
                onValueChange = onCodeChange,
                contentDescriptionText = stringResource(R.string.auth_six_digit_confirmation_code),
                enabled = !busy,
            )
            if (busyMessage != null) {
                BodyText(busyMessage)
            }
            if (error != null) {
                Text(
                    authText(error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (attemptsRemaining != null) {
                Text(
                    stringResource(R.string.auth_attempts_remaining_1_s, attemptsRemaining),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (resendHelp != null) BodyText(resendHelp)
            TextButton(onClick = onChangeEmail, enabled = !busy) {
                Text(stringResource(R.string.auth_change_email_address))
            }
            if (onSignIn != null) {
                TextButton(onClick = onSignIn, enabled = !busy) {
                    Text(stringResource(R.string.auth_already_verified_sign_in))
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun EmailWaitScreenPreview() {
    AttestraAuthTheme {
        EmailWaitScreen("name@example.invalid", "", {}, {}, {}, {}, {})
    }
}
