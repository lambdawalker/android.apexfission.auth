package com.apexfission.android.attestra.auth.capture

import com.apexfission.android.attestra.auth.identity.DocumentSide
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import java.security.MessageDigest
import java.util.UUID

// java.util.Base64 requires API 26; this encoding keeps the auth library on API 24.
internal fun checksum(data: ByteArray): String {
    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    val bytes = MessageDigest.getInstance("SHA-256").digest(data)
    return buildString {
        for (i in bytes.indices step 3) {
            val a = bytes[i].toInt() and 255
            val b = bytes.getOrNull(i + 1)?.toInt()?.and(255) ?: 0
            val c = bytes.getOrNull(i + 2)?.toInt()?.and(255) ?: 0
            append(alphabet[a shr 2]); append(alphabet[((a and 3) shl 4) or (b shr 4)])
            append(if (i + 1 < bytes.size) alphabet[((b and 15) shl 2) or (c shr 6)] else '=')
            append(if (i + 2 < bytes.size) alphabet[c and 63] else '=')
        }
    }
}
data class CaptureUi(
    val policy: CapturePolicy? = null,
    val record: CaptureRecord? = null,
    val busy: Boolean = false,
    val camera: DocumentSide? = null,
    val uploaded: Set<String> = emptySet(),
    val message: String? = null,
    val signInRequired: Boolean = false,
    val draft: CardData? = null,
    val draftRevision: Long? = null,
    val reviewDirty: Boolean = false,
    val originCountry: String = "",
)
/** A stable checkpoint precedes each create/finalize/retry request. GET reconciles uncertain responses. */
class CaptureController(private val api: CaptureGateway, private val store: CaptureCheckpointStore, private val mayCreate: () -> Boolean = { true }) {
    private val mutable = MutableStateFlow(CaptureUi())
    val state = mutable.asStateFlow()
    private val mutex = Mutex()
    private val resumableStates = setOf("uploading", "finalizing", "ready", "failed")
    private var checkpoint = store.load() ?: CaptureCheckpoint()
    private var needsRecapture = checkpoint.awaitingImageConfirmation
    private var closed = false
    private var imageReviewOpen = false
    private var earlyImages = false
    private var expectedImages: Pair<String, Long>? = null
    private var pending: Pair<RegisterUpload, ByteArray>? = null
    private var pendingBack: ByteArray? = null
    private var pendingSecondSlot = "back"
    private val editedFields = mutableSetOf<kotlin.reflect.KProperty1<CardData, String>>()
    private var focusedField: kotlin.reflect.KProperty1<CardData, String>? = null
    private var stagedFrontChecksum: String? = null
    private var acknowledged: Map<String, String> = emptyMap()
    private fun key() = UUID.randomUUID().toString()
    private fun show(value: CaptureUi) {
        if (!closed) mutable.value = value
    }
    private fun save(value: CaptureCheckpoint) { check(!closed); store.save(value); checkpoint = value }
    private suspend fun operation(showBusy: Boolean = true, block: suspend () -> Unit) {
        if (closed) return
        mutex.lock()
        if (closed) { mutex.unlock(); return }
        if (showBusy) show(state.value.copy(busy = true, message = null, camera = if (imageReviewOpen) state.value.camera else null))
        try { currentCoroutineContext().ensureActive(); block() }
        catch (cancel: CancellationException) { throw cancel }
        catch (e: Exception) {
            if (!closed) show(state.value.copy(
                signInRequired = e is CaptureException && e.status == 401,
                message = when {
                    e is CaptureException && e.status == 401 -> "Sign in again to resume your capture."
                    e is CaptureException && e.status == 429 -> "Capture limit reached. Please try again tomorrow."
                    e is CaptureException && e.status == 422 -> "The image could not be accepted. Retake both sides."
                    e is CaptureException && e.status == 409 && state.value.draft != null -> "The saved details changed. Refresh, then discard your edits to load the latest details before editing again."
                    e is CaptureException && e.status == 409 -> "Capture changed or an upload is incomplete. Refresh before continuing."
                    state.value.draft != null -> "We could not save or load the card details. Your edits are still here. Refresh and try again."
                    else -> "We could not finish this step. Refresh to recover, or retry the upload."
                },
            ))
        } finally { if (!closed && showBusy) show(state.value.copy(busy = false)); mutex.unlock() }
    }
    private suspend fun record(r: CaptureRecord) {
        currentCoroutineContext().ensureActive()
        if (closed) throw CancellationException("Capture closed")
        require(r.id.matches(Regex("[a-f0-9]{32}")) && r.evidenceVersion > 0)
        if (earlyImages && checkpoint.id != null && checkpoint.id != r.id) throw CaptureException(409, "capture_changed")
        if (state.value.record?.id != r.id) discard()
        save(checkpoint.copy(id = r.id, create = null))
        acknowledged = if (state.value.record?.id == r.id) acknowledged.filter { (slot, id) -> r.selected[slot] == id } else emptyMap()
        val old = state.value
        val canReview = r.canReviewCard() && (!earlyImages || expectedImages == (r.id to r.imageRevision))
        val savedDraft = old.draft != null && r.reviewed?.data == old.draft && r.reviewed?.originCountry == old.originCountry
        val sameReview = canReview && old.draft != null && old.record?.id == r.id &&
            old.record.imageRevision == r.imageRevision && old.record.reviewed == r.reviewed
        if (!sameReview) {
            editedFields.clear()
            focusedField = null
        }
        val lateResult = sameReview && old.record?.extraction?.retrying == true &&
            r.extraction?.status == "complete" && r.reviewed == null
        val updatedDraft = if (lateResult) {
            requireNotNull(old.draft).mergeOCR(requireNotNull(r.extraction).data, editedFields + listOfNotNull(focusedField))
        } else null
        val keepDraft = canReview && old.record?.id == r.id && old.reviewDirty && !savedDraft
        show(old.copy(
            record = r, policy = r.policy?.copy(stitchedPairSupported = old.policy?.stitchedPairSupported == true) ?: old.policy, uploaded = acknowledged.keys,
            draft = if (!canReview) null else updatedDraft ?: if (keepDraft || sameReview) old.draft else r.reviewData(),
            draftRevision = if (!canReview) null else if (keepDraft && !sameReview) old.draftRevision else r.revision,
            reviewDirty = keepDraft,
            originCountry = if (keepDraft) old.originCountry else if (canReview) r.reviewed?.originCountry.orEmpty() else "",
        ))
        if (r.state != "uploading") discard()
    }
    private suspend fun readCaptureEntry() = coroutineScope {
        val policy = async {
            try { Result.success(api.policy()) }
            catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) { Result.failure(error) }
        }
        val current = api.current()
        policy.await() to current
    }

    suspend fun restore() = operation {
        val (policy, current) = readCaptureEntry()
        restoreCapture(policy.getOrNull(), current)
    }

    private suspend fun restoreCapture(policy: CapturePolicy?, current: CaptureRecord?) {
        // A settings outage must not prevent viewing or deleting an existing capture.
        show(state.value.copy(policy = policy, signInRequired = false))
        val create = checkpoint.create
        val r = if (create != null && mayCreate() && policy?.enabled == true && create.expectedPolicyVersion.isNotBlank()) {
            createOrReview(create)
        } else current
        if (r != null) {
            if (checkpoint.id != r.id) save(CaptureCheckpoint(id = r.id))
            val recoverPhotoReview = checkpoint.awaitingImageConfirmation && needsRecapture
            if (recoverPhotoReview) { earlyImages = true; imageReviewOpen = true }
            record(r)
            if (recoverPhotoReview) {
                // Borrowed camera images cannot survive process death. Never silently
                // confirm an old pair just because its OCR completed on the server.
                expectedImages = null
                resetImages()
                show(state.value.copy(camera = DocumentSide.FRONT))
                needsRecapture = false
            } else {
                if (checkpoint.retake != null) resetImages()
                if (checkpoint.finalize != null && r.state == "uploading") finalizePending(resumeSelectedUploads = true)
                checkpoint.retry?.let { if (r.state == "failed" || r.extraction?.status == "failed") record(api.retry(r.id, it)); save(checkpoint.copy(retry = null)) }
            }
        } else if (checkpoint.create == null) {
            discard(); acknowledged = emptyMap(); store.clear(); checkpoint = CaptureCheckpoint()
            show(state.value.copy(record = null, uploaded = emptySet(), draft = null, draftRevision = null, reviewDirty = false, originCountry = ""))
        }
    }
    /** Poll the known capture directly; configuration outages cannot block status updates. */
    internal suspend fun awaitProcessing(timeoutMillis: Long = 600_000L) {
        val completed = withTimeoutOrNull(timeoutMillis) {
            var attempts = 0
            while (!closed && state.value.record.isProcessingCard() && !state.value.signInRequired && state.value.message == null) {
                delay(when (attempts++) {
                    in 0..14 -> 1000L
                    in 15..44 -> 2000L
                    else -> 5000L
                })
                operation(showBusy = false) { record(api.get(requireNotNull(checkpoint.id))) }
            }
            true
        }
        if (completed == null && !closed && state.value.record.isProcessingCard()) {
            show(state.value.copy(message = "Reading your document is taking longer than expected. Refresh status to check again, or continue later."))
        }
    }
    private suspend fun createOrReview(request: CreateCapture): CaptureRecord? = try {
        api.create(request)
    } catch (e: CaptureException) {
        if (e.status != 409 || e.code != "policy_changed") throw e
        // Server definitively rejected creation: a new intent needs new consent.
        save(checkpoint.copy(create = null))
        val policy = api.policy()
        show(state.value.copy(policy = policy, message = "The capture policy changed. Review the updated details, then select Start capture again."))
        null
    }
    suspend fun start(language: String = "en", reviewedPolicy: CapturePolicy? = state.value.policy) = operation {
        if (!checkAdmission()) return@operation
        val (policy, current) = readCaptureEntry()
        admitCapture(language, reviewedPolicy, policy.getOrThrow(), current)
    }

    private fun checkAdmission(): Boolean {
        if (mayCreate()) return true
        show(state.value.copy(message = "New ID capture is disabled until app settings are refreshed."))
        return false
    }

    private suspend fun admitCapture(
        language: String,
        reviewedPolicy: CapturePolicy?,
        policy: CapturePolicy,
        existing: CaptureRecord?,
    ) {
        show(state.value.copy(policy = policy))
        require(policy.enabled && policy.slots == listOf("front", "back") && policy.contentType == "image/jpeg")
        if (existing?.state in resumableStates) { record(requireNotNull(existing)); return }
        val pending = checkpoint.create
        if (pending == null && reviewedPolicy != policy) {
            show(state.value.copy(message = "Review the current capture policy, then select Start capture again."))
            return
        }
        if (!checkAdmission()) return
        val request = pending?.takeIf { it.expectedPolicyVersion.isNotBlank() }
            ?: CreateCapture(key(), policy.documentType, policy.version, policy.purposes.language(language))
        if (pending != null && pending.expectedPolicyVersion.isBlank()) {
            save(checkpoint.copy(create = null))
            show(state.value.copy(message = "Review the current capture policy, then select Start capture again."))
            return
        }
        save(CaptureCheckpoint(create = request))
        createOrReview(request)?.let { record(it); show(state.value.copy(uploaded = emptySet())) }
    }

    internal suspend fun restoreAndContinue(language: String, allowNewCapture: Boolean, reviewedPolicy: CapturePolicy?, useRestoredPolicy: Boolean) {
        operation {
            val (policy, current) = readCaptureEntry()
            restoreCapture(policy.getOrNull(), current)
            if (state.value.message != null || state.value.record?.state in resumableStates || !allowNewCapture) return@operation
            if (!checkAdmission()) return@operation
            admitCapture(language, if (useRestoredPolicy) state.value.policy else reviewedPolicy, policy.getOrThrow(), current)
        }
        openCaptureCamera()
    }

    /** Continue is the only admission action; existing evidence resumes without reopening the camera. */
    suspend fun continueToCapture(language: String = "en", allowNewCapture: Boolean = true, reviewedPolicy: CapturePolicy? = state.value.policy) {
        if (closed || state.value.busy) return
        if (state.value.record?.state !in resumableStates) {
            if (!allowNewCapture) return
            start(language, reviewedPolicy)
        }
        openCaptureCamera()
    }
    private suspend fun openCaptureCamera() {
        if (closed || state.value.message != null || state.value.record?.state != "uploading") return
        if (state.value.record?.selected?.keys?.hasCapturePair() == true) {
            // Selection alone is not proof of upload: finalize HEAD-checks both objects on the server.
            finalizeCapture(resumeSelectedUploads = true)
        } else camera(DocumentSide.FRONT)
    }
    suspend fun uploadAndContinue() {
        upload()
        if (!closed && state.value.message == null && state.value.uploaded.hasCapturePair()) {
            finalizeCapture()
        }
    }
    fun camera(side: DocumentSide) {
        if (closed || state.value.busy || state.value.record?.state != "uploading") return
        discard(); save(checkpoint.copy(finalize = null)); show(state.value.copy(camera = side, message = null))
    }
    fun cameraCancelled() { show(state.value.copy(camera = null)) }
    fun cameraFailed() { show(state.value.copy(camera = null, message = "Could not capture a clear image. Try again.")) }
    suspend fun captured(side: DocumentSide, bytes: ByteArray) {
        if (closed || state.value.busy || state.value.camera != side) { bytes.fill(0); return }
        val r = state.value.record ?: run { bytes.fill(0); return }
        if (bytes.size !in 4..CaptureApi.MAX_JPEG_BYTES) { bytes.fill(0); cameraFailed(); return }
        discard()
        pending = RegisterUpload(key(), r.revision, side.name.lowercase(), checksum(bytes), bytes.size.toLong()) to bytes
        upload()
    }
    /** Takes ownership of both reviewed JPEGs, retaining unfinished uploads for Retry upload. */
    suspend fun capturedBoth(front: ByteArray, back: ByteArray) {
        val ui = state.value
        if (closed || ui.busy || ui.camera == null || ui.record?.state != "uploading" ||
            front.size !in 4..CaptureApi.MAX_JPEG_BYTES || back.size !in 4..CaptureApi.MAX_JPEG_BYTES) {
            front.fill(0); back.fill(0); cameraFailed(); return
        }
        if (checkpoint.awaitingImageConfirmation) confirmImages()
        discard()
        pendingBack = back
        pendingSecondSlot = "back"
        val r = requireNotNull(ui.record)
        pending = RegisterUpload(key(), r.revision, "front", checksum(front), front.size.toLong()) to front
        uploadAndContinue()
    }
    suspend fun upload() = operation { uploadPending() }
    private suspend fun uploadPending() {
        while (pending != null) {
            val (request, bytes) = requireNotNull(pending)
            val r = requireNotNull(state.value.record)
            val instructions = try {
                api.register(r.id, request)
            } catch (e: CaptureException) {
                if (e.status != 409) throw e
                val current = api.get(r.id)
                if (current.state != "uploading" || current.selected != r.selected) throw e
                record(current)
                api.register(r.id, request.copy(revision = current.revision))
            }
            require(instructions.capture.id == r.id)
            record(instructions.capture)
            api.put(instructions, bytes)
            currentCoroutineContext().ensureActive()
            if (request.slot == "front") {
                val confirmed = try {
                    api.confirmFront(r.id, instructions.uploadId)
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (failure: Exception) {
                    if ((failure as? CaptureException)?.status in setOf(401, 409, 422)) throw failure
                    null
                }
                confirmed?.let { record(it) }
            }
            acknowledged = acknowledged + (request.slot to instructions.uploadId)
            show(state.value.copy(uploaded = acknowledged.keys))
            bytes.fill(0)
            pending = null
            val back = pendingBack
            pendingBack = null
            if (back != null) {
                val current = requireNotNull(state.value.record)
                pending = RegisterUpload(key(), current.revision, pendingSecondSlot, checksum(back), back.size.toLong()) to back
            }
        }
    }
    suspend fun finalizeCapture(resumeSelectedUploads: Boolean = false) = operation { finalizePending(resumeSelectedUploads) }
    private suspend fun finalizePending(resumeSelectedUploads: Boolean = false) {
        val r = api.get(requireNotNull(checkpoint.id)); record(r)
        if (r.state != "uploading") return
        require(state.value.uploaded.hasCapturePair() || checkpoint.finalize != null ||
            (resumeSelectedUploads && r.selected.keys.hasCapturePair()))
        var current = r
        repeat(4) {
            val request = checkpoint.finalize ?: FinalizeCapture(key(), current.revision, current.selected.toMap())
            save(checkpoint.copy(finalize = request))
            if (earlyImages) expectedImages = r.id to (request.revision + 1)
            try {
                record(api.finalize(r.id, request))
                return
            } catch (e: CaptureException) {
                if (e.status != 409) throw e
                current = api.get(r.id)
                if (current.state != "uploading" || current.selected != r.selected || current.revision == request.revision) throw e
                record(current)
                save(checkpoint.copy(finalize = null))
            }
        }
        throw CaptureException(409, "revision_conflict")
    }
    /** UI-owner calls only. Keep the detector review visible while requests run. */
    internal fun beginImageReview() {
        if (closed) return
        earlyImages = true
        needsRecapture = false
        imageReviewOpen = true
        save(checkpoint.copy(awaitingImageConfirmation = true))
    }
    internal fun invalidateImageReview() {
        expectedImages = null
        show(state.value.copy(draft = null, draftRevision = null, reviewDirty = false, originCountry = ""))
    }
    internal fun confirmImages() {
        if (closed) return
        needsRecapture = false
        save(checkpoint.copy(awaitingImageConfirmation = false))
        imageReviewOpen = false
        show(state.value.copy(camera = null))
    }
    internal suspend fun stageFront(front: ByteArray) {
        try {
            operation {
                require(front.size in 4..CaptureApi.MAX_JPEG_BYTES)
                resetImages()
                val r = requireNotNull(state.value.record)
                val digest = checksum(front)
                pending = RegisterUpload(key(), r.revision, "front", digest, front.size.toLong()) to front
                uploadPending()
                stagedFrontChecksum = digest
            }
        } finally {
            if (pending?.second === front) pending = null
            front.fill(0)
        }
    }

    /** Takes ownership, including cancellation while waiting for another operation. */
    internal suspend fun previewPair(front: ByteArray, back: ByteArray, stitched: Boolean = false) {
        var retained = false
        try {
            operation {
                require(front.size in 4..CaptureApi.MAX_JPEG_BYTES && back.size in 4..CaptureApi.MAX_JPEG_BYTES)
                val digest = checksum(front)
                val reuseFront = stitched && stagedFrontChecksum == digest && "front" in acknowledged
                if (!reuseFront) resetImages()
                val r = requireNotNull(state.value.record)
                require(r.state == "uploading")
                discard()
                if (reuseFront) {
                    front.fill(0)
                    pending = RegisterUpload(key(), r.revision, "stitched", checksum(back), back.size.toLong()) to back
                } else {
                    pendingBack = back
                    pendingSecondSlot = if (stitched) "stitched" else "back"
                    pending = RegisterUpload(key(), r.revision, "front", digest, front.size.toLong()) to front
                }
                retained = true
                uploadPending()
                finalizePending()
            }
        } finally {
            if (!retained) { front.fill(0); back.fill(0) }
        }
    }
    internal suspend fun retakeImages() = operation { resetImages() }
    private suspend fun resetImages() {
        stagedFrontChecksum = null
        discard()
        val id = requireNotNull(checkpoint.id)
        var r = api.get(id)
        currentCoroutineContext().ensureActive()
        if (checkpoint.retake == null && r.state == "uploading" && r.selected.isEmpty() && r.imageRevision == null) {
            record(r)
            return
        }
        // Persist the intent before the request: replay the same key after response loss.
        repeat(3) {
            val request = checkpoint.retake ?: RetakeCapture(key(), r.revision)
            save(checkpoint.copy(retake = request, finalize = null, retry = null))
            try {
                val reset = api.retake(id, request)
                currentCoroutineContext().ensureActive()
                require(reset.id == id && reset.state == "uploading" && reset.imageRevision == null && reset.selected.isEmpty())
                expectedImages = null
                acknowledged = emptyMap()
                record(reset)
                save(checkpoint.copy(retake = null))
                return
            } catch (conflict: CaptureException) {
                if (conflict.status != 409) throw conflict
                r = api.get(id)
                currentCoroutineContext().ensureActive()
                // A worker may have advanced revision between GET and retake.
                save(checkpoint.copy(retake = null))
            }
        }
        throw CaptureException(409, "revision_conflict")
    }
    suspend fun retry() = operation {
        val r = api.get(requireNotNull(checkpoint.id)); record(r)
        if (r.state != "failed" && !(r.state == "ready" && r.extraction?.status == "failed")) return@operation
        val operationKey = checkpoint.retry ?: key()
        save(checkpoint.copy(retry = operationKey))
        record(api.retry(r.id, operationKey))
        save(checkpoint.copy(retry = null))
    }
    suspend fun cancel() = operation {
        val id = checkpoint.id ?: return@operation
        record(api.cancel(id)); discard(); acknowledged = emptyMap(); store.clear(); checkpoint = CaptureCheckpoint()
        show(state.value.copy(record = null, uploaded = emptySet(), draft = null, draftRevision = null, reviewDirty = false, originCountry = ""))
    }
    internal fun focusCardField(field: kotlin.reflect.KProperty1<CardData, String>, focused: Boolean) {
        if (focused) focusedField = field
        else if (focusedField == field) focusedField = null
    }
    internal fun editCardField(change: (CardData) -> CardData) {
        state.value.draft?.let { editCard(change(it)) }
    }
    fun editCard(data: CardData) {
        val ui = state.value
        if (closed || ui.busy || ui.draft == null) return
        editedFields += cardFields.filter { it.get(data) != it.get(ui.draft) }
        show(ui.copy(draft = data, reviewDirty = true, message = null))
    }
    fun resetReview() {
        val ui = state.value
        val r = ui.record ?: return
        if (closed || ui.busy || !r.canReviewCard()) return
        if (r.extraction?.retrying != true) editedFields.clear()
        show(ui.copy(draft = r.reviewData(), draftRevision = r.revision, reviewDirty = false, originCountry = r.reviewed?.originCountry.orEmpty(), message = null))
    }
    fun editOrigin(country: String) {
        val ui = state.value
        if (closed || ui.busy || ui.draft == null || country !in listOf("CA", "US", "MX", "GT", "BZ")) return
        show(ui.copy(originCountry = country, reviewDirty = true, message = null))
    }
    suspend fun saveReview() = operation {
        val ui = state.value
        val r = requireNotNull(ui.record)
        val data = requireNotNull(ui.draft)
        if (!data.isValid() || data.isEmpty() || ui.originCountry !in listOf("CA", "US", "MX", "GT", "BZ")) {
            show(ui.copy(message = "Enter the visible card details and select your country of origin. Each field must be at most 512 bytes."))
            return@operation
        }
        require(r.canReviewCard())
        var revision = requireNotNull(ui.draftRevision)
        repeat(4) {
            try {
                record(api.review(r.id, ReviewCapture(revision, data, ui.originCountry)))
                return@operation
            } catch (e: CaptureException) {
                if (e.status != 409 || r.extraction?.retrying != true) throw e
                val latest = api.get(r.id)
                if (latest.imageRevision != r.imageRevision || latest.reviewed != r.reviewed ||
                    !latest.canReviewCard() || latest.revision == revision) throw e
                revision = latest.revision
            }
        }
        throw CaptureException(409, "revision_conflict")
    }
    private fun discard() { pending?.second?.fill(0); pending = null; pendingBack?.fill(0); pendingBack = null }
    fun close() { closed = true; discard(); mutable.value = CaptureUi(signInRequired = mutable.value.signInRequired) }
}

internal fun Set<String>.hasCapturePair(): Boolean = "front" in this && ("back" in this || "stitched" in this)
