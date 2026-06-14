package com.juraj.multiscanqr.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIPasteboard

@Composable
actual fun rememberAppClipboard(): AppClipboard = remember {
    AppClipboard { text -> UIPasteboard.generalPasteboard.string = text }
}
