package com.juraj.multiscanqr.domain

object ScanContentClassifier {

    fun classify(raw: String): ScanContentType {
        val trimmed = raw.trim()
        return when {
            trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) -> ScanContentType.URL

            trimmed.startsWith("WIFI:", ignoreCase = true) -> ScanContentType.WIFI

            trimmed.startsWith("mailto:", ignoreCase = true) ||
                trimmed.startsWith("MATMSG:", ignoreCase = true) -> ScanContentType.EMAIL

            trimmed.startsWith("tel:", ignoreCase = true) -> ScanContentType.PHONE

            trimmed.startsWith("geo:", ignoreCase = true) -> ScanContentType.GEO

            trimmed.startsWith("BEGIN:VCARD", ignoreCase = true) ||
                trimmed.startsWith("MECARD:", ignoreCase = true) -> ScanContentType.CONTACT

            else -> ScanContentType.TEXT
        }
    }

    fun isOpenableUrl(raw: String): Boolean = classify(raw) == ScanContentType.URL
}
