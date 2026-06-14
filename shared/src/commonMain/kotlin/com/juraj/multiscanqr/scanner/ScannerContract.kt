package com.juraj.multiscanqr.scanner

import androidx.compose.runtime.Immutable
import com.juraj.multiscanqr.domain.ScanResult

@Immutable
data class ScannerUiState(
    val isScanning: Boolean = true,
    val activeResult: ScanResult? = null,
)

sealed interface ScannerEvent {
    data class QrDetected(val rawValue: String) : ScannerEvent
    data object ResultDismissed : ScannerEvent
    data object OpenLinkClicked : ScannerEvent
    data object CopyClicked : ScannerEvent
}

sealed interface ScannerEffect {
    data class OpenUrl(val url: String) : ScannerEffect
    data class CopyToClipboard(val text: String) : ScannerEffect
}
