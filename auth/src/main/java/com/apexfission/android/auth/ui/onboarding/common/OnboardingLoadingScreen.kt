package com.apexfission.android.auth.ui.onboarding.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

enum class LoadingTask(val title: String, val description: String, val step: String) {
    REQUEST_EMAIL("Sending your confirmation email", "We are processing your request.", "1/5"),
    CONFIRM_EMAIL("Verifying your email", "We are confirming your email and preparing your session.", "2/5"),
    RESEND_EMAIL("Requesting another email", "We are processing your request.", "2/5"),
    SIGN_IN("Signing you in", "We are checking your account and preparing your session.", "Account"),
    OPEN_PASSKEY_MANAGER("Opening your passkey manager", "Follow your device or password manager to create a passkey.", "3/5"),
    SAVE_PASSKEY("Saving your passkey", "We are adding the passkey to your account.", "3/5"),
    PREPARE_CAPTURE("Preparing your camera", "We are checking your saved capture.", "ID capture"),
    UPLOAD_DOCUMENT("Saving your photos", "We are uploading both sides of your card.", "ID capture"),
    READ_DOCUMENT("Reading your document", "We are extracting details for you to review. You can leave this screen and return to check progress.", "ID check"),
    SUBMIT_ID("Submitting your ID details", "We are sending your reviewed details for an identity check.", "ID check"),
    CHECK_IDENTITY("Checking your identity", "Your details have been submitted. We will show the result when available.", "ID check"),
}

/**
 * Screen that displays a loading state for long-running onboarding tasks.
 */
@Composable
fun OnboardingLoadingScreen(task: LoadingTask, onBack: () -> Unit, onLeave: (() -> Unit)? = null) {
    OnboardingFrame(
        title = task.title, icon = Icons.Default.Autorenew, step = task.step, onBack = onBack,
        secondaryLabel = if (task == LoadingTask.CHECK_IDENTITY) stringResource(R.string.auth_go_to_dashboard) else null,
        onSecondary = { onLeave?.invoke() },
    ) {
        OnboardingCard {
            WaitingIndicator(task.description)
            BodyText(stringResource(R.string.auth_this_may_take_a_moment_we_will_update_the_screen_when_it_finishes))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun OnboardingLoadingScreenPreview() {
    AttestraAuthTheme {
        OnboardingLoadingScreen(LoadingTask.CONFIRM_EMAIL, {})
    }
}
