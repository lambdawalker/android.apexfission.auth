package com.apexfission.android.auth.identity.capture

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme
import com.apexfission.android.permission.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CameraPermissionRecoveryUiTest {
    @get:Rule val compose = createComposeRule()
    private fun text(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    @Test fun denialRequiresAnExplicitRetryInsteadOfRequestingDuringComposition() {
        var requests = 0
        var settings = 0
        compose.setContent {
            AttestraAuthTheme { CameraPermissionRecovery(true, { requests++ }, { settings++ }, {}) }
        }
        compose.runOnIdle { assertEquals(0, requests); assertEquals(0, settings) }
        compose.onNodeWithText(text(R.string.permission_bundle_retry)).performClick()
        compose.runOnIdle { assertEquals(1, requests); assertEquals(0, settings) }
    }

    @Test fun blockedPermissionOpensSettingsOnlyAfterUserChoosesIt() {
        var requests = 0
        var settings = 0
        compose.setContent {
            AttestraAuthTheme { CameraPermissionRecovery(false, { requests++ }, { settings++ }, {}) }
        }
        compose.runOnIdle { assertEquals(0, settings) }
        compose.onNodeWithText(text(R.string.permission_open_settings)).performClick()
        compose.runOnIdle { assertEquals(0, requests); assertEquals(1, settings) }
    }
}
