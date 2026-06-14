package com.juraj.multiscanqr.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScanContentClassifierTest {

    @Test
    fun classifiesHttpAndHttpsUrls() {
        assertEquals(ScanContentType.URL, ScanContentClassifier.classify("https://example.com"))
        assertEquals(ScanContentType.URL, ScanContentClassifier.classify("http://example.com/a?b=c"))
        assertEquals(ScanContentType.URL, ScanContentClassifier.classify("HTTPS://EXAMPLE.COM"))
    }

    @Test
    fun classifiesWifiPayload() {
        assertEquals(
            ScanContentType.WIFI,
            ScanContentClassifier.classify("WIFI:T:WPA;S:MyNetwork;P:secret;;"),
        )
    }

    @Test
    fun classifiesEmailPhoneGeo() {
        assertEquals(ScanContentType.EMAIL, ScanContentClassifier.classify("mailto:a@b.com"))
        assertEquals(ScanContentType.PHONE, ScanContentClassifier.classify("tel:+420123456789"))
        assertEquals(ScanContentType.GEO, ScanContentClassifier.classify("geo:50.08,14.43"))
    }

    @Test
    fun classifiesContactCards() {
        assertEquals(
            ScanContentType.CONTACT,
            ScanContentClassifier.classify("BEGIN:VCARD\nVERSION:3.0\nFN:Juraj\nEND:VCARD"),
        )
        assertEquals(ScanContentType.CONTACT, ScanContentClassifier.classify("MECARD:N:Juraj;;"))
    }

    @Test
    fun fallsBackToText() {
        assertEquals(ScanContentType.TEXT, ScanContentClassifier.classify("hello world"))
        assertEquals(ScanContentType.TEXT, ScanContentClassifier.classify("ftp://not-supported"))
        assertEquals(ScanContentType.TEXT, ScanContentClassifier.classify(""))
    }

    @Test
    fun toleratesSurroundingWhitespace() {
        assertEquals(ScanContentType.URL, ScanContentClassifier.classify("  https://example.com \n"))
    }

    @Test
    fun onlyUrlsAreOpenable() {
        assertTrue(ScanContentClassifier.isOpenableUrl("https://example.com"))
        assertFalse(ScanContentClassifier.isOpenableUrl("tel:+420123456789"))
        assertFalse(ScanContentClassifier.isOpenableUrl("javascript:alert(1)"))
    }
}
