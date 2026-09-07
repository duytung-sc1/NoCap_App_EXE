package com.nocap.app

import com.nocap.app.data.font.LocalCustomFontRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomFontUnitTest {

    @Test
    fun isValidFontHeader_acceptsValidTtfHeader() {
        val ttfHeader1 = byteArrayOf(0x00, 0x01, 0x00, 0x00)
        val ttfHeader2 = byteArrayOf(0x74, 0x72, 0x75, 0x65) // 'true'

        assertTrue(LocalCustomFontRepository.isValidFontHeader(ttfHeader1))
        assertTrue(LocalCustomFontRepository.isValidFontHeader(ttfHeader2))
    }

    @Test
    fun isValidFontHeader_acceptsValidOtfHeader() {
        val otfHeader = byteArrayOf(0x4F, 0x54, 0x54, 0x4F) // 'OTTO'
        assertTrue(LocalCustomFontRepository.isValidFontHeader(otfHeader))
    }

    @Test
    fun isValidFontHeader_acceptsValidWoffHeaders() {
        val woffHeader = byteArrayOf(0x77, 0x4F, 0x46, 0x46)  // 'wOFF'
        val woff2Header = byteArrayOf(0x77, 0x4F, 0x46, 0x32) // 'wOF2'

        assertTrue(LocalCustomFontRepository.isValidFontHeader(woffHeader))
        assertTrue(LocalCustomFontRepository.isValidFontHeader(woff2Header))
    }

    @Test
    fun isValidFontHeader_rejectsInvalidHeaders() {
        // Less than 4 bytes
        assertFalse(LocalCustomFontRepository.isValidFontHeader(byteArrayOf(0x00, 0x01)))
        assertFalse(LocalCustomFontRepository.isValidFontHeader(byteArrayOf()))

        // PDF Magic: %PDF
        val pdfHeader = byteArrayOf(0x25, 0x50, 0x44, 0x46)
        assertFalse(LocalCustomFontRepository.isValidFontHeader(pdfHeader))

        // ZIP / EPUB Magic: PK
        val zipHeader = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
        assertFalse(LocalCustomFontRepository.isValidFontHeader(zipHeader))

        // Random text
        val textHeader = byteArrayOf('A'.code.toByte(), 'B'.code.toByte(), 'C'.code.toByte(), 'D'.code.toByte())
        assertFalse(LocalCustomFontRepository.isValidFontHeader(textHeader))
    }

    @Test
    fun sanitizeFontFileName_sanitizesCharactersAndDirectoryTraversal() {
        val nameWithSpecialChars = "My Cool Font @#\$! (1).ttf"
        val sanitized = LocalCustomFontRepository.sanitizeFontFileName(nameWithSpecialChars)
        assertFalse(sanitized.contains(" "))
        assertFalse(sanitized.contains("@"))
        assertFalse(sanitized.contains("#"))
        assertTrue(sanitized.endsWith(".ttf"))

        val pathTraversal = "../../../fonts/secret.otf"
        val sanitizedPath = LocalCustomFontRepository.sanitizeFontFileName(pathTraversal)
        assertEquals("secret.otf", sanitizedPath)

        val emptySanitized = LocalCustomFontRepository.sanitizeFontFileName("   ")
        assertTrue(emptySanitized.startsWith("font_"))
        assertTrue(emptySanitized.endsWith(".ttf"))
    }

    @Test
    fun maxFontSizeBytes_is15Megabytes() {
        assertEquals(15L * 1024L * 1024L, LocalCustomFontRepository.MAX_FONT_SIZE_BYTES)
    }
}
