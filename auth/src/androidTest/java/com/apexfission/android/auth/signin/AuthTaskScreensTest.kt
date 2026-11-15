package com.apexfission.android.auth.signin

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.apexfission.android.auth.library.R
import com.apexfission.android.auth.ui.onboarding.signin.SignInTaskScreen
import com.apexfission.android.auth.ui.onboarding.passkey.PasskeyTaskScreen
import com.apexfission.android.auth.ui.onboarding.email.EmailStartScreen
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AuthTaskScreensTest {
    @get:Rule val compose = createComposeRule()
    private fun text(id: Int) = ApplicationProvider.getApplicationContext<Context>().getString(id)

    @Test fun codeRemainsVisibleDuringVerificationAndConnectionRecovery() {
        val state = mutableStateOf(SignInTaskState(email = "person@example.test", code = "001234", stage = SignInStage.CODE))
        var submitted = 0
        compose.setContent { AttestraAuthTheme {
            SignInTaskScreen(state.value, 100_000, null, {}, {}, {}, { submitted++ }, {}, {}, {}, {})
        } }
        compose.onNode(hasSetTextAction()).assertTextContains("001234")
        compose.runOnIdle { state.value = state.value.copy(operation = SignInOperation.VERIFY_CODE) }
        compose.onNode(hasSetTextAction()).assertTextContains("001234")
        compose.onNode(hasText(text(R.string.auth_task_verifying_code)) and hasClickAction()).assertIsNotEnabled()
        compose.runOnIdle { state.value = state.value.copy(operation = null, problem = SignInProblem.CONNECTION) }
        compose.onNode(hasSetTextAction()).assertTextContains("001234")
        compose.onNodeWithText(text(R.string.auth_try_again)).performClick()
        compose.runOnIdle { assertEquals(1, submitted) }
    }

    @Test fun signupAddressCarriesIntoSignInWithoutRetyping() {
        val email = mutableStateOf("")
        val returning = mutableStateOf(false)
        compose.setContent { AttestraAuthTheme {
            if (!returning.value) EmailStartScreen(email.value, { email.value = it }, {}, {},
                onExistingAccount = { returning.value = true })
            else SignInTaskScreen(SignInTaskState(email = email.value), 0L, null, {}, {}, {}, {}, {}, {}, {}, {})
        } }
        compose.onNode(hasSetTextAction()).performTextInput("person@example.test")
        compose.onNodeWithText(text(R.string.auth_already_have_an_account_sign_in)).performClick()
        compose.onNode(hasSetTextAction()).assertTextContains("person@example.test")
    }

    @Test fun cancelledRequiredPasskeyCannotOfferNotNow() {
        var requests = 0
        compose.setContent { AttestraAuthTheme {
            PasskeyTaskScreen(PasskeyStage.CANCELLED, true, {}, { requests++ }, {}, {}, {})
        } }
        compose.onNodeWithText(text(R.string.auth_task_secure_account)).assertExists()
        compose.onNodeWithText(text(R.string.auth_do_this_later)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.auth_try_again)).performClick()
        compose.runOnIdle { assertEquals(1, requests) }
    }

    @Test fun optionalCancelledPasskeyCanReturnToAccountWithoutRetrying() {
        var deferred = 0
        compose.setContent { AttestraAuthTheme {
            PasskeyTaskScreen(PasskeyStage.CANCELLED, false, {}, {}, {}, {}, { deferred++ })
        } }
        compose.onNodeWithText(text(R.string.auth_do_this_later)).performClick()
        compose.runOnIdle { assertEquals(1, deferred) }
    }
}
