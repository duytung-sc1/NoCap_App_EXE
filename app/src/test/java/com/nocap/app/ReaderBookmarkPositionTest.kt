package com.nocap.app

import com.nocap.app.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class ReaderBookmarkPositionTest {
    @Test fun textBookmarkMatchesOnlyItsBlockAndRetainsExactOffset() {
        val saved = TextLocator(blockIndex = 12, scrollOffsetPx = 650, snippet = "Saved text").toJson()
        assertTrue(ReaderBookmarkPosition.matches(saved, TextLocator(blockIndex = 12)))
        assertFalse(ReaderBookmarkPosition.matches(saved, TextLocator(blockIndex = 13)))
        assertEquals(650, TextLocator.fromJson(saved)!!.scrollOffsetPx)
    }
    @Test fun archiveBookmarkMatchesOnlyItsPage() {
        val saved = ArchiveLocator(pageIndex = 4, entryName = "page5.jpg").toJson()
        assertTrue(ReaderBookmarkPosition.matches(saved, ArchiveLocator(pageIndex = 4)))
        assertFalse(ReaderBookmarkPosition.matches(saved, ArchiveLocator(pageIndex = 5)))
        assertFalse(ReaderBookmarkPosition.matches(saved, TextLocator(blockIndex = 4)))
    }
    @Test fun oldAndMalformedLocatorsAreHandled() {
        assertTrue(ReaderBookmarkPosition.matches("""{"type":"TEXT","blockIndex":2}""", TextLocator(blockIndex = 2)))
        assertFalse(ReaderBookmarkPosition.matches("invalid", TextLocator()))
    }
}
