package com.apexfission.android.auth.signin

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test

class SignInTaskTest {
    private class Transport : SignInTransport<String> {
        var sends = 0
        var verifies = 0
        var passwords = 0
        var passkeys = 0
        var failure: Exception? = null
        var hold: CompletableDeferred<Unit>? = null
        var submittedCode = ""
        var submittedPassword = ""
        var submittedChallenge = ""
        override suspend fun requestCode(email: String): String {
            sends++; hold?.await(); failure?.let { throw it }; return "challenge-$sends"
        }
        override suspend fun verifyCode(email: String, challenge: String, code: String): String {
            verifies++; submittedCode = code; submittedChallenge = challenge
            hold?.await(); failure?.let { throw it }; return "session"
        }
        override suspend fun signInPassword(email: String, password: String): String {
            passwords++; submittedPassword = password
            hold?.await(); failure?.let { throw it }; return "session"
        }
        override suspend fun signInPasskey(email: String): String {
            passkeys++; hold?.await(); failure?.let { throw it }; return "session"
        }
    }
    @Test fun httpFailuresDistinguishRejectionThrottlingAndTemporaryServiceErrors() {
        check(signInHttpProblem(400) == SignInProblem.REJECTED)
        check(signInHttpProblem(401) == SignInProblem.REJECTED)
        check(signInHttpProblem(429) == SignInProblem.THROTTLED)
        check(signInHttpProblem(503) == SignInProblem.TEMPORARY)
        check(signInHttpProblem(500) == SignInProblem.TEMPORARY)
        check(signInHttpProblem(404) == SignInProblem.UNKNOWN)
    }
    private fun task(transport: Transport, now: () -> Long = { 1_000L }) =
        SignInTask(transport, now).also { it.open("person@example.test") }

    @Test fun connectionFailureKeepsCodeAndChallengeForExplicitRetry() = runBlocking {
        val api = Transport(); val task = task(api)
        task.requestCode(); task.editCode("001234")
        api.failure = IOException("secret transport detail")
        check(task.submitCode() == null)
        check(task.state.value.stage == SignInStage.CODE)
        check(task.state.value.code == "001234")
        check(task.state.value.problem == SignInProblem.CONNECTION)
        check(task.state.value.operation == null)
        check(api.sends == 1 && api.verifies == 1)
        api.failure = null
        check(task.submitCode() == "session")
        check(api.submittedCode == "001234" && api.submittedChallenge == "challenge-1")
        check(api.sends == 1 && task.state.value.code.isEmpty())
    }
    @Test fun duplicateVerifyAndEditsAreBlockedWhilePending() = runBlocking {
        val api = Transport(); val task = task(api)
        task.requestCode(); task.editCode("123456"); api.hold = CompletableDeferred()
        val pending = launch(start = CoroutineStart.UNDISPATCHED) { task.submitCode() }
        check(task.state.value.operation == SignInOperation.VERIFY_CODE)
        check(task.state.value.stage == SignInStage.CODE)
        task.editCode("999999"); task.editEmail("other@example.test"); task.choosePassword()
        check(task.submitCode() == null)
        check(api.verifies == 1 && task.state.value.code == "123456")
        check(task.state.value.email == "person@example.test")
        api.hold!!.complete(Unit); pending.join()
    }
    @Test fun passwordSurvivesConnectionFailureButNotMethodChange() = runBlocking {
        val api = Transport(); val task = task(api); task.choosePassword(); task.editPassword("SampleSecret42!")
        api.failure = IOException()
        check(task.submitPassword() == null)
        check(task.state.value.password == "SampleSecret42!")
        check(task.state.value.stage == SignInStage.PASSWORD)
        api.failure = null
        check(task.submitPassword() == "session" && api.passwords == 2)
        check(api.submittedPassword == "SampleSecret42!" && task.state.value.password.isEmpty())
        task.editPassword("other"); task.chooseMethod()
        check(task.state.value.password.isEmpty())
    }
    @Test fun rejectionIsNotReportedAsAConnectionFailure() = runBlocking {
        val api = Transport(); val task = task(api)
        task.requestCode(); task.editCode("123456"); api.failure = SignInFailure(SignInProblem.REJECTED)
        task.submitCode()
        check(task.state.value.problem == SignInProblem.REJECTED)
        check(task.state.value.code == "123456" && api.sends == 1)
    }
    @Test fun onlyExplicitSuccessfulResendReplacesChallengeAndClearsCode() = runBlocking {
        var now = 1_000L; val api = Transport(); val task = task(api) { now }
        task.requestCode(); task.editCode("123456")
        task.requestCode(); check(api.sends == 1)
        now += 60_000; api.failure = IOException(); task.requestCode()
        check(task.state.value.code == "123456" && task.state.value.stage == SignInStage.CODE)
        api.failure = null; task.submitCode(); check(api.submittedChallenge == "challenge-1")
        now += 60_000; task.requestCode()
        check(api.sends == 3 && task.state.value.code.isEmpty())
        task.editCode("000999"); task.submitCode(); check(api.submittedChallenge == "challenge-3")
    }
    @Test fun rateLimitKeepsInputAndBlocksRequestsUntilRetryTime() = runBlocking {
        var now = 1_000L; val api = Transport(); val task = task(api) { now }
        task.requestCode(); task.editCode("123456")
        api.failure = SignInFailure(SignInProblem.THROTTLED, 120_000)
        task.submitCode(); check(task.state.value.code == "123456")
        check(task.state.value.blockedUntil == 121_000L)
        task.submitCode(); task.requestCode(); check(api.verifies == 1 && api.sends == 1)
        now += 120_000; api.failure = null; check(task.submitCode() == "session")
    }
    @Test fun closeDropsLateSuccessAndClearsSensitiveInput() = runBlocking {
        val api = Transport(); val task = task(api); task.choosePassword(); task.editPassword("secret")
        api.hold = CompletableDeferred(); var result: String? = "not-completed"
        val pending = launch(start = CoroutineStart.UNDISPATCHED) { result = task.submitPassword() }
        task.close(); api.hold!!.complete(Unit); pending.join()
        check(result == null && task.state.value.password.isEmpty() && task.state.value.code.isEmpty())
        check(task.submitPassword() == null)
    }
    @Test fun changingAccountDiscardsOldChallengeAndLateResult() = runBlocking {
        val api = Transport(); val task = task(api); api.hold = CompletableDeferred()
        val pending = launch(start = CoroutineStart.UNDISPATCHED) { task.requestCode() }
        task.open("other@example.test"); api.hold!!.complete(Unit); pending.join()
        check(task.state.value.email == "other@example.test" && task.state.value.stage == SignInStage.METHODS)
        check(task.state.value.operation == null && task.submitCode() == null)
    }
    @Test fun cancellationPropagatesAndDoesNotBecomeAValidationError() = runBlocking {
        val api = Transport(); val task = task(api); api.failure = CancellationException("cancel")
        try { task.requestCode(); error("Expected coroutine cancellation") } catch (_: CancellationException) { }
        check(task.state.value.operation == null && task.state.value.problem == null)
    }
    @Test fun cancelledPasskeyStaysOnMethodChoiceWithoutSendingEmail() = runBlocking {
        val api = Transport(); val task = task(api); api.failure = SignInFailure(SignInProblem.CANCELLED)
        task.submitPasskey()
        check(task.state.value.stage == SignInStage.METHODS && task.state.value.problem == SignInProblem.CANCELLED)
        check(api.sends == 0 && api.passkeys == 1)
    }
    @Test fun malformedCodeNeverReachesTheServer() = runBlocking {
        val api = Transport(); val task = task(api); task.requestCode(); task.editCode("12")
        check(task.submitCode() == null && api.verifies == 0)
    }
    @Test fun stateAndExceptionsDoNotPrintCredentials() = runBlocking {
        val api = Transport(); val task = task(api); task.choosePassword(); task.editPassword("private-value")
        check(!task.state.value.toString().contains("private-value"))
        task.chooseMethod(); task.requestCode(); task.editCode("001234")
        check(!task.state.value.toString().contains("001234"))
    }
}
