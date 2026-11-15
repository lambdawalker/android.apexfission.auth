package com.apexfission.android.auth.signin

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class PasskeyStage {
    READY, OPENING, SAVING, CHECKING, STATUS_FAILED, CANCELLED, UNSUPPORTED, FAILED, REAUTH_REQUIRED;
    val busy: Boolean get() = this == OPENING || this == SAVING || this == CHECKING
}

/** Memory-only orchestration. Options/credentials remain local to the current operation. */
internal class PasskeyTask {
    private val mutable = MutableStateFlow(PasskeyStage.READY)
    val state = mutable.asStateFlow()
    private var generation = 0L
    private var closed = false

    fun reset() { generation++; mutable.value = PasskeyStage.READY }
    fun close() { closed = true; reset() }
    fun statusFailed() { if (state.value == PasskeyStage.CHECKING) mutable.value = PasskeyStage.STATUS_FAILED }
    fun retryStatus(): Boolean {
        if (closed || state.value != PasskeyStage.STATUS_FAILED) return false
        mutable.value = PasskeyStage.CHECKING
        return true
    }

    /** True means registration was acknowledged, not that onboarding is complete. */
    suspend fun enroll(
        options: suspend () -> String,
        create: suspend (String) -> String,
        save: suspend (String) -> Unit,
    ): Boolean {
        if (closed || state.value.busy || state.value == PasskeyStage.STATUS_FAILED) return false
        val requestGeneration = generation
        mutable.value = PasskeyStage.OPENING
        try {
            val parameters = options()
            if (requestGeneration != generation) return false
            val credential = create(parameters)
            if (requestGeneration != generation) return false
            mutable.value = PasskeyStage.SAVING
            save(credential)
            if (requestGeneration != generation) return false
            mutable.value = PasskeyStage.CHECKING
            return true
        } catch (cancelled: CancellationException) {
            if (requestGeneration == generation) mutable.value = PasskeyStage.READY
            throw cancelled
        } catch (error: Exception) {
            if (requestGeneration == generation) mutable.value = when ((error as? SignInFailure)?.problem) {
                SignInProblem.CANCELLED -> PasskeyStage.CANCELLED
                SignInProblem.UNSUPPORTED -> PasskeyStage.UNSUPPORTED
                SignInProblem.SESSION_EXPIRED -> PasskeyStage.REAUTH_REQUIRED
                else -> PasskeyStage.FAILED
            }
            return false
        }
    }
}
