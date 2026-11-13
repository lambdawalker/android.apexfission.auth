package com.apexfission.android.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.apexfission.android.auth.identity.capture.IdentityCamera
import com.apexfission.android.democommon.CardTitle
import com.apexfission.android.democommon.DemoCard
import com.apexfission.android.democommon.DemoScreen

/** Local camera playground. No capture controller, session, persistence, or upload client. */
@Composable
internal fun LocalCardCaptureDemo(
    onExit: () -> Unit,
    camera: @Composable (
        onCaptured: (ByteArray, ByteArray) -> Unit,
        onError: () -> Unit,
        onCancel: () -> Unit,
    ) -> Unit = { captured, error, cancel ->
        CapturePrivacy()
        IdentityCamera(onCaptured = captured, onError = error, onCancel = cancel)
    },
) {
    // Never restore a completed capture after process recreation; images exist only in memory.
    var completed by remember { mutableStateOf<Boolean?>(null) }
    val outcome = completed
    if (outcome == null) {
        camera(
            { front, back ->
                front.fill(0)
                back.fill(0)
                completed = true
            },
            { completed = false },
            onExit,
        )
    } else {
        LocalCardCaptureResult(
            successful = outcome,
            onRetry = { completed = null },
            onExit = onExit,
        )
    }
}

/** Offline result content; previews never instantiate the camera or other runtime services. */
@Composable
internal fun LocalCardCaptureResult(successful: Boolean, onRetry: () -> Unit, onExit: () -> Unit) {
    DemoScreen(
        title = "Card capture demo",
        welcomeNote = "Local preview only. No sign-in, uploads, or backend processing.",
    ) {
        item(key = "capture_result") {
            DemoCard {
                CardTitle(title = if (successful) "Capture complete" else "Capture unavailable")
                Text(
                    if (successful) "Both sides were captured and discarded. No images were saved or sent."
                    else "The camera could not finish the capture. Try again or return to the demo menu.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onRetry, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
                    Text("Try again")
                }
                OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
                    Text("Back to demo menu")
                }
            }
        }
    }
}
