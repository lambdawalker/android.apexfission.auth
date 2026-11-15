package com.apexfission.android.auth.ui.onboarding.signin

import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.apexfission.android.auth.library.R
import com.apexfission.android.auth.signin.*
import com.apexfission.android.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.onboarding.password.PasswordField
import com.apexfission.android.ui.common.CodeVerificationCard

/** One sign-in task; changing request state never replaces this form with a loading page. */
@Composable
internal fun SignInTaskScreen(
    state: SignInTaskState,
    now: Long,
    notice: String?,
    onEmail: (String) -> Unit,
    onCode: (String) -> Unit,
    onPassword: (String) -> Unit,
    onSubmit: () -> Unit,
    onSendCode: () -> Unit,
    onChoosePassword: () -> Unit,
    onChooseMethod: () -> Unit,
    onBack: () -> Unit,
) {
    val autofill = LocalAutofillManager.current
    val leave = { autofill?.cancel(); onBack() }
    BackHandler(onBack = leave)
    val validEmail = Patterns.EMAIL_ADDRESS.matcher(state.email.trim()).matches()
    val progress = when (state.operation) {
        SignInOperation.SEND_CODE -> stringResource(R.string.auth_task_sending_code)
        SignInOperation.VERIFY_CODE -> stringResource(R.string.auth_task_verifying_code)
        SignInOperation.PASSWORD -> stringResource(R.string.auth_task_signing_in)
        SignInOperation.PASSKEY -> stringResource(R.string.auth_task_opening_passkey)
        null -> null
    }
    val feedback = when (state.problem) {
        SignInProblem.CONNECTION -> stringResource(R.string.auth_task_connection)
        SignInProblem.TEMPORARY -> stringResource(R.string.auth_task_service_unavailable)
        SignInProblem.REJECTED -> stringResource(if (state.stage == SignInStage.CODE) R.string.auth_task_invalid_code else R.string.auth_task_request_failed)
        SignInProblem.THROTTLED -> stringResource(R.string.auth_task_throttled)
        SignInProblem.CANCELLED -> stringResource(R.string.auth_task_signin_cancelled)
        SignInProblem.UNSUPPORTED -> stringResource(R.string.auth_task_signin_unsupported)
        SignInProblem.UNKNOWN, SignInProblem.SESSION_EXPIRED -> stringResource(R.string.auth_task_request_failed)
        null -> null
    }
    val primary = progress ?: stringResource(when (state.stage) {
        SignInStage.METHODS -> R.string.auth_sign_in_with_passkey
        SignInStage.CODE -> if (state.problem == SignInProblem.CONNECTION || state.problem == SignInProblem.TEMPORARY) R.string.auth_try_again else R.string.auth_verify_code
        SignInStage.PASSWORD -> R.string.auth_sign_in
    })
    val valid = validEmail && when (state.stage) {
        SignInStage.METHODS -> true
        SignInStage.CODE -> state.code.length == 6
        SignInStage.PASSWORD -> state.password.isNotEmpty()
    }
    OnboardingFrame(stringResource(R.string.auth_sign_in), Icons.Default.Lock, stringResource(R.string.auth_account), leave,
        primaryLabel = primary, primaryEnabled = valid && state.canSubmit(now),
        onPrimary = { autofill?.cancel(); onSubmit() }) {
        if (notice != null) Text(notice, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        if (state.stage != SignInStage.METHODS) {
            // Give the full recipient its own line; the action must not squeeze it on
            // narrow screens or when accessibility text scaling is enabled.
            Column(Modifier.fillMaxWidth()) {
                Text(state.email, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium)
                TextButton(enabled = !state.busy, modifier = Modifier.align(Alignment.End),
                    onClick = { autofill?.cancel(); onChooseMethod() }) {
                    Text(stringResource(R.string.auth_change_email_address))
                }
            }
        }
        if (state.stage == SignInStage.CODE) {
            CodeVerificationCard(
                value = state.code, onValueChange = onCode,
                codeLabel = stringResource(R.string.auth_confirmation_code),
                codeContentDescription = stringResource(R.string.auth_six_digit_code),
                pasteLabel = stringResource(R.string.auth_paste_code),
                instructions = stringResource(R.string.auth_task_enter_code),
                enabled = !state.busy, autoFocus = true,
                autofillContentType = ContentType("emailOTPCode"),
                busyMessage = progress, errorMessage = feedback,
            )
        } else {
            OnboardingCard {
                if (state.stage == SignInStage.METHODS) {
                    OutlinedTextField(state.email, onEmail, enabled = !state.busy,
                        label = { Text(stringResource(R.string.auth_email_address)) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
                } else PasswordField(state.password, onPassword, enabled = !state.busy)
                if (progress != null) Text(progress, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                if (feedback != null) Text(feedback, color = if (state.problem == SignInProblem.CANCELLED) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
        }
        val retryAt = if (state.stage == SignInStage.PASSWORD) state.blockedUntil else maxOf(state.resendAt, state.blockedUntil)
        if (now < retryAt) Text(stringResource(R.string.auth_task_retry_in, ((retryAt - now + 999L) / 1_000L).toInt()),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.stage == SignInStage.METHODS || state.stage == SignInStage.CODE) {
            OutlinedButton(onClick = onSendCode, enabled = validEmail && state.canSendCode(now), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (state.stage == SignInStage.CODE) R.string.auth_resend_email else R.string.auth_send_an_email_code))
            }
        }
        TextButton(onClick = { autofill?.cancel(); if (state.stage == SignInStage.METHODS) onChoosePassword() else onChooseMethod() },
            enabled = !state.busy && validEmail, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (state.stage == SignInStage.METHODS) R.string.auth_sign_in_with_password else R.string.auth_task_another_method))
        }
    }
}
