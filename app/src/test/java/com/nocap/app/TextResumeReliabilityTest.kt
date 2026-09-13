package com.nocap.app

import com.nocap.app.domain.model.TextLocator
import org.junit.Assert.*
import org.junit.Test

class TextResumeReliabilityTest {
    @Test fun unicodeFilenamesRetainHashAndQuestionMarkWhileUrlsIgnoreQuery() {
        val sniffer = com.nocap.app.data.importer.FormatSniffer
        assertEquals(com.nocap.app.domain.model.PublicationFormat.TXT, sniffer.sniffFromExtension("Đọc 日本語 한글 #1.txt"))
        assertEquals(com.nocap.app.domain.model.PublicationFormat.PDF, sniffer.sniffFromExtension("Why?.pdf"))
        assertEquals(com.nocap.app.domain.model.PublicationFormat.PDF, sniffer.sniffFromExtension("https://example.com/file.pdf?token=x#page=1"))
    }
    @Test fun oldSyncedLocatorStillRestoresWithoutPixelOffset() {
        val locator = TextLocator.fromJson("""{"type":"TEXT","version":1,"blockIndex":25,"characterOffset":7,"progression":0.25}""")!!
        assertEquals(25, locator.blockIndex)
        assertEquals(7, locator.characterOffset)
        assertEquals(0, locator.scrollOffsetPx)
    }

    @Test fun positionWithinLongParagraphSurvivesSerialization() {
        val original = TextLocator(blockIndex = 3, characterOffset = 7, progression = .4f,
            snippet = "Đọc 日本語 한글", scrollOffsetPx = 1840)
        assertEquals(original, TextLocator.fromJson(original.toJson()))
    }

    @Test fun corruptOffsetsCannotRequestNegativeScroll() {
        val locator = TextLocator.fromJson("""{"type":"TEXT","scrollOffsetPx":-500}""")!!
        assertEquals(0, locator.scrollOffsetPx)
        assertNull(TextLocator.fromJson("broken json"))
    }
}
