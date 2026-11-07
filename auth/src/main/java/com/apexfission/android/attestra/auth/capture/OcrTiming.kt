package com.apexfission.android.attestra.auth.capture

import kotlinx.coroutines.CancellationException
import kotlin.time.TimeSource

internal inline fun <T> timeOcrPhase(phase: String, diagnostic: (String) -> Unit, block: () -> T): T {
    val start = TimeSource.Monotonic.markNow()
    var outcome = "failed"
    try {
        return block().also { outcome = "completed" }
    } catch (cancel: CancellationException) {
        outcome = "cancelled"
        throw cancel
    } finally {
        diagnostic("timing phase=$phase elapsed_ms=${start.elapsedNow().inWholeMilliseconds} outcome=$outcome")
    }
}
