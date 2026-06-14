package com.juraj.multiscanqr.history

import androidx.compose.runtime.Immutable
import com.juraj.multiscanqr.domain.ScanResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class HistoryUiState(
    val isLoading: Boolean = true,
    val items: ImmutableList<ScanResult> = persistentListOf(),
)

sealed interface HistoryEvent {
    data class Delete(val id: Long) : HistoryEvent
    data object ClearAll : HistoryEvent
    data class CopyItem(val content: String) : HistoryEvent
}

sealed interface HistoryEffect {
    data class CopyToClipboard(val text: String) : HistoryEffect
}
