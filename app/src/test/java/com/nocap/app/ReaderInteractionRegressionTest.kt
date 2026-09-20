package com.nocap.app

import com.nocap.app.presentation.reader.isSameReadiumBookmarkPosition
import com.nocap.app.presentation.reader.text.resolveExternalLink
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderInteractionRegressionTest {

    @Test
    fun bookmarkMatcher_matchesTheSameEpubPosition() {
        val saved = """{"href":"chapter-1.xhtml","locations":{"position":12,"progression":0.42}}"""
        val current = """{"href":"chapter-1.xhtml","locations":{"position":12,"progression":0.421}}"""

        assertTrue(isSameReadiumBookmarkPosition(saved, current))
    }

    @Test
    fun bookmarkMatcher_rejectsAnotherPositionOrResource() {
        val saved = """{"href":"chapter-1.xhtml","locations":{"position":12,"progression":0.42}}"""
        val anotherPage = """{"href":"chapter-1.xhtml","locations":{"position":13,"progression":0.70}}"""
        val anotherChapter = """{"href":"chapter-2.xhtml","locations":{"position":12,"progression":0.42}}"""

        assertFalse(isSameReadiumBookmarkPosition(saved, anotherPage))
        assertFalse(isSameReadiumBookmarkPosition(saved, anotherChapter))
    }

    @Test
    fun htmlLinks_allowSafeExternalSchemesAndResolveRelativeUrls() {
        assertTrue(resolveExternalLink("https://example.com/read", null) == "https://example.com/read")
        assertTrue(
            resolveExternalLink("../next", "https://example.com/books/chapter/current") ==
                "https://example.com/books/next"
        )
        assertNull(resolveExternalLink("javascript:alert(1)", "https://example.com/book"))
        assertNull(resolveExternalLink("#local-heading", "https://example.com/book"))
    }
}
