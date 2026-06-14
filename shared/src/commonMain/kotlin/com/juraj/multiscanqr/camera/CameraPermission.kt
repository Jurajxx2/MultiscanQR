package com.juraj.multiscanqr.camera

import androidx.compose.runtime.Composable

enum class CameraPermissionStatus {
    NotDetermined,
    Granted,
    Denied,
}

interface CameraPermissionController {
    val status: CameraPermissionStatus
    fun request()
    fun openSettings()
}

@Composable
expect fun rememberCameraPermissionController(): CameraPermissionController
