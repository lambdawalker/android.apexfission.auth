package com.apexfission.android.attestra.auth.capture

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * UI-owner session: each ready pair replaces the previous request. Cancellation is
 * advisory for the server; retake and image_revision provide the authoritative fence.
 * The controller serializes canceled requests and their replacements under one mutex.
 */
class EarlyCaptureSession(private val controller: CaptureController, private val scope: CoroutineScope) {
    private var work: Job? = null
    private var hasPair = false
    private var closed = false

    /** Takes ownership of both bounded color JPEGs. */
    fun imagesReady(front: ByteArray, back: ByteArray): Job {
        if (closed) {
            front.fill(0); back.fill(0)
            return Job().apply { complete() }
        }
        work?.cancel()
        hasPair = true
        controller.beginImageReview()
        controller.invalidateImageReview()
        return scope.launch(start = CoroutineStart.UNDISPATCHED) {
            controller.previewPair(front, back)
        }.also { work = it }
    }

    fun invalidated() {
        if (closed) return
        hasPair = false
        work?.cancel()
        controller.beginImageReview()
        controller.invalidateImageReview()
        work = scope.launch(start = CoroutineStart.UNDISPATCHED) { controller.retakeImages() }
    }

    fun confirmed() {
        if (!closed && hasPair) controller.confirmImages()
    }

    fun close() { closed = true; work?.cancel(); hasPair = false }
}

/** Events for camera hosts supporting early image submission. JPEGs transfer ownership. */
class CaptureCameraEvents(
    val onImagesReady: (ByteArray, ByteArray) -> Unit,
    val onImagesInvalidated: () -> Unit,
    val onConfirmed: () -> Unit,
    val onError: () -> Unit,
    val onCancel: () -> Unit,
)
