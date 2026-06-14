package com.juraj.multiscanqr.domain

import androidx.compose.runtime.Immutable

@Immutable
data class ScanResult(
    val id: Long,
    val content: String,
    val type: ScanContentType,
    val scannedAtEpochMillis: Long,
)

enum class ScanContentType {
    URL,
    EMAIL,
    PHONE,
    WIFI,
    GEO,
    CONTACT,
    TEXT,
}
