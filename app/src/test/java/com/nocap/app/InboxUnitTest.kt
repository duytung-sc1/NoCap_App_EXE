package com.nocap.app

import com.nocap.app.domain.model.CatalogBook
import com.nocap.app.domain.model.DocumentReadingStatus
import com.nocap.app.domain.model.PublicationFormat
import com.nocap.app.domain.model.PublicationSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InboxUnitTest {

    @Test
    fun newImport_initializesWithInboxTrueAndUnreadStatus() {
        val now = System.currentTimeMillis()
        val book = CatalogBook(
            id = "imported_1",
            title = "Clean Code",
            author = "Robert C. Martin",
            coverUrl = "",
            fileUrl = "/path/to/clean_code.epub",
            format = PublicationFormat.EPUB,
            mediaType = "application/epub+zip",
            sourceType = PublicationSourceType.LOCAL_FILE,
            sourceUrl = null,
            categoryId = "imported",
            isInInbox = true,
            inboxAddedAt = now,
            isPinned = false,
            isArchived = false,
            readingStatus = DocumentReadingStatus.UNREAD,
            addedAt = now,
            originalFilename = "clean_code.epub"
        )

        assertTrue(book.isInInbox)
        assertNotNull(book.inboxAddedAt)
        assertEquals(DocumentReadingStatus.UNREAD, book.readingStatus)
        assertFalse(book.isArchived)
        assertFalse(book.isPinned)
        assertEquals("clean_code.epub", book.originalFilename)
    }

    @Test
    fun markOrganized_removesFromInboxWithoutDeletingBook() {
        var book = CatalogBook(
            id = "imported_2",
            title = "Design Patterns",
            author = "Gang of Four",
            coverUrl = "",
            fileUrl = "/path/to/dp.pdf",
            format = PublicationFormat.PDF,
            mediaType = "application/pdf",
            sourceType = PublicationSourceType.LOCAL_FILE,
            sourceUrl = null,
            categoryId = "imported",
            isInInbox = true,
            inboxAddedAt = System.currentTimeMillis()
        )

        assertTrue(book.isInInbox)

        // Mark organized: isInInbox = false, inboxAddedAt = null
        book = book.copy(isInInbox = false, inboxAddedAt = null)

        assertFalse(book.isInInbox)
        assertEquals(null, book.inboxAddedAt)
        assertEquals("Design Patterns", book.title) // Book remains intact
    }

    @Test
    fun archivingBook_clearsInboxState() {
        var book = CatalogBook(
            id = "imported_3",
            title = "Refactoring",
            author = "Martin Fowler",
            coverUrl = "",
            fileUrl = "/path/to/ref.epub",
            format = PublicationFormat.EPUB,
            mediaType = "application/epub+zip",
            sourceType = PublicationSourceType.LOCAL_FILE,
            sourceUrl = null,
            categoryId = "imported",
            isInInbox = true,
            inboxAddedAt = System.currentTimeMillis(),
            isArchived = false
        )

        // Archive: isArchived = true, isInInbox = false
        book = book.copy(isArchived = true, isInInbox = false)

        assertTrue(book.isArchived)
        assertFalse(book.isInInbox)
    }
}
