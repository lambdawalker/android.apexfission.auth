package com.apexfission.android.auth.identity.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraPermissionRouteTest {
    private fun route(hasCamera: Boolean = true, granted: Boolean = false, attempted: Boolean = false,
                      inFlight: Boolean = false, foreground: Boolean = true) =
        cameraPermissionRoute(hasCamera, granted, attempted, inFlight, foreground)

    @Test fun acceptedDisclosureGoesDirectlyToNativeRequest() {
        assertEquals(CameraPermissionRoute.REQUEST, route())
    }
    @Test fun existingGrantBypassesPermissionInteraction() {
        assertEquals(CameraPermissionRoute.CAMERA, route(granted = true))
    }
    @Test fun anOutstandingRequestWaitsInsteadOfLaunchingAgain() {
        assertEquals(CameraPermissionRoute.WAIT, route(attempted = true, inFlight = true))
    }
    @Test fun deniedPermissionOffersRecoveryWithoutAutomaticRetry() {
        assertEquals(CameraPermissionRoute.RECOVERY, route(attempted = true))
    }
    @Test fun restoredAttemptWithNoCallbackDoesNotRelaunchOrWaitForever() {
        assertEquals(CameraPermissionRoute.RECOVERY, route(attempted = true, inFlight = false))
    }
    @Test fun backgroundEntryWaitsForForegroundBeforePrompting() {
        assertEquals(CameraPermissionRoute.WAIT, route(foreground = false))
        assertEquals(CameraPermissionRoute.REQUEST, route(foreground = true))
    }
    @Test fun grantedSettingsResultOpensCameraButDeniedSettingsResultDoesNot() {
        assertEquals(CameraPermissionRoute.CAMERA, route(granted = true, attempted = true))
        assertEquals(CameraPermissionRoute.RECOVERY, route(granted = false, attempted = true))
    }
    @Test fun noCameraKeepsTheExistingUnavailableFallback() {
        assertEquals(CameraPermissionRoute.NO_CAMERA, route(hasCamera = false))
    }
    @Test fun allStateCombinationsPreserveGrantAndSingleRequestBoundaries() {
        for (camera in listOf(false, true)) for (granted in listOf(false, true))
        for (attempted in listOf(false, true)) for (pending in listOf(false, true))
        for (foreground in listOf(false, true)) {
            val result = route(camera, granted, attempted, pending, foreground)
            assertEquals(camera && granted, result == CameraPermissionRoute.CAMERA)
            assertTrue(result != CameraPermissionRoute.REQUEST || (camera && !granted && !attempted && !pending && foreground))
            assertTrue(camera || result == CameraPermissionRoute.NO_CAMERA)
        }
    }
}
