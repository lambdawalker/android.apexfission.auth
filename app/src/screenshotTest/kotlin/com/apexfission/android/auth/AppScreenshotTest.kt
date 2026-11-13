package com.apexfission.android.auth

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

@Preview(name = "Light", widthDp = 393, heightDp = 852, uiMode = Configuration.UI_MODE_NIGHT_NO, locale = "en")
@Preview(name = "Dark", widthDp = 393, heightDp = 852, uiMode = Configuration.UI_MODE_NIGHT_YES, locale = "en")
annotation class AppDocumentationPreview

class AppScreenshotTest {
    @PreviewTest @AppDocumentationPreview @Composable
    fun menu() { AttestraAuthTheme { OnboardingEntryScreen(true, false, null, {}, {}, {}, {}) } }
    @PreviewTest @AppDocumentationPreview @Composable
    fun resetting() { AttestraAuthTheme { OnboardingEntryScreen(false, true, "Clearing local data", {}, {}, {}, {}) } }
    @PreviewTest @AppDocumentationPreview @Composable
    fun menuWithoutBackend() { AttestraAuthTheme { OnboardingEntryScreen(false, false, null, {}, {}, {}, {}) } }
    @PreviewTest
    @Preview(name = "NarrowLight", widthDp = 320, heightDp = 852, fontScale = 1.6f, uiMode = Configuration.UI_MODE_NIGHT_NO)
    @Preview(name = "NarrowDark", widthDp = 320, heightDp = 852, fontScale = 1.6f, uiMode = Configuration.UI_MODE_NIGHT_YES)
    @Composable
    fun menuLargeText() { AttestraAuthTheme { OnboardingEntryScreen(true, false, null, {}, {}, {}, {}) } }
    @PreviewTest @AppDocumentationPreview @Composable
    fun localCaptureComplete() { AttestraAuthTheme { LocalCardCaptureResult(true, {}, {}) } }
    @PreviewTest @AppDocumentationPreview @Composable
    fun localCaptureFailure() { AttestraAuthTheme { LocalCardCaptureResult(false, {}, {}) } }
    @PreviewTest @AppDocumentationPreview @Composable
    fun configurationLoading() { AttestraAuthTheme { EnvironmentStatusScreen(true, null, {}, {}) } }
    @PreviewTest @AppDocumentationPreview @Composable
    fun configurationFailure() { AttestraAuthTheme { EnvironmentStatusScreen(false, "Could not load environment settings. Retry when connected.", {}, {}) } }
    @PreviewTest @AppDocumentationPreview @Composable
    fun signInRequired() { AttestraAuthTheme { CaptureSignInRequired({}) } }
}
