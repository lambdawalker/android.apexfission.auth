package com.apexfission.android.auth.email

import com.apexfission.android.auth.library.R

import androidx.activity.compose.BackHandler
import com.apexfission.android.auth.capture.CaptureApi
import com.apexfission.android.auth.capture.CapturePolicy
import com.apexfission.android.auth.discovery.*
import com.apexfission.android.auth.passkey.AccountStatus
import com.apexfission.android.auth.passkey.SignInError
import com.apexfission.android.auth.ui.onboarding.password.PasswordField
import com.apexfission.android.auth.ui.onboarding.password.PasswordScreen
import com.apexfission.android.auth.ui.onboarding.common.OnboardingFrame
import com.apexfission.android.auth.ui.onboarding.common.BodyText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import com.apexfission.android.auth.signin.*
import com.apexfission.android.auth.ui.onboarding.signin.SignInTaskScreen
import com.apexfission.android.auth.ui.onboarding.passkey.PasskeyTaskScreen
import com.apexfission.android.auth.ui.onboarding.account.AccountTaskScreen
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.CancellationException
import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.apexfission.android.auth.passkey.ReturnAuthApi
import androidx.compose.runtime.mutableStateOf
import com.apexfission.android.auth.passkey.AndroidPasskeyManager
import com.apexfission.android.auth.passkey.PasskeyRegistrationApi
import com.apexfission.android.auth.ui.onboarding.id.IdentityStartScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import com.apexfission.android.auth.ui.onboarding.common.LoadingTask
import com.apexfission.android.auth.ui.onboarding.common.OnboardingLoadingScreen
import com.apexfission.android.auth.ui.onboarding.email.EmailAttemptLimitScreen
import com.apexfission.android.auth.ui.onboarding.email.EmailLinkUnusableScreen
import com.apexfission.android.auth.ui.onboarding.email.EmailStartScreen
import com.apexfission.android.auth.ui.onboarding.email.EmailWaitScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

private fun emailFromSession(session: AuthSession?): String? = runCatching {
    val payload = session!!.idToken.split('.')[1]
    val bytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    EmailHttpClient.json.parseToJsonElement(bytes.toString(Charsets.UTF_8)).jsonObject["email"]?.jsonPrimitive?.content
}.getOrNull()

/** Rejects URLs from other origins even if another app explicitly sends an intent to this activity. */
fun parseVerificationLink(uri: Uri?, expectedHost: String): VerificationLink? {
    if (uri?.scheme != "https" || uri.host != expectedHost || uri.path != "/verify-email" || (uri.port != -1 && uri.port != 443)) return null
    return VerificationLink(uri.getQueryParameter("request_id").orEmpty(), uri.getQueryParameter("b").orEmpty())
}

/**
 * Screen that hosts the complete email onboarding and sign-in flow.
 * Orchestrates password, passkey, and ID capture steps based on the provided onboarding policy.
 */
@Composable
fun EmailOnboardingHost(
    apiBaseUrl: String,
    link: VerificationLink?,
    onLinkConsumed: () -> Unit,
    onPasskeyDeferred: () -> Unit,
    onboarding: OnboardingPolicy? = null,
    onRetryConfiguration: () -> Unit = {},
    // Keep identityContent as the final parameter for existing trailing-lambda clients.
    identityReviewContent: (@Composable (Boolean, CapturePolicy?, () -> Unit) -> Unit)? = null,
    identityContent: @Composable (Boolean, CapturePolicy?, () -> Unit) -> Unit = { _, _, exit ->
        com.apexfission.android.auth.identity.IdentityIntegrationUnavailable(exit)
    },
) {
    val activityContext: Context = LocalContext.current
    val resources = LocalResources.current
    val context: Context = activityContext.applicationContext
    val storage = remember(context) { SecureAuthStorage(context) }
    val client = remember(apiBaseUrl) { EmailHttpClient.create() }
    DisposableEffect(client) { onDispose { client.close() } }
    val controller = remember(apiBaseUrl, storage, client) {
        EmailFlowController(EmailVerificationApi(apiBaseUrl, client), storage)
    }
    val scope = rememberCoroutineScope()
    // Transient only: never persist email codes in saved instance state.
    var verificationCode by remember(apiBaseUrl) { mutableStateOf("") }
    LaunchedEffect(controller.lastEmailAtMillis) { verificationCode = "" }
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val passkeyApi = remember(apiBaseUrl, client) { PasskeyRegistrationApi(apiBaseUrl, client) }
    val passkeyManager = remember(activityContext) { AndroidPasskeyManager(activityContext) }
    val returnApi = remember(apiBaseUrl, client) { ReturnAuthApi(apiBaseUrl, client) }
    val returnTask = remember(returnApi, passkeyManager) {
        SignInTask(ReturningSignInTransport(returnApi, passkeyManager::signIn))
    }
    val returnState by returnTask.state.collectAsState()
    val passkeyTask = remember(apiBaseUrl) { PasskeyTask() }
    val passkeyStage by passkeyTask.state.collectAsState()
    var passkeyScreen by remember(apiBaseUrl) { mutableStateOf("refresh") }
    var reviewSavedDetailsRequested by remember(apiBaseUrl) { mutableStateOf(false) }
    val capturePolicyPrefetch = remember(apiBaseUrl, client, scope) {
        CapturePolicyPrefetch(scope) { token ->
            // This transport only reads policy. LiveCaptureFlow owns the image transport.
            CaptureApi(apiBaseUrl, client, client) { token }.policy()
        }
    }
    DisposableEffect(capturePolicyPrefetch) { onDispose { capturePolicyPrefetch.clear() } }
    var capturePolicy by remember(apiBaseUrl) { mutableStateOf<CapturePolicy?>(null) }
    var acceptedCapturePolicy by remember(apiBaseUrl) { mutableStateOf<CapturePolicy?>(null) }
    var capturePolicyLoading by remember(apiBaseUrl) { mutableStateOf(false) }
    var capturePolicyError by remember(apiBaseUrl) { mutableStateOf<String?>(null) }
    var capturePolicyRefresh by remember(apiBaseUrl) { mutableLongStateOf(0L) }
    LaunchedEffect(passkeyScreen, apiBaseUrl, capturePolicyRefresh) {
        if (passkeyScreen != "identity") return@LaunchedEffect
        capturePolicy = null
        acceptedCapturePolicy = null
        capturePolicyLoading = true
        capturePolicyError = null
        try {
            val token = requireNotNull(storage.session()).accessToken
            val policy = capturePolicyPrefetch.load(token)
            if (storage.session()?.accessToken == token) capturePolicy = policy
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            capturePolicyError = resources.getString(R.string.auth_we_could_not_load_the_capture_information_try_again_or_return_to)
        } finally { capturePolicyLoading = false }
    }
    var status by remember(apiBaseUrl) { mutableStateOf<AccountStatus?>(null) }
    // Intentionally not rememberSaveable: never serialize credentials into saved state.
    var password by remember(apiBaseUrl) { mutableStateOf("") }
    var passwordEmail by remember(apiBaseUrl) { mutableStateOf<String?>(null) }
    var passwordRequestId by remember(apiBaseUrl) { mutableStateOf<String?>(null) }
    var passwordError by remember(apiBaseUrl) { mutableStateOf<String?>(null) }
    var passwordBusy by remember(apiBaseUrl) { mutableStateOf(false) }
    var passwordSaved by remember(apiBaseUrl) { mutableStateOf(false) }
    var signupEmail by remember(apiBaseUrl) { mutableStateOf(storage.pending()?.email.orEmpty()) }
    var returnMode by remember(apiBaseUrl) { mutableStateOf("") }
    var returnEmail by remember(apiBaseUrl) { mutableStateOf(storage.email() ?: storage.pending()?.email ?: emailFromSession(storage.session()).orEmpty()) }
    var returnError by remember(apiBaseUrl) { mutableStateOf<String?>(null) }
    var authenticated by remember(apiBaseUrl) { mutableStateOf(false) }
    var restoring by remember(apiBaseUrl) { mutableStateOf(storage.session() != null) }
    val currentPolicy by rememberUpdatedState(onboarding)
    var hostGeneration by remember(apiBaseUrl) { mutableLongStateOf(0L) }
    fun clearPassword() { password = ""; passwordEmail = null; passwordRequestId = null; passwordSaved = false }
    fun showSignIn(email: String = returnEmail, notice: String? = null) {
        hostGeneration++
        passwordBusy = false
        clearPassword()
        passkeyTask.reset()
        returnEmail = email
        returnError = notice
        returnTask.open(email)
        authenticated = false
        returnMode = "choice"
    }
    DisposableEffect(returnTask, passkeyTask) {
        onDispose { hostGeneration++; returnTask.close(); passkeyTask.close(); clearPassword() }
    }
    val exit = {
        hostGeneration++
        returnTask.close()
        passkeyTask.close()
        capturePolicyPrefetch.clear()
        clearPassword()
        onPasskeyDeferred()
    }
    LaunchedEffect(controller, link) {
        if (link != null) {
            hostGeneration++
            returnTask.close()
            passkeyTask.reset()
            capturePolicyPrefetch.clear()
            if (link.requestId != passwordRequestId) clearPassword()
            status = null
            authenticated = false
            returnMode = ""
            onLinkConsumed()
            scope.launch { controller.openLink(link) }
        }
    }
    BackHandler { exit() }
    fun route() {
        passkeyScreen = when (currentPolicy?.nextRequired(status ?: AccountStatus())) {
            OnboardingStep.PASSWORD -> "password"
            OnboardingStep.PASSKEY -> "start"
            OnboardingStep.ID_CAPTURE -> "identity"
            OnboardingStep.COMPLETE -> "welcome"
            else -> "refresh"
        }
    }
    val prefetchCapturePolicy: (String) -> Unit = { token ->
        if (currentPolicy?.idCapture?.let { it != Requirement.DISABLED } == true && status?.idCaptured != true) {
            capturePolicyPrefetch.start(token)
        }
    }
    suspend fun refreshStatus(keepScreen: Boolean = false): Boolean {
        val generation = hostGeneration
        if (!keepScreen) passkeyScreen = "refreshing"
        try {
            var session = requireNotNull(storage.session())
            prefetchCapturePolicy(session.accessToken)
            val loaded = try { returnApi.status(session.accessToken) }
            catch (expired: SignInError) {
                if (!expired.expired || session.refreshToken.isBlank()) throw expired
                val refreshed = returnApi.refresh(session.refreshToken).let {
                    if (it.refreshToken.isBlank()) it.copy(refreshToken = session.refreshToken) else it
                }
                if (generation != hostGeneration || storage.session()?.accessToken != session.accessToken) return false
                storage.saveSession(refreshed)
                session = refreshed
                prefetchCapturePolicy(refreshed.accessToken)
                returnApi.status(refreshed.accessToken)
            }
            if (generation != hostGeneration || storage.session()?.accessToken != session.accessToken) return false
            status = loaded
            route()
            return true
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: SignInError) {
            if (generation != hostGeneration) return false
            if (error.expired) {
                status = null
                capturePolicyPrefetch.clear()
                storage.clearSession()
                showSignIn(notice = resources.getString(R.string.auth_sign_in_to_continue_setup))
            }
            if (!keepScreen) passkeyScreen = "refresh"
        } catch (_: Exception) {
            if (generation == hostGeneration && !keepScreen) passkeyScreen = "refresh"
        }
        return false
    }
    val skip: (OnboardingStep) -> Unit = { step ->
        val requirement = when (step) {
            OnboardingStep.PASSWORD -> onboarding?.password
            OnboardingStep.PASSKEY -> onboarding?.passkey
            OnboardingStep.ID_CAPTURE -> onboarding?.idCapture
            else -> null
        }
        if (requirement == Requirement.OPTIONAL) {
            passkeyTask.reset()
            if (step == OnboardingStep.PASSWORD) clearPassword()
            scope.launch { refreshStatus() }
        }
    }
    LaunchedEffect(onboarding) {
        if (onboarding == null || onboarding.password == Requirement.DISABLED) clearPassword()
        if (status != null) refreshStatus()
    }
    val saveSignedIn: suspend (AuthSession) -> Unit = { result ->
        val address = returnTask.state.value.email.trim()
        hostGeneration++
        returnTask.close()
        clearPassword()
        storage.saveSession(result)
        storage.saveEmail(address)
        returnEmail = address
        authenticated = true
        returnMode = ""
        refreshStatus()
    }
    LaunchedEffect(controller, controller.screen) {
        if (controller.screen == EmailScreen.Verified && !authenticated) {
            restoring = true
            val stored = storage.session()
            val generation = hostGeneration
            try {
                if (stored != null) {
                    val fresh = if (stored.refreshToken.isNotBlank()) returnApi.refresh(stored.refreshToken)
                        .let { if (it.refreshToken.isBlank()) it.copy(refreshToken = stored.refreshToken) else it } else stored
                    if (generation != hostGeneration || storage.session()?.accessToken != stored.accessToken) return@LaunchedEffect
                    storage.saveSession(fresh)
                    authenticated = true
                    refreshStatus()
                    val policy = currentPolicy
                    val matches = pendingPasswordMatches(passwordEmail, passwordRequestId,
                        emailFromSession(fresh), controller.verifiedRequestId)
                    if (!matches || status?.passwordRegistered == true || policy?.passwordPlacement != PasswordPlacement.WITH_EMAIL) clearPassword()
                    if (matches && password.isNotEmpty() && policy?.passwordPlacement == PasswordPlacement.WITH_EMAIL &&
                        policy.password != Requirement.DISABLED && status != null && status?.passwordRegistered == false &&
                        policy.passwordPolicy.problems(password).isEmpty()) {
                        val entered = password
                        passwordBusy = true
                        passkeyScreen = "password"
                        try {
                            check(returnApi.setPassword(requireNotNull(storage.session()).accessToken, entered).passwordRegistered)
                            if (generation == hostGeneration) {
                                clearPassword()
                                passwordSaved = true
                                refreshStatus(keepScreen = true)
                            }
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) {
                            if (generation == hostGeneration) passwordError = resources.getString(R.string.auth_task_request_failed)
                        } finally { if (generation == hostGeneration) passwordBusy = false }
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (generation == hostGeneration) {
                    clearPassword()
                    capturePolicyPrefetch.clear()
                    status = null
                    if (error is SignInError && error.expired) {
                        storage.clearSession()
                        showSignIn(notice = resources.getString(R.string.auth_sign_in_to_continue))
                    } else {
                        // A connection/provider outage is not evidence that the saved session was revoked.
                        authenticated = stored != null
                        passkeyScreen = "refresh"
                    }
                }
            } finally { restoring = false }
        }
    }
    val createPasskey: () -> Unit = {
        if (currentPolicy != null && currentPolicy?.passkey != Requirement.DISABLED) {
            scope.launch {
                val token = storage.session()?.accessToken
                val generation = hostGeneration
                val acknowledged = passkeyTask.enroll(
                    options = {
                        if (token.isNullOrBlank()) throw SignInFailure(SignInProblem.SESSION_EXPIRED)
                        authOperation { passkeyApi.options(token) }
                    },
                    create = { options -> authOperation { passkeyManager.create(options) } },
                    save = { credential -> authOperation { passkeyApi.complete(requireNotNull(token), credential) } },
                )
                if (acknowledged && generation == hostGeneration) {
                    val loaded = refreshStatus(keepScreen = true)
                    if (generation == hostGeneration && (!loaded || status?.passkeyRegistered != true)) {
                        passkeyScreen = "start"
                        passkeyTask.statusFailed()
                    }
                }
            }
        }
    }
    val screen = if (authenticated) EmailScreen.Verified else controller.screen
    LaunchedEffect(screen) {
        if (screen == EmailScreen.Recovery) showSignIn(notice = resources.getString(R.string.auth_sign_in_to_continue_setup))
        if (screen == EmailScreen.Verified) verificationCode = ""
        while (screen is EmailScreen.Wait || screen is EmailScreen.Code || screen is EmailScreen.Limit || screen is EmailScreen.Unusable) {
            clock = System.currentTimeMillis()
            delay(1_000)
        }
    }
    LaunchedEffect(returnMode) {
        while (returnMode.isNotEmpty()) { clock = System.currentTimeMillis(); delay(1_000) }
    }
    val canResend = clock >= controller.lastEmailAtMillis + 60_000
    val resend = { scope.launch { controller.resend() }; Unit }
    val changeEmail = {
        verificationCode = ""
        capturePolicyPrefetch.clear()
        clearPassword()
        passwordError = null
        controller.changeEmail()
    }
    val savePassword: () -> Unit = {
        if (!passwordBusy && !passwordSaved && onboarding != null && onboarding.password != Requirement.DISABLED && onboarding.passwordPolicy.problems(password).isEmpty()) {
            val entered = password
            val generation = hostGeneration
            passwordError = null
            passwordBusy = true
            scope.launch {
                try {
                    check(returnApi.setPassword(requireNotNull(storage.session()).accessToken, entered).passwordRegistered)
                    if (generation == hostGeneration) {
                        clearPassword()
                        passwordSaved = true
                        refreshStatus(keepScreen = true)
                    }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: SignInError) {
                    if (generation == hostGeneration) {
                        if (error.expired) showSignIn(notice = resources.getString(R.string.auth_sign_in_to_continue_setup))
                        else passwordError = resources.getString(R.string.auth_task_request_failed)
                    }
                } catch (_: Exception) {
                    if (generation == hostGeneration) passwordError = resources.getString(R.string.auth_task_request_failed)
                } finally { if (generation == hostGeneration) passwordBusy = false }
            }
        }
    }
    if (returnMode == "choice") {
        SignInTaskScreen(returnState, clock, returnError,
            onEmail = returnTask::editEmail, onCode = returnTask::editCode, onPassword = returnTask::editPassword,
            onSubmit = { scope.launch {
                val result = when (returnTask.state.value.stage) {
                    SignInStage.METHODS -> returnTask.submitPasskey()
                    SignInStage.CODE -> returnTask.submitCode()
                    SignInStage.PASSWORD -> returnTask.submitPassword()
                }
                if (result != null) saveSignedIn(result)
            } },
            onSendCode = { scope.launch { returnTask.requestCode() } },
            onChoosePassword = returnTask::choosePassword, onChooseMethod = returnTask::chooseMethod,
            onBack = {
                signupEmail = returnTask.state.value.email
                returnEmail = signupEmail
                returnTask.close()
                returnMode = ""
                returnError = null
            })
    } else if (screen == EmailScreen.Verified && restoring) {
        OnboardingLoadingScreen(LoadingTask.SIGN_IN, onBack = exit)
    } else if (onboarding == null) {
        OnboardingFrame(resources.getString(R.string.auth_app_settings_unavailable), Icons.Default.Lock, resources.getString(R.string.auth_setup), exit,
            primaryLabel = resources.getString(R.string.auth_retry_settings), onPrimary = onRetryConfiguration,
            secondaryLabel = resources.getString(R.string.auth_sign_in_to_an_existing_account), onSecondary = { showSignIn() }) {
            BodyText(resources.getString(R.string.auth_connect_to_load_current_setup_requirements_before_continuing))
        }
    } else when (screen) {
        is EmailScreen.Start -> EmailStartScreen(signupEmail, { signupEmail = it; clearPassword() }, exit, { address ->
            returnEmail = address
            passwordEmail = address
            scope.launch {
                controller.start(address)
                passwordRequestId = (controller.screen as? EmailScreen.Wait)?.requestId
                if (passwordRequestId == null) clearPassword()
            }
        }, screen.error,
            onExistingAccount = { showSignIn(signupEmail) },
            passwordValid = onboarding.passwordPlacement != PasswordPlacement.WITH_EMAIL ||
                (password.isEmpty() && onboarding.password == Requirement.OPTIONAL) || onboarding.passwordPolicy.problems(password).isEmpty(),
            passwordContent = if (onboarding.passwordPlacement == PasswordPlacement.WITH_EMAIL) {
                { PasswordField(password, { password = it }, onboarding.passwordPolicy, onboarding.password == Requirement.OPTIONAL) }
            } else null)
        is EmailScreen.Wait -> EmailWaitScreen(
            email = screen.email,
            code = verificationCode,
            onCodeChange = { verificationCode = it },
            onVerify = { scope.launch { controller.confirmCode(verificationCode) } },
            onBack = changeEmail,
            onResend = resend,
            onChangeEmail = changeEmail,
            canResend = canResend,
            resendHelp = screen.help ?: if (canResend) null else resources.getString(R.string.auth_you_can_request_another_email_in_a_minute),
            onSignIn = { showSignIn(screen.email) },
        )
        is EmailScreen.Code -> EmailWaitScreen(
            email = screen.email,
            code = verificationCode,
            onCodeChange = { verificationCode = it },
            onVerify = { scope.launch { controller.confirmCode(verificationCode) } },
            onBack = changeEmail,
            onResend = resend,
            onChangeEmail = changeEmail,
            error = screen.error,
            attemptsRemaining = screen.attemptsRemaining,
            autoFocus = true,
            canResend = canResend,
            resendHelp = if (canResend) null else resources.getString(R.string.auth_you_can_request_another_email_in_a_minute),
        )
        is EmailScreen.Limit -> EmailAttemptLimitScreen(
            screen.email, changeEmail, resend, changeEmail, canRequestEmail = canResend,
            retryAfter = if (canResend) null else resources.getString(R.string.auth_wait_a_minute_before_requesting_another_email),
        )
        is EmailScreen.Unusable -> EmailLinkUnusableScreen(
            screen.email, changeEmail, resend, changeEmail, canRequestEmail = screen.requestId != null && canResend,
        )
        EmailScreen.Recovery -> Unit // The effect opens sign-in with recovery context; no extra confirmation button.
        EmailScreen.Verified -> when (passkeyScreen) {
            "refreshing" -> OnboardingLoadingScreen(LoadingTask.SIGN_IN, onBack = exit)
            "password" -> PasswordScreen(password, { password = it }, savePassword, exit, passwordError,
                onboarding.passwordPolicy, onboarding.password == Requirement.OPTIONAL, { skip(OnboardingStep.PASSWORD) },
                busyMessage = if (passwordBusy) resources.getString(R.string.auth_task_saving_password) else null,
                saved = passwordSaved, onCheckStatus = { scope.launch { refreshStatus(keepScreen = true) } })
            "identity" -> IdentityStartScreen(exit, {
                capturePolicy?.let {
                    acceptedCapturePolicy = it
                    reviewSavedDetailsRequested = false
                    passkeyScreen = "identity_flow"
                }
            }, { skip(OnboardingStep.ID_CAPTURE) },
                passkeyAdded = status?.passkeyRegistered == true, required = onboarding.idCapture == Requirement.REQUIRED,
                capturePolicy = capturePolicy, policyLoading = capturePolicyLoading, policyError = capturePolicyError,
                requireDisclosure = true, onRetryPolicy = { capturePolicyPrefetch.clear(); capturePolicyRefresh++ })
            "identity_flow" -> {
                val renderIdentity = if (reviewSavedDetailsRequested) identityReviewContent ?: identityContent else identityContent
                renderIdentity(status?.passkeyRegistered == true, acceptedCapturePolicy) {
                    reviewSavedDetailsRequested = false
                    // Both successful capture and cancellation trigger server-authoritative routing.
                    if (storage.session() == null) {
                        capturePolicyPrefetch.clear()
                        clearPassword()
                        status = null
                        authenticated = false
                        showSignIn(notice = resources.getString(R.string.auth_sign_in_to_resume_your_capture))
                    } else {
                        passkeyScreen = "refreshing"
                        scope.launch { refreshStatus() }
                    }
                }
            }
            "welcome" -> AccountTaskScreen(status ?: AccountStatus(), onboarding.optionalAccountSteps(status ?: AccountStatus()),
                onContinue = exit,
                onAdd = { step ->
                    if (step in onboarding.optionalAccountSteps(status ?: AccountStatus())) {
                        clearPassword()
                        passkeyTask.reset()
                        acceptedCapturePolicy = null
                        passkeyScreen = when (step) {
                            OnboardingStep.PASSKEY -> "start"
                            OnboardingStep.PASSWORD -> "password"
                            else -> "identity"
                        }
                    }
                },
                onReviewId = { acceptedCapturePolicy = null; reviewSavedDetailsRequested = true; passkeyScreen = "identity_flow" },
                onRefresh = { scope.launch { refreshStatus() } })
            "start" -> PasskeyTaskScreen(passkeyStage, onboarding.passkey == Requirement.REQUIRED,
                onBack = exit, onCreate = createPasskey,
                onRetryStatus = { if (passkeyTask.retryStatus()) scope.launch {
                    val generation = hostGeneration
                    val loaded = refreshStatus(keepScreen = true)
                    if (generation == hostGeneration && (!loaded || status?.passkeyRegistered != true)) {
                        passkeyScreen = "start"
                        passkeyTask.statusFailed()
                    }
                } },
                onSignIn = { showSignIn(notice = resources.getString(R.string.auth_sign_in_to_continue_setup)) },
                onLater = { skip(OnboardingStep.PASSKEY) })
            else -> OnboardingFrame(resources.getString(R.string.auth_check_account_setup), Icons.Default.Lock, resources.getString(R.string.auth_account), exit,
                primaryLabel = resources.getString(R.string.auth_retry_account_status), onPrimary = { scope.launch { refreshStatus() } },
                secondaryLabel = resources.getString(R.string.auth_reload_settings), onSecondary = onRetryConfiguration) {
                BodyText(resources.getString(R.string.auth_we_could_not_confirm_that_setup_is_complete_retry_to_get_the_late))
            }
        }
        is EmailScreen.Loading -> {
            val previous = screen.verification
            if (previous is EmailScreen.Wait || previous is EmailScreen.Code) {
                val email = when (previous) {
                    is EmailScreen.Wait -> previous.email
                    is EmailScreen.Code -> previous.email
                    else -> ""
                }
                EmailWaitScreen(
                    email = email,
                    code = verificationCode,
                    onCodeChange = { verificationCode = it },
                    onVerify = {},
                    onBack = {},
                    onResend = {},
                    onChangeEmail = {},
                    canResend = false,
                    error = (previous as? EmailScreen.Code)?.error,
                    attemptsRemaining = (previous as? EmailScreen.Code)?.attemptsRemaining,
                    busyMessage = if (screen.task == LoadingStep.RESEND_EMAIL) {
                        resources.getString(R.string.auth_resend_email)
                    } else {
                        resources.getString(R.string.auth_verifying_your_email)
                    },
                )
            } else {
                OnboardingLoadingScreen(
                    when (screen.task) {
                        LoadingStep.REQUEST_EMAIL -> LoadingTask.REQUEST_EMAIL
                        LoadingStep.CONFIRM_EMAIL -> LoadingTask.CONFIRM_EMAIL
                        LoadingStep.RESEND_EMAIL -> LoadingTask.RESEND_EMAIL
                    }, onBack = {},
                )
            }
        }
    }
}
