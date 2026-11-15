package com.apexfission.android.auth.identity.capture

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.apexfission.android.auth.library.R
import com.apexfission.android.auth.ui.onboarding.common.LoadingTask
import com.apexfission.android.auth.ui.onboarding.common.OnboardingLoadingScreen
import com.apexfission.android.permission.recipe.PermissionStatus
import com.apexfission.android.permission.ui.DefaultPermissionPage
import com.apexfission.android.permission.ui.PermissionBundleScreen
import com.apexfission.android.permission.ui.PermissionDescription
import com.apexfission.android.permission.ui.PermissionOverviewMode

/**
 * Camera entry after a user has accepted a disclosure explaining camera use.
 * Requests Android permission once, without a second app explanation/Continue page.
 * Call only when capture is actually needed, not when restoring processing or saved details.
 * Existing [IdentityCamera] entries retain their explanation-first behavior.
 */
@Composable
fun IdentityCameraAfterDisclosure(
    onCaptured: (ByteArray, ByteArray) -> Unit,
    onError: () -> Unit,
    onCancel: () -> Unit,
    onImagesReady: ((ByteArray, ByteArray) -> Unit)? = null,
    onImagesInvalidated: () -> Unit = {},
    onConfirmed: () -> Unit = {},
    stitchedPair: Boolean = false,
    onFrontReady: (ByteArray) -> Unit = { it.fill(0) },
) {
    val context = LocalContext.current
    val activity = remember(context) { context.capturePermissionActivity() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentError by rememberUpdatedState(onError)
    val hasCamera = remember(context) { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    fun isGranted() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    fun needsRationale() = activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA) } == true

    // Save only the attempt marker, never permission authority. Recreating the UI
    // must not launch another prompt; a lost result has explicit recovery instead.
    var attempted by rememberSaveable { mutableStateOf(false) }
    var inFlight by remember { mutableStateOf(false) }
    var granted by remember(context) { mutableStateOf(isGranted()) }
    var canAskAgain by remember(activity) { mutableStateOf(needsRationale()) }
    var foreground by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        // Recheck the actual grant rather than treating a callback as permission.
        granted = isGranted()
        canAskAgain = needsRationale()
        attempted = true
        inFlight = false
    }
    DisposableEffect(lifecycle, context) {
        val observer = LifecycleEventObserver { _, event ->
            foreground = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = isGranted()
                canAskAgain = needsRationale()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val requestPermission: () -> Unit = {
        if (!inFlight && foreground) {
            attempted = true
            inFlight = true
            try {
                launcher.launch(Manifest.permission.CAMERA)
            } catch (_: IllegalStateException) {
                inFlight = false
                currentError()
            } catch (_: SecurityException) {
                inFlight = false
                currentError()
            }
        }
    }
    val route = cameraPermissionRoute(hasCamera, granted, attempted, inFlight, foreground)
    LaunchedEffect(route) {
        if (route == CameraPermissionRoute.REQUEST) requestPermission()
    }
    when (route) {
        CameraPermissionRoute.CAMERA, CameraPermissionRoute.NO_CAMERA -> IdentityCamera(
            onCaptured = onCaptured,
            onError = onError,
            onCancel = onCancel,
            onImagesReady = onImagesReady,
            onImagesInvalidated = onImagesInvalidated,
            onConfirmed = onConfirmed,
            stitchedPair = stitchedPair,
            onFrontReady = onFrontReady,
        )
        CameraPermissionRoute.REQUEST, CameraPermissionRoute.WAIT ->
            OnboardingLoadingScreen(LoadingTask.PREPARE_CAPTURE, onBack = onCancel)
        CameraPermissionRoute.RECOVERY -> CameraPermissionRecovery(
            canAskAgain = canAskAgain,
            onRequest = requestPermission,
            onSettings = {
                try {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (_: ActivityNotFoundException) {
                    currentError()
                } catch (_: SecurityException) {
                    currentError()
                }
            },
            onCancel = onCancel,
        )
    }
}

/** Pure recovery content shared by runtime, previews and screenshot tests. */
@Composable
internal fun CameraPermissionRecovery(
    canAskAgain: Boolean,
    onRequest: () -> Unit,
    onSettings: () -> Unit,
    onCancel: () -> Unit,
) {
    PermissionBundleScreen(
        permissions = listOf(PermissionDescription(Manifest.permission.CAMERA) {
            DefaultPermissionPage(
                stringResource(R.string.auth_camera), Icons.Default.PhotoCamera,
                title = stringResource(R.string.auth_scan_both_sides_of_your_id),
                body = stringResource(R.string.auth_allow_the_camera_to_capture_the_front_and_back_you_can_cancel_and),
            )
        }),
        statuses = listOf(if (canAskAgain) PermissionStatus.RationaleRequired else PermissionStatus.PermanentlyDenied),
        onBack = onCancel,
        onNotNow = onCancel,
        onRequest = onRequest,
        onOpenSettings = onSettings,
        overviewMode = PermissionOverviewMode.Hide,
        autoAdvance = false,
        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
    )
}

private tailrec fun Context.capturePermissionActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> if (baseContext === this) null else baseContext.capturePermissionActivity()
    else -> null
}
