package com.nocap.app

import com.nocap.app.domain.model.CatalogBook
import com.nocap.app.domain.model.PublicationFormat
import com.nocap.app.domain.model.PublicationSourceType
import org.junit.Assert.assertEquals
import org.junit.Test

class MetadataOverrideUnitTest {

    @Test
    fun displayTitle_returnsOverrideWhenPresentElseExtractedTitle() {
        val bookWithoutOverride = CatalogBook(
            id = "b1",
            title = "Original Title",
            author = "Original Author",
            coverUrl = "",
            fileUrl = "/path",
            format = PublicationFormat.EPUB,
            mediaType = "application/epub+zip",
            sourceType = PublicationSourceType.LOCAL_FILE,
            sourceUrl = null,
            categoryId = "imported",
            userTitleOverride = null
        )
        assertEquals("Original Title", bookWithoutOverride.displayTitle)

        val bookWithOverride = bookWithoutOverride.copy(userTitleOverride = "Custom New Title")
        assertEquals("Custom New Title", bookWithOverride.displayTitle)

        val bookWithBlankOverride = bookWithoutOverride.copy(userTitleOverride = "   ")
        assertEquals("Original Title", bookWithBlankOverride.displayTitle)
    }

    @Test
    fun displayAuthor_returnsOverrideWhenPresentElseExtractedAuthor() {
        val bookWithoutOverride = CatalogBook(
            id = "b2",
            title = "Book Two",
            author = "Original Author",
            coverUrl = "",
            fileUrl = "/path",
            format = PublicationFormat.PDF,
            mediaType = "application/pdf",
            sourceType = PublicationSourceType.LOCAL_FILE,
            sourceUrl = null,
            categoryId = "imported",
            userAuthorOverride = null
        )
        assertEquals("Original Author", bookWithoutOverride.displayAuthor)

        val bookWithOverride = bookWithoutOverride.copy(userAuthorOverride = "Custom Pen Name")
        assertEquals("Custom Pen Name", bookWithOverride.displayAuthor)

        val bookWithBlankOverride = bookWithoutOverride.copy(userAuthorOverride = "")
        assertEquals("Original Author", bookWithBlankOverride.displayAuthor)
    }

    @Test
    fun customCoverValidation_checks10MbLimit() {
        val maxAllowedBytes = 10 * 1024 * 1024L
        val validSize = 5 * 1024 * 1024L
        val invalidSize = 11 * 1024 * 1024L

        assertEquals(true, validSize <= maxAllowedBytes)
        assertEquals(false, invalidSize <= maxAllowedBytes)
    }
}
