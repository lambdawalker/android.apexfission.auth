package com.apexfission.android.attestra.auth.ui.onboarding.email

import com.apexfission.android.attestra.auth.ui.onboarding.common.authText
import com.apexfission.android.attestra.auth.library.R
import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apexfission.android.attestra.auth.ui.onboarding.common.BodyText
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.attestra.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme

/**
 * Screen that prompts the user to enter the six-digit code sent to their email.
 */
@Composable
fun EmailCodeScreen(
    email: String,
    onBack: () -> Unit,
    onVerify: (String) -> Unit,
    onResend: () -> Unit,
    error: String? = null,
    attemptsRemaining: Int? = null,
    canResend: Boolean = true,
) {
    var code by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val codeDescription = stringResource(R.string.auth_six_digit_confirmation_code)
    OnboardingFrame(
        title = stringResource(R.string.auth_complete_email_verification), icon = Icons.Default.Mail, step = "2/5",
        onBack = onBack, primaryLabel = if (error == null) stringResource(R.string.auth_verify_email) else stringResource(R.string.auth_try_again),
        onPrimary = { onVerify(code) }, primaryEnabled = code.length == 6,
        secondaryLabel = stringResource(R.string.auth_resend_email), onSecondary = onResend, secondaryEnabled = canResend,
    ) {
        OnboardingCard {
            BodyText(stringResource(R.string.auth_enter_the_six_digit_code_from_the_email_you_just_opened_to_confir))
            Text(stringResource(R.string.auth_confirmation_code), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            BasicTextField(
                value = code,
                onValueChange = { code = it.filter(Char::isDigit).take(6) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
                    .semantics { contentDescription = codeDescription },
                decorationBox = { innerTextField ->
                    Box {
                        Row(
                            Modifier.fillMaxWidth().clickable { focus.requestFocus() },
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            repeat(6) { index ->
                                Surface(
                                    modifier = Modifier.weight(1f).heightIn(min = 50.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = MaterialTheme.shapes.extraSmall,
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            code.getOrNull(index)?.toString() ?: "",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.titleLarge,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }
                        }
                        Box(Modifier.size(1.dp).alpha(0.01f)) { innerTextField() }
                    }
                },
            )
            if (error != null) Text(authText(error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            if (attemptsRemaining != null) {
                Text(stringResource(R.string.auth_attempts_remaining_1_s, attemptsRemaining), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (email.isNotBlank()) Text(email, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            BodyText(stringResource(R.string.auth_the_code_is_separate_from_the_link_entering_it_does_not_submit_au))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun EmailCodeScreenPreview() {
    AttestraAuthTheme {
        EmailCodeScreen("name@example.invalid", {}, {}, {})
    }
}
