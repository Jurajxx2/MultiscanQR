package com.juraj.multiscanqr.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juraj.multiscanqr.data.ScanHistoryRepository
import com.juraj.multiscanqr.domain.ScanContentClassifier
import com.juraj.multiscanqr.domain.ScanContentType
import com.juraj.multiscanqr.domain.ScanResult
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalTime::class)
class ScannerViewModel(
    private val repository: ScanHistoryRepository,
    private val clock: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : ViewModel() {

    private val _state = MutableStateFlow(ScannerUiState())
    val state: StateFlow<ScannerUiState> = _state.asStateFlow()

    // Channel, not SharedFlow: effects must be delivered exactly once and must
    // not be dropped if they are emitted before the collector is attached.
    private val _effects = Channel<ScannerEffect>(Channel.BUFFERED)
    val effects: Flow<ScannerEffect> = _effects.receiveAsFlow()

    private var lastHandledContent: String? = null
    private var lastHandledAtMillis = 0L

    fun onEvent(event: ScannerEvent) {
        when (event) {
            is ScannerEvent.QrDetected -> handleDetection(event.rawValue)
            ScannerEvent.ResultDismissed -> dismissResult()
            ScannerEvent.OpenLinkClicked -> openActiveLink()
            ScannerEvent.CopyClicked -> copyActiveContent()
        }
    }

    private fun handleDetection(rawValue: String) {
        val current = _state.value
        // The camera keeps streaming frames; ignore anything while a result is
        // displayed, and debounce re-detections of the same code right after
        // the sheet is dismissed (the code is usually still in front of the lens).
        if (current.activeResult != null) return
        val now = clock()
        if (rawValue == lastHandledContent && now - lastHandledAtMillis < DUPLICATE_COOLDOWN_MS) {
            return
        }
        lastHandledContent = rawValue
        lastHandledAtMillis = now

        val result = ScanResult(
            id = 0L, // assigned by the database
            content = rawValue,
            type = ScanContentClassifier.classify(rawValue),
            scannedAtEpochMillis = now,
        )
        _state.update { it.copy(isScanning = false, activeResult = result) }
        viewModelScope.launch {
            repository.save(result.content, result.type, result.scannedAtEpochMillis)
        }
    }

    private fun dismissResult() {
        lastHandledAtMillis = clock()
        _state.update { it.copy(isScanning = true, activeResult = null) }
    }

    private fun openActiveLink() {
        val result = _state.value.activeResult ?: return
        if (result.type != ScanContentType.URL) return
        viewModelScope.launch { _effects.send(ScannerEffect.OpenUrl(result.content)) }
    }

    private fun copyActiveContent() {
        val result = _state.value.activeResult ?: return
        viewModelScope.launch { _effects.send(ScannerEffect.CopyToClipboard(result.content)) }
    }

    companion object {
        const val DUPLICATE_COOLDOWN_MS = 2_000L
    }
}
