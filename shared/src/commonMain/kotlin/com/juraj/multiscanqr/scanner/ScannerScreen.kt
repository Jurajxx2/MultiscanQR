package com.juraj.multiscanqr.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juraj.multiscanqr.camera.CameraPermissionStatus
import com.juraj.multiscanqr.camera.CameraPreview
import com.juraj.multiscanqr.camera.rememberCameraPermissionController
import com.juraj.multiscanqr.domain.ScanContentType
import com.juraj.multiscanqr.platform.rememberAppClipboard
import multiscanqr.shared.generated.resources.Res
import multiscanqr.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel,
    onOpenHistory: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permissionController = rememberCameraPermissionController()
    val uriHandler = LocalUriHandler.current
    val clipboard = rememberAppClipboard()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ScannerEffect.OpenUrl -> uriHandler.openUri(effect.url)
                is ScannerEffect.CopyToClipboard -> clipboard.copy(effect.text)
            }
        }
    }

    ScannerScreenContent(
        state = state,
        permissionStatus = permissionController.status,
        onEvent = viewModel::onEvent,
        onRequestPermission = permissionController::request,
        onOpenSettings = permissionController::openSettings,
        onOpenHistory = onOpenHistory,
        // The live camera is injected as a slot so this content composable
        // stays previewable without real camera hardware.
        cameraContent = { isScanning, onQrDetected, modifier ->
            CameraPreview(isScanning, onQrDetected, modifier)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScannerScreenContent(
    state: ScannerUiState,
    permissionStatus: CameraPermissionStatus,
    onEvent: (ScannerEvent) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    cameraContent: @Composable (Boolean, (String) -> Unit, Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(Res.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(Res.string.cd_scan_history))
                    }
                },
            )
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            when (permissionStatus) {
                CameraPermissionStatus.Granted -> {
                    cameraContent(
                        state.isScanning,
                        { raw -> onEvent(ScannerEvent.QrDetected(raw)) },
                        Modifier.fillMaxSize(),
                    )
                    ViewfinderOverlay(Modifier.fillMaxSize())
                }

                CameraPermissionStatus.NotDetermined -> PermissionPrompt(
                    text = stringResource(Res.string.permission_camera_rationale),
                    buttonLabel = stringResource(Res.string.permission_grant_camera),
                    onClick = onRequestPermission,
                )

                CameraPermissionStatus.Denied -> PermissionPrompt(
                    text = stringResource(Res.string.permission_camera_denied),
                    buttonLabel = stringResource(Res.string.permission_open_settings),
                    onClick = onOpenSettings,
                )
            }
        }
    }

    state.activeResult?.let { result ->
        ModalBottomSheet(onDismissRequest = { onEvent(ScannerEvent.ResultDismissed) }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(result.type.displayName()) },
                )
                Spacer(Modifier.height(12.dp))
                SelectionContainer {
                    Text(result.content, style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (result.type == ScanContentType.URL) {
                        Button(onClick = { onEvent(ScannerEvent.OpenLinkClicked) }) {
                            Text(stringResource(Res.string.action_open_link))
                        }
                    }
                    OutlinedButton(onClick = { onEvent(ScannerEvent.CopyClicked) }) {
                        Text(stringResource(Res.string.action_copy))
                    }
                    TextButton(onClick = { onEvent(ScannerEvent.ResultDismissed) }) {
                        Text(stringResource(Res.string.action_done))
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ViewfinderOverlay(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(240.dp)
                .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(24.dp)),
        )
        Text(
            text = stringResource(Res.string.scanner_viewfinder_hint),
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun PermissionPrompt(
    text: String,
    buttonLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onClick) { Text(buttonLabel) }
    }
}

@Composable
internal fun ScanContentType.displayName(): String = when (this) {
    ScanContentType.URL -> stringResource(Res.string.scan_type_url)
    ScanContentType.EMAIL -> stringResource(Res.string.scan_type_email)
    ScanContentType.PHONE -> stringResource(Res.string.scan_type_phone)
    ScanContentType.WIFI -> stringResource(Res.string.scan_type_wifi)
    ScanContentType.GEO -> stringResource(Res.string.scan_type_geo)
    ScanContentType.CONTACT -> stringResource(Res.string.scan_type_contact)
    ScanContentType.TEXT -> stringResource(Res.string.scan_type_text)
}
