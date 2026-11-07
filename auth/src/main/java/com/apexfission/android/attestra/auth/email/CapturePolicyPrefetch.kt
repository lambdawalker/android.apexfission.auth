package com.apexfission.android.attestra.auth.email

import com.apexfission.android.attestra.auth.capture.CapturePolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlin.time.Duration.Companion.minutes

/** In-memory disclosure prefetch owned by one onboarding host and API origin. */
internal class CapturePolicyPrefetch(
    private val scope: CoroutineScope,
    private val fetch: suspend (String) -> CapturePolicy,
) {
    private data class Request(val token: String, val started: TimeMark, val policy: Deferred<CapturePolicy?>)
    private var request: Request? = null

    internal fun start(token: String) {
        if (token.isBlank() || matching(token) != null) return
        clear()
        request = Request(token, TimeSource.Monotonic.markNow(), scope.async {
            try { fetch(token) }
            catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { null }
        })
    }

    internal suspend fun load(token: String): CapturePolicy =
        matching(token)?.policy?.await() ?: fetch(token)

    private fun matching(token: String): Request? =
        request?.takeIf { it.token == token && it.started.elapsedNow() < 2.minutes }

    internal fun clear() {
        request?.policy?.cancel()
        request = null
    }
}
