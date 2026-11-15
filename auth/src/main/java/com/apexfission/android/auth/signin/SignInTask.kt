package com.apexfission.android.auth.signin

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class SignInStage { METHODS, CODE, PASSWORD }
internal enum class SignInOperation { SEND_CODE, VERIFY_CODE, PASSWORD, PASSKEY }
internal enum class SignInProblem { CONNECTION, TEMPORARY, REJECTED, THROTTLED, CANCELLED, UNSUPPORTED, SESSION_EXPIRED, UNKNOWN }

/** Deliberately contains no provider message, response body, credential or account identifier. */
internal class SignInFailure(val problem: SignInProblem, val retryAfterMillis: Long = 60_000L) : Exception(problem.name)

/** Memory-only form state. Never put this object in saved state, persistence, logs or analytics. */
internal data class SignInTaskState(
    val email: String = "",
    val code: String = "",
    val password: String = "",
    val stage: SignInStage = SignInStage.METHODS,
    val operation: SignInOperation? = null,
    val problem: SignInProblem? = null,
    val resendAt: Long = 0,
    val blockedUntil: Long = 0,
) {
    val busy: Boolean get() = operation != null
    fun canSendCode(now: Long): Boolean = !busy && now >= maxOf(resendAt, blockedUntil)
    fun canSubmit(now: Long): Boolean = !busy && now >= blockedUntil
    override fun toString(): String = "SignInTaskState(stage=$stage, operation=$operation, problem=$problem, input=redacted)"
}

/** The task owns interaction state, not storage, credentials providers or HTTP serialization. */
internal interface SignInTransport<T> {
    suspend fun requestCode(email: String): String
    suspend fun verifyCode(email: String, challenge: String, code: String): T
    suspend fun signInPassword(email: String, password: String): T
    suspend fun signInPasskey(email: String): T
}

/**
 * UI-owner confined. Each operation marks itself pending before its first suspension.
 * A generation fences responses after exit/account change; only a current success is returned
 * to the host for persistence. No request, retry, resend or credential sheet starts on composition.
 */
internal class SignInTask<T>(
    private val transport: SignInTransport<T>,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val mutable = MutableStateFlow(SignInTaskState())
    val state: StateFlow<SignInTaskState> = mutable.asStateFlow()
    private var challenge: String? = null
    private var generation = 0L
    private var active = false

    fun open(email: String) {
        generation++; active = true; challenge = null
        mutable.value = SignInTaskState(email = email.trim())
    }

    fun close() {
        generation++; active = false; challenge = null
        mutable.value = SignInTaskState()
    }

    fun editEmail(value: String) {
        if (!active || state.value.busy || value == state.value.email) return
        challenge = null
        mutable.value = state.value.copy(email = value, code = "", password = "", stage = SignInStage.METHODS, problem = null)
    }

    fun editCode(value: String) {
        if (active && !state.value.busy) mutable.value = state.value.copy(code = value.filter { it in '0'..'9' }.take(6), problem = null)
    }

    fun editPassword(value: String) {
        if (active && !state.value.busy) mutable.value = state.value.copy(password = value, problem = null)
    }

    fun chooseMethod() = select(SignInStage.METHODS)
    fun choosePassword() = select(SignInStage.PASSWORD)
    private fun select(stage: SignInStage) {
        if (!active || state.value.busy) return
        challenge = null
        mutable.value = state.value.copy(stage = stage, code = "", password = "", problem = null)
    }

    suspend fun requestCode() {
        if (!active || state.value.email.isBlank() || !state.value.canSendCode(now())) return
        val email = state.value.email.trim()
        perform(SignInOperation.SEND_CODE, {
            mutable.value = state.value.copy(resendAt = now() + 60_000L)
            transport.requestCode(email).also { check(it.isNotBlank()) }
        }) { result ->
            challenge = result
            mutable.value = state.value.copy(email = email, code = "", password = "", stage = SignInStage.CODE)
        }
    }

    suspend fun submitCode(): T? {
        val before = state.value
        val proof = challenge ?: return null
        if (before.stage != SignInStage.CODE || !before.code.matches(Regex("[0-9]{6}"))) return null
        return authenticate(SignInOperation.VERIFY_CODE) { transport.verifyCode(before.email.trim(), proof, before.code) }
    }

    suspend fun submitPassword(): T? {
        val before = state.value
        if (before.stage != SignInStage.PASSWORD || before.password.isEmpty()) return null
        return authenticate(SignInOperation.PASSWORD) { transport.signInPassword(before.email.trim(), before.password) }
    }

    suspend fun submitPasskey(): T? {
        val before = state.value
        if (before.stage != SignInStage.METHODS || before.email.isBlank()) return null
        return authenticate(SignInOperation.PASSKEY) { transport.signInPasskey(before.email.trim()) }
    }

    private suspend fun authenticate(operation: SignInOperation, block: suspend () -> T): T? =
        perform(operation, block) {
            challenge = null
            mutable.value = state.value.copy(code = "", password = "")
        }

    private suspend fun <R> perform(operation: SignInOperation, block: suspend () -> R, onSuccess: (R) -> Unit): R? {
        if (!active || !state.value.canSubmit(now())) return null
        val requestGeneration = generation
        mutable.value = state.value.copy(operation = operation, problem = null)
        try {
            val result = block()
            if (!active || requestGeneration != generation) return null
            onSuccess(result)
            return result
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (active && requestGeneration == generation) {
                val problem = when (error) {
                    is SignInFailure -> error.problem
                    is IOException -> SignInProblem.CONNECTION
                    else -> SignInProblem.UNKNOWN
                }
                val blockedUntil = if (error is SignInFailure && problem == SignInProblem.THROTTLED) {
                    now() + error.retryAfterMillis.coerceIn(1_000L, 3_600_000L)
                } else state.value.blockedUntil
                mutable.value = state.value.copy(problem = problem, blockedUntil = blockedUntil)
            }
            return null
        } finally {
            if (active && requestGeneration == generation) mutable.value = state.value.copy(operation = null)
        }
    }
}

/** Classify transport status without disclosing response text or account existence. */
internal fun signInHttpProblem(status: Int): SignInProblem = when {
    status == 429 -> SignInProblem.THROTTLED
    status == 400 || status == 401 -> SignInProblem.REJECTED
    status in 500..599 -> SignInProblem.TEMPORARY
    else -> SignInProblem.UNKNOWN
}
