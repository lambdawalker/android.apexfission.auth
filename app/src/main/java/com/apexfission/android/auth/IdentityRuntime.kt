package com.apexfission.android.auth

import androidx.compose.runtime.Composable
import com.apexfission.android.auth.capture.CapturePolicy

/** Both app variants use the authenticated backend capture flow. */
@Composable
internal fun DirectIdentityCapture(onExit: () -> Unit) = LiveCaptureFlow(onExit)

/** Keeps the policy accepted during onboarding attached to capture. */
@Composable
internal fun AccountIdentityFlow(acceptedPolicy: CapturePolicy?, onExit: () -> Unit) =
    LiveCaptureFlow(onExit, acceptedPolicy)
