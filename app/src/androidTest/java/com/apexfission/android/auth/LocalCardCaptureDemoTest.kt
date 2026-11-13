package com.apexfission.android.auth

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Exercises demo callbacks with a camera substitute, without permissions, images, or services. */
class LocalCardCaptureDemoTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun confirmationDiscardsBothImagesAndRetryReopensCamera() {
        val front = byteArrayOf(1, 2, 3)
        val back = byteArrayOf(4, 5)
        compose.setContent {
            AttestraAuthTheme {
                LocalCardCaptureDemo(onExit = {}, camera = { captured, _, _ ->
                    Button(onClick = { captured(front, back) }) { Text("Confirm test capture") }
                })
            }
        }
        compose.onNodeWithText("Confirm test capture").performClick()
        compose.onNodeWithText("Capture complete").assertIsDisplayed()
        compose.runOnIdle {
            assertArrayEquals(ByteArray(3), front)
            assertArrayEquals(ByteArray(2), back)
        }
        compose.onNodeWithText("Try again").performClick()
        compose.onNodeWithText("Confirm test capture").assertIsDisplayed()
    }

    @Test
    fun failureOffersRetryAndReturnToMenu() {
        var exits = 0
        compose.setContent {
            AttestraAuthTheme {
                LocalCardCaptureDemo(onExit = { exits++ }, camera = { _, error, _ ->
                    Button(onClick = error) { Text("Simulate camera failure") }
                })
            }
        }
        compose.onNodeWithText("Simulate camera failure").performClick()
        compose.onNodeWithText("Capture unavailable").assertIsDisplayed()
        compose.onNodeWithText("Try again").performClick()
        compose.onNodeWithText("Simulate camera failure").performClick()
        compose.onNodeWithText("Back to demo menu").performClick()
        compose.runOnIdle { assertEquals(1, exits) }
    }

    @Test
    fun cameraCancellationReturnsToMenu() {
        var exits = 0
        compose.setContent {
            AttestraAuthTheme {
                LocalCardCaptureDemo(onExit = { exits++ }, camera = { _, _, cancel ->
                    Button(onClick = cancel) { Text("Cancel test capture") }
                })
            }
        }
        compose.onNodeWithText("Cancel test capture").performClick()
        compose.runOnIdle { assertEquals(1, exits) }
    }
}
