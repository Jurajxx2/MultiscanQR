package com.juraj.multiscanqr.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juraj.multiscanqr.data.ScanHistoryRepository
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: ScanHistoryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    private val _effects = Channel<HistoryEffect>(Channel.BUFFERED)
    val effects: Flow<HistoryEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.observeHistory().collect { items ->
                _state.update {
                    it.copy(isLoading = false, items = items.toImmutableList())
                }
            }
        }
    }

    fun onEvent(event: HistoryEvent) {
        when (event) {
            is HistoryEvent.Delete -> viewModelScope.launch { repository.delete(event.id) }
            HistoryEvent.ClearAll -> viewModelScope.launch { repository.clearAll() }
            is HistoryEvent.CopyItem -> viewModelScope.launch {
                _effects.send(HistoryEffect.CopyToClipboard(event.content))
            }
        }
    }
}
