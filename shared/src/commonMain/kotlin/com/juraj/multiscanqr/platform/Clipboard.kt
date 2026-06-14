package com.juraj.multiscanqr.platform

import androidx.compose.runtime.Composable

fun interface AppClipboard {
    fun copy(text: String)
}

@Composable
expect fun rememberAppClipboard(): AppClipboard
