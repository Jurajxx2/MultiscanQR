package com.juraj.multiscanqr.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun rememberCameraPermissionController(): CameraPermissionController =
    remember { IosCameraPermissionController() }

private class IosCameraPermissionController : CameraPermissionController {

    override var status by mutableStateOf(readSystemStatus())
        private set

    override fun request() {
        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
            dispatch_async(dispatch_get_main_queue()) {
                status = if (granted) {
                    CameraPermissionStatus.Granted
                } else {
                    CameraPermissionStatus.Denied
                }
            }
        }
    }

    override fun openSettings() {
        val settingsUrl = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(
            settingsUrl,
            options = emptyMap<Any?, Any>(),
            completionHandler = null,
        )
    }

    private fun readSystemStatus(): CameraPermissionStatus =
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> CameraPermissionStatus.Granted
            AVAuthorizationStatusNotDetermined -> CameraPermissionStatus.NotDetermined
            else -> CameraPermissionStatus.Denied
        }
}
