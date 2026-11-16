package com.apexfission.android.auth.ui.onboarding.account

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.apexfission.android.auth.discovery.OnboardingStep
import com.apexfission.android.auth.library.R
import com.apexfission.android.auth.passkey.AccountStatus
import com.apexfission.android.auth.ui.onboarding.common.*

/** Server-confirmed account destination. Optional setup is an action, never another gate. */
@Composable
internal fun AccountTaskScreen(
    status: AccountStatus,
    optionalSteps: Set<OnboardingStep>,
    onContinue: () -> Unit,
    onAdd: (OnboardingStep) -> Unit,
    onReviewId: () -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit = onContinue,
) {
    OnboardingFrame(stringResource(R.string.auth_your_account), Icons.Default.Lock, "", onBack,
        primaryLabel = stringResource(R.string.auth_continue), onPrimary = onContinue) {
        OnboardingCard {
            BodyText(stringResource(R.string.auth_your_account_setup_is_complete))
            StatusRow(stringResource(R.string.auth_email_verified), stringResource(R.string.auth_your_email_was_confirmed))
        }
        Text(stringResource(R.string.auth_task_account_security), style = MaterialTheme.typography.titleMedium)
        OnboardingCard {
            if (status.passkeyRegistered) StatusRow(stringResource(R.string.auth_passkey_added), stringResource(R.string.auth_passkey_registration_completed))
            if (status.passwordRegistered) BodyText(stringResource(R.string.auth_task_password_added))
            for (step in listOf(OnboardingStep.PASSKEY, OnboardingStep.PASSWORD, OnboardingStep.ID_CAPTURE)) {
                if (step in optionalSteps) {
                    OutlinedButton(onClick = { onAdd(step) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(when (step) {
                            OnboardingStep.PASSKEY -> R.string.auth_create_passkey
                            OnboardingStep.PASSWORD -> R.string.auth_set_your_password
                            else -> R.string.auth_capture_your_id
                        }))
                    }
                }
            }
            if (status.idCaptured) {
                BodyText(stringResource(R.string.auth_document_captured_this_does_not_assert_identity_verification))
                TextButton(onClick = onReviewId) { Text(stringResource(R.string.auth_review_id_details)) }
            }
            TextButton(onClick = onRefresh) { Text(stringResource(R.string.auth_refresh_account_status)) }
        }
    }
}
