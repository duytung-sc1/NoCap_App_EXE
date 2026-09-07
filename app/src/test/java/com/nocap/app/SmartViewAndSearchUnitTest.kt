package com.nocap.app

import com.nocap.app.core.datastore.LibrarySmartView
import com.nocap.app.core.datastore.LibrarySort
import com.nocap.app.domain.model.CatalogBook
import com.nocap.app.domain.model.DocumentReadingStatus
import com.nocap.app.domain.model.DownloadedBook
import com.nocap.app.domain.model.LibraryBook
import com.nocap.app.domain.model.PublicationFormat
import com.nocap.app.domain.model.PublicationSourceType
import com.nocap.app.domain.model.ReadingProgress
import com.nocap.app.domain.model.Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartViewAndSearchUnitTest {

    private val sampleBooks = listOf(
        createBook("1", "Lập trình Kotlin", "Nguyễn Văn A", PublicationFormat.EPUB, isPinned = true, status = DocumentReadingStatus.READING, isArchived = false, tags = listOf("android", "kotlin")),
        createBook("2", "Flutter Toàn Tập", "Trần Văn B", PublicationFormat.PDF, isPinned = false, status = DocumentReadingStatus.UNREAD, isArchived = false, tags = listOf("flutter")),
        createBook("3", "Thiết kế Hệ thống", "Lê Văn C", PublicationFormat.EPUB, isPinned = false, status = DocumentReadingStatus.COMPLETED, isArchived = false, isFavorite = true),
        createBook("4", "Sách Cũ Đã Lưu Trữ", "Phạm Văn D", PublicationFormat.EPUB, isPinned = false, status = DocumentReadingStatus.COMPLETED, isArchived = true)
    )

    private fun createBook(
        id: String,
        title: String,
        author: String,
        format: PublicationFormat,
        isPinned: Boolean = false,
        status: DocumentReadingStatus = DocumentReadingStatus.UNREAD,
        isArchived: Boolean = false,
        isFavorite: Boolean = false,
        tags: List<String> = emptyList()
    ): LibraryBook {
        val catBook = CatalogBook(
            id = id,
            title = title,
            author = author,
            coverUrl = "",
            fileUrl = "/path/$id",
            format = format,
            mediaType = if (format == PublicationFormat.EPUB) "application/epub+zip" else "application/pdf",
            sourceType = PublicationSourceType.LOCAL_FILE,
            sourceUrl = null,
            categoryId = "imported",
            isPinned = isPinned,
            isArchived = isArchived,
            readingStatus = status,
            addedAt = 1000L + id.toLong()
        )
        val dlBook = DownloadedBook(bookId = id, localFilePath = "/path/$id", totalBytes = 100L)
        return LibraryBook(
            book = catBook,
            downloadedBook = dlBook,
            isFavorite = isFavorite,
            tags = tags.map { Tag(id = it, name = it, normalizedName = it, createdAt = 0L) }
        )
    }

    @Test
    fun smartView_filtersCorrectly() {
        // ALL (unarchived)
        val all = sampleBooks.filter { !it.book.isArchived }
        assertEquals(3, all.size)

        // PINNED
        val pinned = all.filter { it.book.isPinned }
        assertEquals(1, pinned.size)
        assertEquals("1", pinned[0].book.id)

        // FAVORITES
        val favorites = all.filter { it.isFavorite }
        assertEquals(1, favorites.size)
        assertEquals("3", favorites[0].book.id)

        // UNREAD
        val unread = all.filter { it.book.readingStatus == DocumentReadingStatus.UNREAD }
        assertEquals(1, unread.size)
        assertEquals("2", unread[0].book.id)

        // READING
        val reading = all.filter { it.book.readingStatus == DocumentReadingStatus.READING }
        assertEquals(1, reading.size)
        assertEquals("1", reading[0].book.id)

        // ARCHIVED
        val archived = sampleBooks.filter { it.book.isArchived }
        assertEquals(1, archived.size)
        assertEquals("4", archived[0].book.id)

        // PDF
        val pdfs = all.filter { it.book.format == PublicationFormat.PDF }
        assertEquals(1, pdfs.size)
        assertEquals("2", pdfs[0].book.id)
    }

    @Test
    fun search_matchesTitleAuthorAndTags() {
        val all = sampleBooks.filter { !it.book.isArchived }

        // Search by author
        val byAuthor = all.filter { it.book.displayAuthor.lowercase().contains("nguyễn") }
        assertEquals(1, byAuthor.size)
        assertEquals("1", byAuthor[0].book.id)

        // Search by tag
        val byTag = all.filter { book -> book.tags.any { it.name.lowercase().contains("flutter") } }
        assertEquals(1, byTag.size)
        assertEquals("2", byTag[0].book.id)

        // Search by title
        val byTitle = all.filter { it.book.displayTitle.lowercase().contains("hệ thống") }
        assertEquals(1, byTitle.size)
        assertEquals("3", byTitle[0].book.id)
    }

    @Test
    fun sorting_ordersAscendingAndDescending() {
        val all = sampleBooks.filter { !it.book.isArchived }

        val sortedByTitleAsc = all.sortedBy { it.book.displayTitle.lowercase() }
        assertEquals("Flutter Toàn Tập", sortedByTitleAsc.first().book.displayTitle)

        val sortedByTitleDesc = all.sortedByDescending { it.book.displayTitle.lowercase() }
        assertEquals("Thiết kế Hệ thống", sortedByTitleDesc.first().book.displayTitle)
    }
}
