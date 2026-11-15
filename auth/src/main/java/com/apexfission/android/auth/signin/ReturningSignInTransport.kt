package com.apexfission.android.auth.signin

import com.apexfission.android.auth.email.AuthSession
import com.apexfission.android.auth.passkey.PasskeyApiException
import com.apexfission.android.auth.passkey.PasskeyCancelled
import com.apexfission.android.auth.passkey.PasskeyUnsupported
import com.apexfission.android.auth.passkey.ReturnAuthApi
import com.apexfission.android.auth.passkey.SignInError

/** Adapter keeps native credentials and provider errors out of presentation state. */
internal class ReturningSignInTransport(
    private val api: ReturnAuthApi,
    private val authenticatePasskey: suspend (String) -> String,
) : SignInTransport<AuthSession> {
    override suspend fun requestCode(email: String): String = authOperation { api.start(email, false).session }
    override suspend fun verifyCode(email: String, challenge: String, code: String): AuthSession =
        authOperation { api.finishEmail(email, challenge, code) }
    override suspend fun signInPassword(email: String, password: String): AuthSession = authOperation {
        val challenge = api.startPassword(email)
        api.finishPassword(email, challenge.session, password)
    }
    override suspend fun signInPasskey(email: String): AuthSession = authOperation {
        val challenge = api.start(email, true)
        val credential = authenticatePasskey(challenge.options)
        api.finishPasskey(email, challenge.session, credential)
    }
}

internal suspend fun <T> authOperation(block: suspend () -> T): T = try {
    block()
} catch (_: PasskeyCancelled) {
    throw SignInFailure(SignInProblem.CANCELLED)
} catch (_: PasskeyUnsupported) {
    throw SignInFailure(SignInProblem.UNSUPPORTED)
} catch (error: SignInError) {
    throw SignInFailure(signInHttpProblem(error.httpStatus), (error.retryAfterSeconds ?: 60L) * 1_000L)
} catch (error: PasskeyApiException) {
    throw SignInFailure(if (error.signInRequired) SignInProblem.SESSION_EXPIRED else SignInProblem.UNKNOWN)
}
