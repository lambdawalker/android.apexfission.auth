package com.apexfission.android.auth.capture

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.apexfission.android.auth.library.R
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DocumentReviewUiTest {
    @get:Rule val compose = createComposeRule()
    private fun text(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    @Test fun dismissingLocationSheetDoesNotSaveItsDraft() {
        var saved: IssuingJurisdiction? = null
        val original = IssuingJurisdiction("Mexico", "Chihuahua", "Ciudad Juárez")
        compose.setContent { MaterialTheme { IssuingJurisdictionField(original, onChange = { saved = it }) } }
        compose.onNodeWithText(original.display()).performClick()
        compose.onNode(hasSetTextAction() and hasText(text(R.string.auth_issuing_country))).performTextReplacement("Canada")
        compose.onNodeWithText(text(R.string.auth_close_location)).performScrollTo().performClick()
        compose.runOnIdle { assertNull(saved) }
        compose.onNodeWithText(original.display()).assertExists()
    }

    @Test fun applyingChangedCountryClearsDependentValuesTogether() {
        var saved: IssuingJurisdiction? = null
        compose.setContent { MaterialTheme { IssuingJurisdictionEditor(IssuingJurisdiction("Mexico", "Chihuahua", "Ciudad Juárez"), true, { saved = it }, {}) } }
        compose.onNode(hasSetTextAction() and hasText(text(R.string.auth_issuing_country))).performTextReplacement("Canada")
        compose.runOnIdle { assertNull(saved) }
        compose.onNodeWithText(text(R.string.auth_use_location)).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(IssuingJurisdiction("Canada"), saved) }
    }
    @Test fun reopeningSavedDetailsShowsEditableReviewInsteadOfDoneScreen() {
        val card = CardData(fullName = "Sample Person")
        val record = CaptureRecord(
            id = "a".repeat(32),
            evidenceVersion = 1,
            revision = 1,
            policyVersion = "v1",
            documentType = "id_card",
            state = "ready",
            expiresAt = 9999999999,
            extraction = CardExtraction("complete"),
            reviewed = ReviewedCard(card, 123, "MX"),
        )
        compose.setContent {
            MaterialTheme {
                CaptureHostContent(
                    ui = CaptureUi(record = record, draft = card, originCountry = "MX"),
                    onExit = {},
                    reviewSavedDetails = true,
                )
            }
        }
        compose.onNodeWithText(text(R.string.auth_save_and_continue)).assertExists()
        compose.onNodeWithText(text(R.string.auth_done)).assertDoesNotExist()
    }

    @Test fun lateOcrCannotReplaceUnappliedSheetEdits() {
        val value = androidx.compose.runtime.mutableStateOf(IssuingJurisdiction("Mexico"))
        var saved: IssuingJurisdiction? = null
        compose.setContent { MaterialTheme { IssuingJurisdictionEditor(value.value, true, { saved = it }, {}) } }
        compose.onNode(hasSetTextAction() and hasText(text(R.string.auth_issuing_country))).performTextReplacement("Canada")
        compose.runOnIdle { value.value = IssuingJurisdiction("Mexico", "Chihuahua", "Ciudad Juárez") }
        compose.onNodeWithText(text(R.string.auth_use_location)).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(IssuingJurisdiction("Canada"), saved) }
    }

}
