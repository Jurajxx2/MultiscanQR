package com.juraj.multiscanqr.domain

object RelativeTimeFormatter {

    fun format(nowEpochMillis: Long, thenEpochMillis: Long): String {
        val deltaSeconds = ((nowEpochMillis  - thenEpochMillis) / 1000).coerceAtLeast(0)
        return when {
            deltaSeconds < 60 -> "just now"
            deltaSeconds < 3_600 -> "${deltaSeconds / 60} min ago"
            deltaSeconds < 86_400 -> "${deltaSeconds / 3_600} h ago"
            else -> "${deltaSeconds / 86_400} d ago"
        }
    }
}
