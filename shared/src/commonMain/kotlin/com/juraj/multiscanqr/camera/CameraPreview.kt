package com.juraj.multiscanqr.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun CameraPreview(
    isScanning: Boolean,
    onQrDetected: (String) -> Unit,
    modifier: Modifier = Modifier,
)
