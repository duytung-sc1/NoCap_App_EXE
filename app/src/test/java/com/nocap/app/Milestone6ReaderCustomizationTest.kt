package com.nocap.app

import com.nocap.app.core.datastore.ReaderFontFamily
import com.nocap.app.core.datastore.ReaderPreferences
import com.nocap.app.core.datastore.ReaderTextAlignment
import com.nocap.app.core.datastore.ReaderTheme
import com.nocap.app.domain.model.Bookmark
import com.nocap.app.presentation.reader.toReadiumPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.navigator.preferences.Theme

class Milestone6ReaderCustomizationTest {

    @Test
    fun readerPreferences_toReadiumPreferences_lightTheme_serif() {
        val prefs = ReaderPreferences(
            theme = ReaderTheme.LIGHT,
            fontFamily = ReaderFontFamily.SERIF,
            fontSizeMultiplier = 1.2f,
            lineHeightMultiplier = 1.6f,
            textAlignment = ReaderTextAlignment.JUSTIFY,
            isScrollMode = false
        )

        val readiumPrefs = prefs.toReadiumPreferences()

        assertEquals(Theme.LIGHT, readiumPrefs.theme)
        assertEquals(FontFamily.SERIF, readiumPrefs.fontFamily)
        assertEquals(1.2, readiumPrefs.fontSize ?: 0.0, 0.001)
        assertEquals(1.6, readiumPrefs.lineHeight ?: 0.0, 0.001)
        assertEquals(TextAlign.JUSTIFY, readiumPrefs.textAlign)
        assertEquals(false, readiumPrefs.scroll)
    }

    @Test
    fun readerPreferences_toReadiumPreferences_sepiaTheme_sansSerif() {
        val prefs = ReaderPreferences(
            theme = ReaderTheme.SEPIA,
            fontFamily = ReaderFontFamily.SANS_SERIF,
            fontSizeMultiplier = 1.5f,
            lineHeightMultiplier = 1.8f,
            textAlignment = ReaderTextAlignment.START,
            isScrollMode = true
        )

        val readiumPrefs = prefs.toReadiumPreferences()

        assertEquals(Theme.SEPIA, readiumPrefs.theme)
        assertEquals(FontFamily.SANS_SERIF, readiumPrefs.fontFamily)
        assertEquals(1.5, readiumPrefs.fontSize ?: 0.0, 0.001)
        assertEquals(1.8, readiumPrefs.lineHeight ?: 0.0, 0.001)
        assertEquals(TextAlign.START, readiumPrefs.textAlign)
        assertEquals(true, readiumPrefs.scroll)
    }

    @Test
    fun readerPreferences_toReadiumPreferences_darkTheme_defaultFont() {
        val prefs = ReaderPreferences(
            theme = ReaderTheme.DARK,
            fontFamily = ReaderFontFamily.SYSTEM_DEFAULT,
            fontSizeMultiplier = 0.9f,
            lineHeightMultiplier = 1.3f,
            textAlignment = ReaderTextAlignment.JUSTIFY,
            isScrollMode = false
        )

        val readiumPrefs = prefs.toReadiumPreferences()

        assertEquals(Theme.DARK, readiumPrefs.theme)
        assertEquals(null, readiumPrefs.fontFamily)
        assertEquals(0.9, readiumPrefs.fontSize ?: 0.0, 0.001)
        assertEquals(0.9f.coerceIn(0.8f, 2.0f), prefs.fontSizeMultiplier, 0.001f)
    }

    @Test
    fun bookmark_creation_andLookupByHref() {
        val bookmark = Bookmark(
            id = "bm-1",
            bookId = "book-001",
            locatorJson = """{"href":"chapter2.xhtml","locations":{"progression":0.4}}""",
            chapterTitle = "Chapter 2",
            snippet = "A little bottle on it...",
            createdAt = 123456789L
        )

        val bookmarks = listOf(bookmark)

        // Test href containment
        val currentHref1 = "chapter2.xhtml"
        val isBookmarked1 = bookmarks.any { it.locatorJson.contains(currentHref1) }
        assertTrue(isBookmarked1)

        val currentHref2 = "chapter3.xhtml"
        val isBookmarked2 = bookmarks.any { it.locatorJson.contains(currentHref2) }
        assertFalse(isBookmarked2)
    }
}
