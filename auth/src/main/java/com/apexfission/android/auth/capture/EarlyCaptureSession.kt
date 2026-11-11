package com.apexfission.android.auth.capture

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
    private var frontWork: Job? = null
    private var hasPair = false
    private var closed = false

    /** Takes ownership of the grayscale front as soon as the camera supplies it. */
    fun frontReady(front: ByteArray) {
        if (closed || hasPair) { front.fill(0); return }
        frontWork?.cancel()
        controller.beginImageReview()
        frontWork = scope.launch(start = CoroutineStart.UNDISPATCHED) { controller.stageFront(front) }
    }

    /** Takes ownership of the two JPEGs. */
    fun imagesReady(front: ByteArray, back: ByteArray, stitched: Boolean = false): Job {
        if (closed) {
            front.fill(0); back.fill(0)
            return Job().apply { complete() }
        }
        work?.cancel()
        hasPair = true
        controller.beginImageReview()
        controller.invalidateImageReview()
        return scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try { frontWork?.join() }
            catch (cancel: kotlinx.coroutines.CancellationException) {
                front.fill(0); back.fill(0)
                throw cancel
            }
            controller.previewPair(front, back, stitched)
        }.also { work = it }
    }

    fun invalidated() {
        if (closed) return
        hasPair = false
        frontWork?.cancel()
        frontWork = null
        work?.cancel()
        controller.beginImageReview()
        controller.invalidateImageReview()
        work = scope.launch(start = CoroutineStart.UNDISPATCHED) { controller.retakeImages() }
    }

    fun confirmed() {
        if (!closed && hasPair) controller.confirmImages()
    }

    fun close() { closed = true; frontWork?.cancel(); work?.cancel(); hasPair = false }
}

/** Events for camera hosts supporting early image submission. JPEGs transfer ownership. */
class CaptureCameraEvents(
    val onImagesReady: (ByteArray, ByteArray) -> Unit,
    val onImagesInvalidated: () -> Unit,
    val onConfirmed: () -> Unit,
    val onError: () -> Unit,
    val onCancel: () -> Unit,
    val stitchedPair: Boolean = false,
    val onFrontReady: (ByteArray) -> Unit = { it.fill(0) },
)
