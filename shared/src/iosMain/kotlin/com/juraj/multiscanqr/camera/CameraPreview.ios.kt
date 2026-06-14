package com.juraj.multiscanqr.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.CoreGraphics.CGRectZero
import platform.QuartzCore.CATransaction
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create
import kotlinx.cinterop.readValue

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CameraPreview(
    isScanning: Boolean,
    onQrDetected: (String) -> Unit,
    modifier: Modifier,
) {
    val currentOnQrDetected by rememberUpdatedState(onQrDetected)
    val currentIsScanning by rememberUpdatedState(isScanning)

    val controller = remember {
        QrCameraController { raw ->
            if (currentIsScanning) currentOnQrDetected(raw)
        }
    }

    DisposableEffect(Unit) {
        controller.start()
        onDispose { controller.stop() }
    }

    UIKitView(
        factory = { CameraPreviewView(controller.session) },
        modifier = modifier,
    )
}

@OptIn(ExperimentalForeignApi::class)
private class CameraPreviewView(session: AVCaptureSession) : UIView(frame = CGRectZero.readValue()) {

    private val previewLayer = AVCaptureVideoPreviewLayer(session = session).apply {
        videoGravity = AVLayerVideoGravityResizeAspectFill
    }

    init {
        layer.addSublayer(previewLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        previewLayer.frame = bounds
        CATransaction.commit()
    }
}

@OptIn(ExperimentalForeignApi::class)
private class QrCameraController(
    private val onQrDetected: (String) -> Unit,
) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {

    val session = AVCaptureSession()
    private val sessionQueue = dispatch_queue_create("multiscanqr.camera.session", null)
    private var configured = false

    fun start() {
        dispatch_async(sessionQueue) {
            if (!configured) {
                configured = configureSession()
            }
            if (configured && !session.running) {
                session.startRunning()
            }
        }
    }

    fun stop() {
        dispatch_async(sessionQueue) {
            if (session.running) session.stopRunning()
        }
    }

    private fun configureSession(): Boolean {
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) ?: return false
        val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null) ?: return false
        if (!session.canAddInput(input)) return false
        session.addInput(input)

        val output = AVCaptureMetadataOutput()
        if (!session.canAddOutput(output)) return false
        session.addOutput(output)
        output.setMetadataObjectsDelegate(this, dispatch_get_main_queue())

        output.metadataObjectTypes = listOf(AVMetadataObjectTypeQRCode)
        return true
    }

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection,
    ) {
        val code = didOutputMetadataObjects.firstOrNull() as? AVMetadataMachineReadableCodeObject
        code?.stringValue?.let(onQrDetected)
    }
}
