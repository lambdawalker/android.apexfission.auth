package com.apexfission.android.auth.identity.capture

/** Routing only; Android remains the authority for hardware and permission grants. */
internal enum class CameraPermissionRoute { NO_CAMERA, CAMERA, REQUEST, WAIT, RECOVERY }

/** The caller has already shown the disclosure. An attempted request must never auto-repeat. */
internal fun cameraPermissionRoute(
    hasCamera: Boolean,
    granted: Boolean,
    attempted: Boolean,
    inFlight: Boolean,
    foreground: Boolean,
): CameraPermissionRoute = when {
    !hasCamera -> CameraPermissionRoute.NO_CAMERA
    granted -> CameraPermissionRoute.CAMERA
    inFlight -> CameraPermissionRoute.WAIT
    attempted -> CameraPermissionRoute.RECOVERY
    !foreground -> CameraPermissionRoute.WAIT
    else -> CameraPermissionRoute.REQUEST
}
