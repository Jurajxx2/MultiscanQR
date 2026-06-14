package com.juraj.multiscanqr.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juraj.multiscanqr.domain.RelativeTimeFormatter
import com.juraj.multiscanqr.domain.ScanResult
import com.juraj.multiscanqr.platform.rememberAppClipboard
import com.juraj.multiscanqr.scanner.displayName
import multiscanqr.shared.generated.resources.Res
import multiscanqr.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboard = rememberAppClipboard()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HistoryEffect.CopyToClipboard -> clipboard.copy(effect.text)
            }
        }
    }

    HistoryScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
internal fun HistoryScreenContent(
    state: HistoryUiState,
    onEvent: (HistoryEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(Res.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.cd_navigate_back))
                    }
                },
                actions = {
                    if (state.items.isNotEmpty()) {
                        IconButton(onClick = { onEvent(HistoryEvent.ClearAll) }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.cd_clear_history))
                        }
                    }
                },
            )
        },
    ) { contentPadding ->
        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize().padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            state.items.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(Res.string.history_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> {
                val nowMillis = Clock.System.now().toEpochMilliseconds()
                LazyColumn(Modifier.fillMaxSize().padding(contentPadding)) {
                    items(items = state.items, key = { it.id }) { item ->
                        HistoryRow(
                            item = item,
                            nowMillis = nowMillis,
                            onCopy = { onEvent(HistoryEvent.CopyItem(item.content)) },
                            onDelete = { onEvent(HistoryEvent.Delete(item.id)) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    item: ScanResult,
    nowMillis: Long,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier.fillMaxWidth().clickable(onClick = onCopy),
        headlineContent = {
            Text(item.content, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            val typeName = item.type.displayName()
            Text(
                stringResource(
                    Res.string.history_item_meta,
                    typeName,
                    RelativeTimeFormatter.format(nowMillis, item.scannedAtEpochMillis),
                )
            )
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.cd_delete_entry))
            }
        },
    )
}
