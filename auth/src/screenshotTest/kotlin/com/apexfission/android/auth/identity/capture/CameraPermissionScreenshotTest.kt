package com.apexfission.android.auth.identity.capture

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import com.apexfission.android.auth.ui.onboarding.DocumentationPreview
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme

/** Only the production visual: no permission launcher, hardware, camera or account. */
class CameraPermissionScreenshotTest {
    @PreviewTest @DocumentationPreview @Composable
    fun retry() { AttestraAuthTheme { CameraPermissionRecovery(true, {}, {}, {}) } }

    @PreviewTest @DocumentationPreview @Composable
    fun settings() { AttestraAuthTheme { CameraPermissionRecovery(false, {}, {}, {}) } }
}
