package com.apexfission.android.auth.ui.onboarding.id

import com.apexfission.android.auth.ui.onboarding.common.authText
import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalConfiguration
import com.apexfission.android.auth.capture.CapturePolicy
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import com.apexfission.android.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.onboarding.common.StatusRow
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

/**
 * Screen that introduces the identity capture process and displays the privacy policy before starting.
 */
@Composable
fun IdentityStartScreen(
    onBack: () -> Unit, onStartCapture: () -> Unit, onSkip: () -> Unit,
    passkeyAdded: Boolean,
    showAccountStatus: Boolean = true,
    required: Boolean = false,
    capturePolicy: CapturePolicy? = null,
    policyLoading: Boolean = false,
    policyError: String? = null,
    requireDisclosure: Boolean = false,
    onRetryPolicy: () -> Unit = {},
) {
    val language = LocalConfiguration.current.locales[0].language
    val canContinue = !requireDisclosure || (!policyLoading && policyError == null && capturePolicy?.enabled == true)

    OnboardingFrame(
        title = stringResource(R.string.auth_capture_your_id), icon = Icons.Default.Badge, step = if (required) stringResource(R.string.auth_required_id_capture) else stringResource(R.string.auth_optional_id_capture), onBack = onBack,
        primaryLabel = stringResource(R.string.auth_continue_to_capture), onPrimary = onStartCapture, primaryEnabled = canContinue,
        secondaryLabel = if (required) null else stringResource(R.string.auth_skip_for_now), onSecondary = onSkip,
    ) {
        OnboardingCard {
            BodyText(if (required) stringResource(R.string.auth_save_document_photos_to_finish_setup_capturing_a_document_does_no) else stringResource(R.string.auth_you_can_save_document_photos_now_or_return_later_capturing_a_docu))
            if (showAccountStatus) {
            StatusRow(stringResource(R.string.auth_email_verified), stringResource(R.string.auth_your_email_was_confirmed))
            StatusRow(
                if (passkeyAdded) stringResource(R.string.auth_passkey_added) else stringResource(R.string.auth_passkey_not_added),
                if (passkeyAdded) stringResource(R.string.auth_passkey_registration_completed) else stringResource(R.string.auth_you_can_add_a_passkey_later),
                complete = passkeyAdded,
            )
            }
            if (requireDisclosure) {
                when {
                    policyLoading || (capturePolicy == null && policyError == null) -> BodyText(stringResource(R.string.auth_loading_capture_information))
                    policyError != null -> {
                        Text(authText(policyError), color = MaterialTheme.colorScheme.error)
                        TextButton(shape = MaterialTheme.shapes.small, onClick = onRetryPolicy) { Text(stringResource(R.string.auth_retry)) }
                    }
                    capturePolicy != null -> {
                        Text(capturePolicy.purposes.text(language).ifBlank { capturePolicy.purpose },
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        BodyText(stringResource(R.string.auth_uploaded_photos_are_retained_for_up_to_1_s_days_after_capture_com, capturePolicy.retentionDays))
                        if (!capturePolicy.enabled) BodyText(stringResource(R.string.auth_document_capture_is_not_enabled_for_this_service_yet))
                    }
                }
            }
            BodyText(stringResource(R.string.auth_use_a_two_sided_card_we_will_ask_for_camera_access_capture_each_s))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun IdentityStartScreenPreview() {
    AttestraAuthTheme {
        IdentityStartScreen({}, {}, {}, false)
    }
}
