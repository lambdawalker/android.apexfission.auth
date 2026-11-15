package com.apexfission.android.auth.signin

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test

class PasskeyTaskTest {
    @Test fun disposedOwnerCannotStartQueuedRegistration() = runBlocking {
        val task = PasskeyTask(); var requests = 0
        task.close()
        task.reset()
        check(!task.enroll({ requests++; "options" }, { "credential" }, {}))
        check(!task.retryStatus() && requests == 0)
    }
    @Test fun cancelledProviderKeepsRecoverableStageAndDoesNotSave() = runBlocking {
        val task = PasskeyTask(); var saves = 0
        check(!task.enroll({ "options" }, { throw SignInFailure(SignInProblem.CANCELLED) }, { saves++ }))
        check(task.state.value == PasskeyStage.CANCELLED && saves == 0)
    }
    @Test fun duplicateRequestsCannotOpenAnotherProvider() = runBlocking {
        val task = PasskeyTask(); val hold = CompletableDeferred<Unit>(); var opens = 0; var saves = 0
        val pending = launch(start = CoroutineStart.UNDISPATCHED) {
            task.enroll({ "options" }, { opens++; hold.await(); "credential" }, { saves++ })
        }
        check(task.state.value == PasskeyStage.OPENING)
        check(!task.enroll({ "options" }, { opens++; "credential" }, { saves++ }))
        hold.complete(Unit); pending.join()
        check(opens == 1 && saves == 1 && task.state.value == PasskeyStage.CHECKING)
    }
    @Test fun statusRetryDoesNotReopenProviderAfterAcknowledgedSave() = runBlocking {
        val task = PasskeyTask(); var opens = 0
        check(task.enroll({ "options" }, { opens++; "credential" }, {}))
        task.statusFailed(); check(task.state.value == PasskeyStage.STATUS_FAILED)
        check(!task.enroll({ "options" }, { opens++; "credential" }, {}))
        check(task.retryStatus())
        check(!task.retryStatus())
        check(task.state.value == PasskeyStage.CHECKING && opens == 1)
    }
    @Test fun abandonedProviderResultCannotSaveCredential() = runBlocking {
        val task = PasskeyTask(); val hold = CompletableDeferred<Unit>(); var saves = 0
        val pending = launch(start = CoroutineStart.UNDISPATCHED) {
            task.enroll({ "options" }, { hold.await(); "credential" }, { saves++ })
        }
        task.reset(); hold.complete(Unit); pending.join()
        check(saves == 0 && task.state.value == PasskeyStage.READY)
    }
    @Test fun unsupportedAndReauthenticationHaveDistinctActions() = runBlocking {
        for ((problem, stage) in listOf(SignInProblem.UNSUPPORTED to PasskeyStage.UNSUPPORTED,
                SignInProblem.SESSION_EXPIRED to PasskeyStage.REAUTH_REQUIRED)) {
            val task = PasskeyTask()
            check(!task.enroll({ throw SignInFailure(problem) }, { "credential" }, {}))
            check(task.state.value == stage)
        }
    }
}
