package com.apexfission.android.attestra.auth.email

import com.apexfission.android.attestra.auth.capture.CapturePolicy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CapturePolicyPrefetchTest {
    private fun policy(version: String) =
        CapturePolicy(true, version, "sample_card", listOf("front", "back"), "image/jpeg", 4194304)

    @Test fun disclosureJoinsInFlightPrefetch() = runBlocking {
        val release = CompletableDeferred<Unit>()
        var calls = 0
        val prefetch = CapturePolicyPrefetch(this) { token ->
            calls++
            release.await()
            policy(token)
        }
        prefetch.start("account")
        val disclosure = async { prefetch.load("account") }
        release.complete(Unit)
        assertEquals(policy("account"), disclosure.await())
        assertEquals(1, calls)
    }

    @Test fun failedPrefetchFallsBackAndDifferentSessionDoesNotReuseIt() = runBlocking {
        val tokens = mutableListOf<String>()
        val prefetch = CapturePolicyPrefetch(this) { token ->
            tokens += token
            if (tokens.size == 1) error("unavailable")
            policy(token)
        }
        prefetch.start("first")
        assertEquals(policy("first"), prefetch.load("first"))
        assertEquals(policy("second"), prefetch.load("second"))
        assertEquals(listOf("first", "first", "second"), tokens)
    }
}
