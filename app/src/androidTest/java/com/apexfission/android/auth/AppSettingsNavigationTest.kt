package com.apexfission.android.auth

import android.Manifest
import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.content.ContextCompat
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppSettingsNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun headerOpensApplicationDetailsForTheInstalledPackage() {
        Intents.init()
        try {
            val expected = allOf(
                hasAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS),
                hasData(Uri.fromParts("package", compose.activity.packageName, null)),
            )
            Intents.intending(expected).respondWith(ActivityResult(Activity.RESULT_CANCELED, null))
            compose.onNodeWithText(compose.activity.getString(R.string.demo_app_settings))
                .assertIsDisplayed().performClick()
            Intents.intended(expected)
            compose.onNodeWithText("Attestra test menu").assertIsDisplayed()
        } finally {
            Intents.release()
        }
    }

    @Test fun settingsReturnAndRecreationKeepTheCatalogUsableAfterPermissionGrant() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        val context = instrumentation.targetContext
        val packageName = context.packageName
        // Run on a fresh installation, or revoke CAMERA before starting instrumentation.
        assertEquals(PackageManager.PERMISSION_DENIED,
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA))
        compose.onNodeWithText(compose.activity.getString(R.string.demo_app_settings)).performClick()
        assertTrue("Android Settings opened",
            device.wait(Until.hasObject(By.pkg("com.android.settings").depth(0)), 10_000))
        // Android owns the grant. Do not seed a cached permission value in the demo.
        instrumentation.uiAutomation.grantRuntimePermission(packageName, Manifest.permission.CAMERA)
        device.pressBack()
        assertTrue("Demo returned",
            device.wait(Until.hasObject(By.pkg(packageName).depth(0)), 10_000))
        assertEquals(PackageManager.PERMISSION_GRANTED,
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA))
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText(compose.activity.getString(R.string.demo_app_settings)).assertIsDisplayed()
        compose.onNodeWithText("Open UI catalog").performScrollTo().performClick()
        pressBack()
        compose.onNodeWithText(compose.activity.getString(R.string.demo_app_settings)).assertIsDisplayed()
    }
}
