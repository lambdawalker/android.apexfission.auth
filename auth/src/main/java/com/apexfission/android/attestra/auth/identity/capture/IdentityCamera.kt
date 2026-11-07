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
import com.apexfission.android.permission.requester.HandlePermissions
import com.apexfission.android.permission.ui.DefaultPermissionPage
import com.apexfission.android.permission.ui.PermissionDescription
import com.apexfission.android.permission.ui.PermissionOverviewMode
import com.apexfission.android.carddetector.ui.capture.CardCaptureConfig
import com.apexfission.android.carddetector.ui.capture.TwoSidedCardCapture
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.*

/** Source-compatible confirmation-only camera entry. */
@Composable
fun IdentityCamera(onCaptured: (ByteArray, ByteArray) -> Unit, onError: () -> Unit, onCancel: () -> Unit) =
    IdentityCamera(onCaptured, onError, onCancel, onImagesReady = null)

/**
 * Screen that manages camera permissions and the ID document capture workflow.
 * Permission-gated front/back capture. Side labels guide users; they do not verify identity.
 */
@Composable
fun IdentityCamera(
    onCaptured: (ByteArray, ByteArray) -> Unit,
    onError: () -> Unit,
    onCancel: () -> Unit,
    onImagesReady: ((ByteArray, ByteArray) -> Unit)?,
    onImagesInvalidated: () -> Unit = {},
    onConfirmed: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var encoding by remember { mutableStateOf<Job?>(null) }
    var generation by remember { mutableIntStateOf(0) }
    var encodedGeneration by remember { mutableIntStateOf(-1) }
    val ready by rememberUpdatedState(onImagesReady)
    val failed by rememberUpdatedState(onError)
    val confirmed by rememberUpdatedState(onConfirmed)
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
            onSideChanged = { _, image ->
                if (image == null && ready != null) {
                    generation++
                    encodedGeneration = -1
                    encoding?.cancel()
                    onImagesInvalidated()
                }
            },
            onImagesReady = { result ->
                if (ready != null) {
                    val token = ++generation
                    encodedGeneration = -1
                    encoding?.cancel()
                    // Detector owns these pixels; make independent bounded copies before returning.
                    var frontCopy: Bitmap? = null
                    var backCopy: Bitmap? = null
                    try {
                        frontCopy = copyForEncoding(result.front.bitmap)
                        backCopy = copyForEncoding(result.back.bitmap)
                        val frontImage = frontCopy
                        val backImage = backCopy
                        @OptIn(ExperimentalCoroutinesApi::class)
                        val task = scope.launch(Dispatchers.Default, start = CoroutineStart.ATOMIC) {
                            var front: ByteArray? = null
                            var back: ByteArray? = null
                            var transferred = false
                            try {
                                ensureActive()
                                front = encodeJpeg(frontImage)
                                ensureActive()
                                back = encodeJpeg(backImage)
                                withContext(Dispatchers.Main.immediate) {
                                    if (generation == token) {
                                        ready?.invoke(requireNotNull(front), requireNotNull(back))
                                        transferred = true
                                        encodedGeneration = token
                                    }
                                }
                            } catch (cancel: CancellationException) { throw cancel }
                            catch (_: Exception) {
                                withContext(Dispatchers.Main.immediate) { if (generation == token) failed() }
                            } finally {
                                if (!transferred) { front?.fill(0); back?.fill(0) }
                                frontImage.recycle(); backImage.recycle()
                            }
                        }
                        encoding = task
                    } catch (_: Exception) {
                        frontCopy?.recycle(); backCopy?.recycle()
                        failed()
                    }
                }
            },
            onComplete = { result ->
                if (ready != null) {
                    result.front.bitmap.recycle(); result.back.bitmap.recycle()
                    val token = generation
                    scope.launch {
                        encoding?.join()
                        if (generation == token && encodedGeneration == token) confirmed()
                        else failed()
                    }
                } else {
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
                }
            },
        )
    }
}

private fun copyForEncoding(bitmap: Bitmap): Bitmap {
    val scale = minOf(1.0, 1600.0 / maxOf(bitmap.width, bitmap.height))
    return if (scale < 1.0) Bitmap.createScaledBitmap(bitmap,
        (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), true)
    else requireNotNull(bitmap.copy(Bitmap.Config.ARGB_8888, false))
}

private fun encodeJpeg(bitmap: Bitmap): ByteArray {
    val sourceEdge = maxOf(bitmap.width, bitmap.height)
    val maxBytes = 500 * 1024
    // Keep color and try lower JPEG quality before reducing text resolution further.
    for (maxEdge in listOf(1600, 1200, 960).map { minOf(sourceEdge, it) }.distinct()) {
        val scaled = if (sourceEdge > maxEdge) {
            val scale = maxEdge.toDouble() / sourceEdge
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true,
            )
        } else {
            bitmap
        }
        try {
            for (quality in listOf(90, 80, 70)) {
                ByteArrayOutputStream().use { out ->
                    check(scaled.compress(Bitmap.CompressFormat.JPEG, quality, out))
                    if (out.size() in 4..maxBytes) return out.toByteArray()
                }
            }
        } finally {
            if (scaled !== bitmap) scaled.recycle()
        }
    }
    error("Captured image exceeds the upload size limit")
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
