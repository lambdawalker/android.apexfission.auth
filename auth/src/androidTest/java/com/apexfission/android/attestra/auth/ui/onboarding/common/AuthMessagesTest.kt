package com.apexfission.android.attestra.auth.ui.onboarding.common

import android.content.res.Configuration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.apexfission.android.attestra.auth.library.R
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthMessagesTest {
    private fun context(language: String) = InstrumentationRegistry.getInstrumentation().targetContext.let {
        it.createConfigurationContext(Configuration(it.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) })
    }

    @Test fun controllerErrorsFollowHostLocale() {
        val german = context("de")
        val actual = AuthMessages(german).localize("Sign in again to resume your capture.")
        assertEquals(german.getString(R.string.auth_sign_in_again_to_resume_your_capture), actual)
        assertNotEquals("Sign in again to resume your capture.", actual)
    }

    @Test fun formattedPolicyMessagesKeepTheirArguments() {
        val japanese = context("ja")
        assertEquals(japanese.getString(R.string.auth_use_1_s_2_s_characters, "8", "64"),
            AuthMessages(japanese).localize("Use 8–64 characters"))
    }

    @Test fun unknownHostTextIsPreserved() {
        assertEquals("Custom host error: 123", AuthMessages(context("fr")).localize("Custom host error: 123"))
    }

    @Test fun localeChangeCreatesNewTranslations() {
        val english = AuthMessages(context("en")).localize("Back")
        val spanish = AuthMessages(context("es")).localize("Back")
        assertNotEquals(english, spanish)
    }
}
