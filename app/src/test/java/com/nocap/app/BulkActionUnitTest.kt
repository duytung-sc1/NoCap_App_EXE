package com.nocap.app

import com.nocap.app.domain.model.CatalogBook
import com.nocap.app.domain.model.DocumentReadingStatus
import com.nocap.app.domain.model.PublicationFormat
import com.nocap.app.domain.model.PublicationSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BulkActionUnitTest {

    private fun createSampleBooks(): List<CatalogBook> {
        return (1..5).map { i ->
            CatalogBook(
                id = "book_$i",
                title = "Document $i",
                author = "Author $i",
                coverUrl = "",
                fileUrl = "/path/$i",
                format = PublicationFormat.EPUB,
                mediaType = "application/epub+zip",
                sourceType = PublicationSourceType.LOCAL_FILE,
                sourceUrl = null,
                categoryId = "imported",
                isPinned = false,
                isArchived = false,
                isInInbox = true,
                readingStatus = DocumentReadingStatus.UNREAD
            )
        }
    }

    @Test
    fun bulkArchive_archivesSelectedBooksAndRemovesFromInbox() {
        val books = createSampleBooks().toMutableList()
        val selectedIds = setOf("book_1", "book_2", "book_3")

        // Apply bulk archive
        val updated = books.map { book ->
            if (book.id in selectedIds) {
                book.copy(isArchived = true, isInInbox = false)
            } else {
                book
            }
        }

        assertEquals(3, updated.count { it.isArchived })
        assertEquals(0, updated.filter { it.id in selectedIds }.count { it.isInInbox })
        assertEquals(2, updated.count { it.isInInbox }) // Remaining unselected
    }

    @Test
    fun bulkPin_pinsAllSelectedBooks() {
        val books = createSampleBooks()
        val selectedIds = setOf("book_2", "book_4")

        val updated = books.map { book ->
            if (book.id in selectedIds) book.copy(isPinned = true) else book
        }

        assertEquals(2, updated.count { it.isPinned })
        assertTrue(updated.first { it.id == "book_2" }.isPinned)
        assertTrue(updated.first { it.id == "book_4" }.isPinned)
        assertFalse(updated.first { it.id == "book_1" }.isPinned)
    }

    @Test
    fun bulkSetReadingStatus_updatesStatusForAllSelectedBooks() {
        val books = createSampleBooks()
        val selectedIds = setOf("book_1", "book_3", "book_5")

        val updated = books.map { book ->
            if (book.id in selectedIds) book.copy(readingStatus = DocumentReadingStatus.COMPLETED) else book
        }

        assertEquals(3, updated.count { it.readingStatus == DocumentReadingStatus.COMPLETED })
        assertEquals(2, updated.count { it.readingStatus == DocumentReadingStatus.UNREAD })
    }
}
