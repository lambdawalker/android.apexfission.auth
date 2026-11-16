package com.apexfission.android.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import com.apexfission.android.auth.ui.onboarding.common.OnboardingCard
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.onboarding.common.StatusRow
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

/**
 * Demo-only destination shown after the library reports successful onboarding completion.
 *
 * This page celebrates completion of configured onboarding requirements. It does not claim that
 * identity validation or any later assurance step has succeeded.
 */
@Composable
internal fun OnboardingCongratulationsScreen(onDone: () -> Unit) {
    OnboardingFrame(
        title = "Congratulations",
        icon = Icons.Default.CheckCircle,
        step = "Setup complete",
        onBack = onDone,
        primaryLabel = "Back to demo menu",
        onPrimary = onDone,
    ) {
        OnboardingCard {
            StatusRow(
                label = "Onboarding complete",
                detail = "The server confirmed all required setup steps.",
            )
            BodyText("You are ready to continue from this demo.")
            BodyText("This confirms onboarding completion only; it does not assert identity verification.")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingCongratulationsScreenPreview() {
    AttestraAuthTheme {
        OnboardingCongratulationsScreen(onDone = {})
    }
}
