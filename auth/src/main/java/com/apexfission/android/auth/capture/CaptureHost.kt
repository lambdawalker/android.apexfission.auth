package com.apexfission.android.auth.capture

import com.apexfission.android.auth.ui.onboarding.common.authText
import com.apexfission.android.auth.library.R
import androidx.compose.ui.res.stringResource

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.apexfission.android.auth.ui.onboarding.common.LoadingTask
import com.apexfission.android.auth.ui.onboarding.common.OnboardingLoadingScreen
import com.apexfission.android.auth.identity.DocumentSide
import com.apexfission.android.auth.ui.onboarding.id.CardDetailsSavedScreen
import com.apexfission.android.auth.ui.theme.AttestraAuthTheme
import kotlinx.coroutines.launch

/** Legacy camera slot: submit both JPEGs only after photo confirmation. */
@Composable fun CaptureHost(
    controller: CaptureController,
    onExit: () -> Unit,
    startCaptureOnEntry: Boolean = false,
    acceptedPolicy: CapturePolicy? = null,
    requireReviewedPolicy: Boolean = false,
    allowNewCapture: Boolean = true,
    camera: @Composable ((ByteArray, ByteArray) -> Unit, () -> Unit, () -> Unit) -> Unit,
) = CaptureHost(controller, onExit, startCaptureOnEntry, acceptedPolicy,
    requireReviewedPolicy, allowNewCapture, earlyCamera = null, camera = camera)

/**
 * Screen that orchestrates the ID capture process, guiding the user through capturing, uploading, and reviewing.
 */
@Composable fun CaptureHost(
    controller: CaptureController,
    onExit: () -> Unit,
    startCaptureOnEntry: Boolean = false,
    acceptedPolicy: CapturePolicy? = null,
    requireReviewedPolicy: Boolean = false,
    allowNewCapture: Boolean = true,
    earlyCamera: (@Composable (CaptureCameraEvents) -> Unit)?,
    camera: @Composable ((ByteArray, ByteArray) -> Unit, () -> Unit, () -> Unit) -> Unit = { _, _, _ -> },
) {
    val ui by controller.state.collectAsState()
    val language = LocalConfiguration.current.locales[0].language
    val scope = rememberCoroutineScope()
    val early = remember(controller, scope) { EarlyCaptureSession(controller, scope) }
    DisposableEffect(early) { onDispose { early.close() } }
    var reviewSavedDetails by remember(controller) { mutableStateOf(false) }
    var initializing by remember(controller) { mutableStateOf(true) }
    var refreshGeneration by remember(controller) { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var foreground by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> foreground = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(controller) {
        try {
            if (startCaptureOnEntry) {
                controller.restoreAndContinue(language, allowNewCapture, acceptedPolicy,
                    useRestoredPolicy = !requireReviewedPolicy)
            } else controller.restore()
        } finally { initializing = false }
    }
    DisposableEffect(controller) { onDispose { controller.close() } }
    // Keep a single deadline across finalizing -> extraction pending transitions.
    LaunchedEffect(controller, ui.record?.id, ui.record.isProcessingCard(), foreground, refreshGeneration) {
        if (foreground && ui.record.isProcessingCard()) controller.awaitProcessing()
    }
    BackHandler { controller.close(); onExit() }

    CaptureHostContent(
        ui = ui,
        onExit = onExit,
        initializing = initializing,
        reviewSavedDetails = reviewSavedDetails,
        onReviewSavedDetails = { reviewSavedDetails = true },
        allowNewCapture = allowNewCapture,
        onStart = { lang -> scope.launch { controller.continueToCapture(lang, allowNewCapture) } },
        onCamera = controller::camera,
        onCaptured = { front, back -> scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { controller.capturedBoth(front, back) } },
        onCameraFailed = controller::cameraFailed,
        onCameraCancelled = { controller.cameraCancelled(); onExit() },
        onUpload = { scope.launch { controller.uploadAndContinue() } },
        onFinalizeCapture = { scope.launch { controller.finalizeCapture(resumeSelectedUploads = true) } },
        onRetry = { scope.launch { controller.retry() } },
        onEditCard = controller::editCardField,
        onJurisdiction = controller::confirmJurisdiction,
        onCardFieldFocus = controller::focusCardField,
        onResetReview = controller::resetReview,
        onRescan = { scope.launch {
            controller.retakeImages()
            if (controller.state.value.message == null) controller.camera(DocumentSide.FRONT)
        } },
        onEditOrigin = controller::editOrigin,
        onSaveReview = { scope.launch {
            controller.saveReview()
            if (controller.state.value.hasSavedReview()) reviewSavedDetails = false
        } },
        onRestore = { scope.launch { controller.restore(); refreshGeneration++ } },
        onCancel = { scope.launch { controller.cancel() } },
        onClose = controller::close,
        camera = { captured, error, cancel ->
            if (earlyCamera == null) camera(captured, error, cancel)
            else earlyCamera(CaptureCameraEvents(
                onImagesReady = { front, back -> early.imagesReady(front, back, ui.policy?.stitchedPairSupported == true) },
                stitchedPair = ui.policy?.stitchedPairSupported == true,
                onFrontReady = early::frontReady,
                onImagesInvalidated = early::invalidated,
                onConfirmed = early::confirmed,
                onError = { early.invalidated(); error() },
                onCancel = { early.close(); cancel() },
            ))
        },
    )
}

/**
 * Component of the CaptureHost screen that displays the capture states and provides navigation.
 */
@Composable
internal fun CaptureHostContent(
    ui: CaptureUi,
    onExit: () -> Unit,
    initializing: Boolean = false,
    reviewSavedDetails: Boolean = false,
    onReviewSavedDetails: () -> Unit = {},
    allowNewCapture: Boolean = true,
    onStart: (String) -> Unit = {},
    onCamera: (DocumentSide) -> Unit = {},
    onCaptured: (ByteArray, ByteArray) -> Unit = { _, _ -> },
    onCameraFailed: () -> Unit = {},
    onCameraCancelled: () -> Unit = {},
    onUpload: () -> Unit = {},
    onFinalizeCapture: () -> Unit = {},
    onRetry: () -> Unit = {},
    onEditCard: ((CardData) -> CardData) -> Unit = {},
    onCardFieldFocus: (kotlin.reflect.KProperty1<CardData, String>, Boolean) -> Unit = { _, _ -> },
    onResetReview: () -> Unit = {},
    onRescan: (() -> Unit)? = null,
    onJurisdiction: ((IssuingJurisdiction) -> Unit)? = null,
    onEditOrigin: (String) -> Unit = {},
    onSaveReview: () -> Unit = {},
    onRestore: () -> Unit = {},
    onCancel: () -> Unit = {},
    onClose: () -> Unit = {},
    camera: @Composable ((ByteArray, ByteArray) -> Unit, () -> Unit, () -> Unit) -> Unit = { _, _, _ -> },
) {
    val language = LocalConfiguration.current.locales[0].language
    val destination = captureDestination(ui, initializing, reviewSavedDetails)
    if (destination == CaptureDestination.FINISH) {
        CardDetailsSavedScreen(onDone = { onClose(); onExit() }, onReview = onReviewSavedDetails)
        return
    }
    if (destination == CaptureDestination.CAMERA) {
        camera(onCaptured, onCameraFailed, onCameraCancelled)
        return
    }
    if (destination == CaptureDestination.LOADING) {
        val task = when {
            initializing -> LoadingTask.PREPARE_CAPTURE
            ui.record?.state == "uploading" -> LoadingTask.UPLOAD_DOCUMENT
            else -> LoadingTask.READ_DOCUMENT
        }
        OnboardingLoadingScreen(task, onBack = { onClose(); onExit() })
        return
    }
    if (destination == CaptureDestination.REVIEW) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if ((ui.record?.extraction?.status == "failed" || ui.record?.extraction?.retrying == true) && ui.record?.reviewed == null) {
                    Text(stringResource(R.string.auth_we_could_not_read_your_card_enter_your_details_below), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ui.message?.let { Text(authText(it), color = MaterialTheme.colorScheme.error) }
                if (ui.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                CardReviewForm(ui, onEditCard, onResetReview, onEditOrigin, onSaveReview, onCardFieldFocus, onRescan, onJurisdiction)
                if (ui.message != null) TextButton(shape = MaterialTheme.shapes.small, enabled = !ui.busy, onClick = onRestore) { Text(stringResource(R.string.auth_refresh_status)) }
                TextButton(shape = MaterialTheme.shapes.small, enabled = !ui.busy, onClick = onCancel) { Text(stringResource(R.string.auth_cancel_and_delete_card_data)) }
                TextButton(shape = MaterialTheme.shapes.small, onClick = { onClose(); onExit() }) { Text(stringResource(R.string.auth_continue_later)) }
            }
        }
        return
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.auth_capture_your_id), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(
                stringResource(R.string.auth_capture_both_sides_check_the_photos_and_upload_them_securely_we_r),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (ui.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            ui.message?.let { Text(authText(it), color = MaterialTheme.colorScheme.error) }
            val record = ui.record
            if (record == null || record.state in setOf("requires_recapture", "expired", "cancelled")) {
                ui.policy?.let { policy ->
                    Text(stringResource(R.string.auth_id_card_jpeg_images_up_to_1_s_mib_per_side, policy.maxBytes / (1024 * 1024)), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val disclosureLanguage = ui.record?.disclosureLanguage?.takeIf { it.isNotBlank() } ?: language
                    Text(policy.purposes.text(disclosureLanguage).ifBlank { policy.purpose }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.auth_uploaded_photos_are_retained_for_up_to_1_s_days_after_capture_com, policy.retentionDays), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            when (record?.state) {
                null -> {
                    if (allowNewCapture && ui.policy?.enabled == true) {
                        Button(shape = MaterialTheme.shapes.small, enabled = !ui.busy, onClick = { onStart(language) }) { Text(stringResource(R.string.auth_continue_to_capture)) }
                    } else Text(stringResource(R.string.auth_document_capture_is_not_enabled_for_this_service_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                "uploading" -> {
                    Text(stringResource(R.string.auth_photos_stay_in_memory_until_uploaded_after_leaving_this_screen_un), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(shape = MaterialTheme.shapes.small, enabled = !ui.busy, onClick = { onCamera(DocumentSide.FRONT) }) {
                        Text(if (ui.uploaded.isEmpty()) stringResource(R.string.auth_capture_both_sides) else stringResource(R.string.auth_retake_both_sides))
                    }
                    if (ui.message != null) OutlinedButton(shape = MaterialTheme.shapes.small, enabled = !ui.busy, onClick = onUpload) { Text(stringResource(R.string.auth_retry_upload)) }
                    if (ui.message != null && record.selected.keys.hasCapturePair()) {
                        TextButton(shape = MaterialTheme.shapes.small, enabled = !ui.busy, onClick = onFinalizeCapture) { Text(stringResource(R.string.auth_retry_processing)) }
                    }
                }
                "finalizing" -> Text(stringResource(R.string.auth_checking_the_uploaded_files_you_can_leave_and_return_while_this_f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                "ready" -> {
                    when (record.extraction?.status) {
                        "pending" -> { Text(stringResource(R.string.auth_reading_your_card_details), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground); CircularProgressIndicator(); Text(stringResource(R.string.auth_you_can_leave_and_return_while_this_finishes), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        "failed", "complete" -> Text(stringResource(R.string.auth_refresh_to_load_your_review_form))
                        null -> Text(stringResource(R.string.auth_the_service_has_not_provided_a_reading_result_refresh_status_or_r), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else -> Text(stringResource(R.string.auth_update_the_app_to_view_this_extraction_status), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                "failed" -> { Text(stringResource(R.string.auth_file_checks_are_temporarily_unavailable), color = MaterialTheme.colorScheme.onSurfaceVariant); Button(shape = MaterialTheme.shapes.small, enabled = !ui.busy, onClick = onRetry) { Text(stringResource(R.string.auth_retry_file_checks)) } }
                "requires_recapture", "expired", "cancelled" -> { Text(stringResource(R.string.auth_please_capture_your_document_again), color = MaterialTheme.colorScheme.onSurfaceVariant); Button(shape = MaterialTheme.shapes.small, enabled = !ui.busy && allowNewCapture && ui.policy?.enabled == true, onClick = { onStart(language) }) { Text(stringResource(R.string.auth_start_again)) } }
                else -> Text(stringResource(R.string.auth_this_capture_state_needs_a_newer_app_your_document_has_not_been_m), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!ui.signInRequired) TextButton(shape = MaterialTheme.shapes.small, enabled = !ui.busy, onClick = onRestore) { Text(stringResource(R.string.auth_refresh_status)) }
            if (record != null && record.state !in setOf("expired", "cancelled")) TextButton(shape = MaterialTheme.shapes.small, enabled = !ui.busy, onClick = onCancel) { Text(stringResource(R.string.auth_cancel_and_delete_card_data)) }
            TextButton(shape = MaterialTheme.shapes.small, onClick = { onClose(); onExit() }) { Text(if (ui.signInRequired) stringResource(R.string.auth_return_to_sign_in) else stringResource(R.string.auth_continue_later)) }
        }
    }
}

@Preview(name = "Dark Theme", showBackground = true, backgroundColor = 0xFF0F131B)
@Composable
fun CaptureHostDarkPreview() {
    AttestraAuthTheme(darkTheme = true) {
        CaptureHostContent(
            ui = CaptureUi(
                policy = CapturePolicy(
                    enabled = true,
                    version = "capture-v1",
                    documentType = "id_card",
                    slots = listOf("front", "back"),
                    contentType = "image/jpeg",
                    maxBytes = 4194304L,
                    retentionDays = 7,
                ),
                record = CaptureRecord(
                    id = "sample_capture_id",
                    evidenceVersion = 1,
                    revision = 1,
                    policyVersion = "capture-v1",
                    documentType = "id_card",
                    state = "uploading",
                    expiresAt = 0,
                ),
                uploaded = setOf("front"),
            ),
            onExit = {},
        )
    }
}

@Preview(name = "Light Theme", showBackground = true)
@Composable
fun CaptureHostLightPreview() {
    AttestraAuthTheme(darkTheme = false) {
        CaptureHostContent(
            ui = CaptureUi(
                policy = CapturePolicy(
                    enabled = true,
                    version = "capture-v1",
                    documentType = "id_card",
                    slots = listOf("front", "back"),
                    contentType = "image/jpeg",
                    maxBytes = 4194304L,
                    retentionDays = 7,
                ),
                record = CaptureRecord(
                    id = "sample_capture_id",
                    evidenceVersion = 1,
                    revision = 1,
                    policyVersion = "capture-v1",
                    documentType = "id_card",
                    state = "uploading",
                    expiresAt = 0,
                ),
                uploaded = setOf("front"),
            ),
            onExit = {},
        )
    }
}



