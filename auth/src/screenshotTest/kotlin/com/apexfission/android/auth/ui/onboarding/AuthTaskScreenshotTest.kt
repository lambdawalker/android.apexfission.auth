package com.apexfission.android.auth.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.apexfission.android.auth.discovery.OnboardingStep
import com.apexfission.android.auth.passkey.AccountStatus
import com.apexfission.android.auth.signin.*
import com.apexfission.android.auth.ui.onboarding.account.AccountTaskScreen
import com.apexfission.android.auth.ui.onboarding.passkey.PasskeyTaskScreen
import com.apexfission.android.auth.ui.onboarding.signin.SignInTaskScreen
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

/** Synthetic, presentation-only states. No storage, credential provider or HTTP calls. */
class AuthTaskScreenshotTest {
    @Composable private fun signIn(state: SignInTaskState) {
        AttestraAuthTheme { SignInTaskScreen(state, 100_000, null, {}, {}, {}, {}, {}, {}, {}, {}) }
    }
    private val code get() = SignInTaskState(email = "person@example.invalid", code = "001234", stage = SignInStage.CODE)

    @PreviewTest @DocumentationPreview @Composable
    fun methodChoice() { signIn(SignInTaskState(email = "person@example.invalid")) }
    @PreviewTest @DocumentationPreview @Composable
    fun codeEntry() { signIn(code) }
    @PreviewTest @DocumentationPreview @Composable
    fun verifyingCode() { signIn(code.copy(operation = SignInOperation.VERIFY_CODE)) }
    @PreviewTest @DocumentationPreview @Composable
    fun connectionRecovery() { signIn(code.copy(problem = SignInProblem.CONNECTION)) }
    @PreviewTest @DocumentationPreview @Composable
    fun passwordRecovery() { signIn(SignInTaskState(email = "person@example.invalid", stage = SignInStage.PASSWORD,
        password = "SyntheticSample42!", problem = SignInProblem.CONNECTION)) }
    @PreviewTest @DocumentationPreview @Composable
    fun passkeyReady() { AttestraAuthTheme { PasskeyTaskScreen(PasskeyStage.READY, false, {}, {}, {}, {}, {}) } }
    @PreviewTest @DocumentationPreview @Composable
    fun passkeyCancelled() { AttestraAuthTheme { PasskeyTaskScreen(PasskeyStage.CANCELLED, false, {}, {}, {}, {}, {}) } }
    @PreviewTest @DocumentationPreview @Composable
    fun passkeyUnsupportedRequired() { AttestraAuthTheme { PasskeyTaskScreen(PasskeyStage.UNSUPPORTED, true, {}, {}, {}, {}, {}) } }
    @PreviewTest @DocumentationPreview @Composable
    fun passkeySavedStatusRetry() { AttestraAuthTheme { PasskeyTaskScreen(PasskeyStage.STATUS_FAILED, true, {}, {}, {}, {}, {}) } }
    @PreviewTest @DocumentationPreview @Composable
    fun completedAccount() { AttestraAuthTheme { AccountTaskScreen(AccountStatus(onboardingComplete = true),
        setOf(OnboardingStep.PASSKEY, OnboardingStep.PASSWORD, OnboardingStep.ID_CAPTURE), {}, {}, {}, {}) } }
    @PreviewTest @Preview(name = "Narrow large text", widthDp = 320, heightDp = 740, fontScale = 1.3f, locale = "en") @Composable
    fun narrowCodeRecovery() { signIn(code.copy(problem = SignInProblem.CONNECTION)) }
}
