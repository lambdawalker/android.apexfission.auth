package com.apexfission.android.attestra.auth.identity.capture

import com.apexfission.android.attestra.auth.library.R
import androidx.compose.ui.res.stringResource

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.apexfission.android.attestra.auth.capture.CaptureApi
import com.apexfission.android.permission.requester.HandlePermissions
import com.apexfission.android.permission.ui.DefaultPermissionPage
import com.apexfission.android.permission.ui.PermissionDescription
import com.apexfission.android.permission.ui.PermissionOverviewMode
import com.apexfission.android.carddetector.ui.capture.CardCaptureConfig
import com.apexfission.android.carddetector.ui.capture.TwoSidedCardCapture
import java.io.ByteArrayOutputStream

/**
 * Screen that manages camera permissions and the ID document capture workflow.
 * Permission-gated front/back capture. Side labels guide users; they do not verify identity.
 */
@Composable
fun IdentityCamera(onCaptured: (ByteArray, ByteArray) -> Unit, onError: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
        NoCameraContent(onCancel)
        return
    }
    HandlePermissions(
        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        permissions = listOf(PermissionDescription(Manifest.permission.CAMERA) {
            DefaultPermissionPage(stringResource(R.string.auth_camera), Icons.Default.PhotoCamera, title = stringResource(R.string.auth_scan_both_sides_of_your_id),
                body = stringResource(R.string.auth_allow_the_camera_to_capture_the_front_and_back_you_can_cancel_and))
        }),
        overviewMode = PermissionOverviewMode.Hide,
        onBack = onCancel, onNotNow = onCancel,
    ) {
        TwoSidedCardCapture(
            instanceKey = "identity-document",
            modelPath = IdentityCardModel.path,
            classLabels = IdentityCardModel.classes,
            cardClasses = IdentityCardModel.cardClasses,
            config = CardCaptureConfig(autoCaptureEnabled = true),
            onBack = onCancel,
            onComplete = { result ->
                var front: ByteArray? = null
                var back: ByteArray? = null
                try {
                    front = encodeJpeg(result.front.bitmap)
                    back = encodeJpeg(result.back.bitmap)
                } catch (_: Exception) {
                    front?.fill(0); back?.fill(0)
                    onError()
                } finally {
                    result.front.bitmap.recycle()
                    result.back.bitmap.recycle()
                }
                if (front != null && back != null) onCaptured(front, back)
            },
        )
    }
}

private fun encodeJpeg(bitmap: Bitmap): ByteArray {
    val maxEdge = 2048
    val scale = minOf(1f, maxEdge.toFloat() / maxOf(bitmap.width, bitmap.height))
    val scaled = if (scale < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), true) else bitmap
    try {
        return ByteArrayOutputStream().use { out ->
            check(scaled.compress(Bitmap.CompressFormat.JPEG, 90, out))
            out.toByteArray().also { require(it.size <= CaptureApi.MAX_JPEG_BYTES) }
        }
    } finally { if (scaled !== bitmap) scaled.recycle() }
}

/** Pure fallback UI; the runtime camera entry and preview use the same content. */
@Composable
internal fun NoCameraContent(onCancel: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground) {
        Column(Modifier.padding(24.dp)) { Text(stringResource(R.string.auth_this_device_has_no_camera)); Button(shape = MaterialTheme.shapes.small, onClick = onCancel) { Text(stringResource(R.string.auth_return)) } }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "NoCameraContent", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun NoCameraContentPreview() {
    com.apexfission.android.attestra.auth.ui.theme.AttestraAuthTheme {
        androidx.compose.material3.Surface {
            androidx.compose.foundation.layout.Column {
                NoCameraContent(onCancel = {})
            }
        }
    }
}
