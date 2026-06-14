package com.juraj.multiscanqr.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberAppClipboard(): AppClipboard {
    val context = LocalContext.current
    return remember(context) {
        AppClipboard { text ->
            val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            manager.setPrimaryClip(ClipData.newPlainText("Scanned QR code", text))
        }
    }
}
