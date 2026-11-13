package com.apexfission.android.auth

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OnboardingEntryScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun catalogAndOnboardingRemainReachableWithoutBackend() {
        var starts = 0
        var catalogs = 0
        compose.setContent {
            AttestraAuthTheme {
                OnboardingEntryScreen(false, false, null, {}, { starts++ }, { catalogs++ })
            }
        }
        compose.onNodeWithText("Open onboarding").performClick()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Open UI catalog"))
        compose.onNodeWithText("Open UI catalog").performClick()
        compose.runOnIdle {
            assertEquals(1, starts)
            assertEquals(1, catalogs)
        }
    }

    @Test
    fun localDeletionRequiresConfirmationAndCanBeCancelled() {
        var deletions = 0
        compose.setContent {
            AttestraAuthTheme {
                OnboardingEntryScreen(true, false, null, { deletions++ }, {}, {})
            }
        }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Delete local user"))
        compose.onNodeWithText("Delete local user").performClick()
        compose.onNodeWithText("Delete local user?").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, deletions) }
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(0, deletions) }
        compose.onNodeWithText("Delete local user").performClick()
        // Select the confirmation rather than the list action behind the dialog.
        compose.onNode(hasText("Delete local user") and hasAnyAncestor(isDialog())).performClick()
        compose.runOnIdle { assertEquals(1, deletions) }
    }

    @Test
    fun resetDisablesNavigationAndDeletionAndShowsStatus() {
        compose.setContent {
            AttestraAuthTheme {
                OnboardingEntryScreen(false, true, "Clearing local data", {}, {}, {})
            }
        }
        compose.onNodeWithText("Open onboarding").assertIsNotEnabled()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Open UI catalog"))
        compose.onNodeWithText("Open UI catalog").assertIsNotEnabled()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Deleting local data…"))
        compose.onNodeWithText("Deleting local data…").assertIsNotEnabled()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Clearing local data"))
        compose.onNodeWithText("Clearing local data").assertIsDisplayed()
    }
}
