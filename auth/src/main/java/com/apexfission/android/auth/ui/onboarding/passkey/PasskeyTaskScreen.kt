package com.apexfission.android.auth.ui.onboarding.passkey

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.apexfission.android.auth.library.R
import com.apexfission.android.auth.signin.PasskeyStage
import com.apexfission.android.auth.ui.onboarding.common.*

/** Native provider interaction remains explicit; progress and recovery stay on this surface. */
@Composable
internal fun PasskeyTaskScreen(
    stage: PasskeyStage,
    required: Boolean,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onRetryStatus: () -> Unit,
    onSignIn: () -> Unit,
    onLater: () -> Unit,
) {
    val message = when (stage) {
        PasskeyStage.READY -> null
        PasskeyStage.OPENING -> R.string.auth_task_opening_passkey
        PasskeyStage.SAVING -> R.string.auth_task_saving_passkey
        PasskeyStage.CHECKING -> R.string.auth_task_checking_passkey
        PasskeyStage.STATUS_FAILED -> R.string.auth_task_passkey_status_retry
        PasskeyStage.CANCELLED -> R.string.auth_task_setup_cancelled
        PasskeyStage.UNSUPPORTED -> R.string.auth_task_setup_unsupported
        PasskeyStage.FAILED -> R.string.auth_task_request_failed
        PasskeyStage.REAUTH_REQUIRED -> R.string.auth_sign_in_to_continue_setup
    }
    val primary = when (stage) {
        PasskeyStage.READY -> R.string.auth_create_passkey
        PasskeyStage.OPENING -> R.string.auth_task_opening_passkey
        PasskeyStage.SAVING -> R.string.auth_task_saving_passkey
        PasskeyStage.CHECKING -> R.string.auth_check_account_setup
        PasskeyStage.STATUS_FAILED -> R.string.auth_retry_account_status
        PasskeyStage.REAUTH_REQUIRED -> R.string.auth_sign_in
        else -> R.string.auth_try_again
    }
    OnboardingFrame(stringResource(R.string.auth_task_secure_account), Icons.Default.Key,
        stringResource(R.string.auth_task_account_security), onBack,
        primaryLabel = stringResource(primary), primaryEnabled = !stage.busy,
        onPrimary = { when (stage) {
            PasskeyStage.STATUS_FAILED -> onRetryStatus()
            PasskeyStage.REAUTH_REQUIRED -> onSignIn()
            else -> onCreate()
        } }, secondaryLabel = if (required) null else stringResource(R.string.auth_do_this_later),
        onSecondary = onLater, secondaryEnabled = !stage.busy) {
        OnboardingCard {
            BodyText(stringResource(R.string.auth_start_passkey_setup_and_follow_the_instructions_from_your_device))
            StatusRow(stringResource(R.string.auth_email_verified), stringResource(R.string.auth_your_email_was_confirmed))
            if (message != null) Text(stringResource(message),
                color = if (stage == PasskeyStage.FAILED || stage == PasskeyStage.REAUTH_REQUIRED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
    }
}
