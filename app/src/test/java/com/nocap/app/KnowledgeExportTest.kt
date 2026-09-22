package com.nocap.app

import com.nocap.app.core.database.entity.BookmarkEntity
import com.nocap.app.core.database.entity.CatalogBookEntity
import com.nocap.app.core.database.entity.HighlightEntity
import com.nocap.app.domain.export.KnowledgeExporter
import com.nocap.app.domain.model.PublicationFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class KnowledgeExportTest {

    @Test
    fun `test formatDocumentKnowledge includes title, author, notes, highlights, and active bookmarks`() {
        val now = 1_700_000_000_000L
        val book = CatalogBookEntity(
            id = "b1",
            title = "Refactoring",
            author = "Martin Fowler",
            description = "Improving the design of existing code",
            coverUrl = "",
            categoryId = "c1",
            fileUrl = "/f1",
            fileSizeBytes = 5000,
            contentVersion = 1,
            contentHash = null,
            isFeatured = false,
            isNew = false,
            isPremium = false,
            rating = 5.0f,
            publishedDate = "2018",
            updatedAt = now,
            format = PublicationFormat.EPUB,
            userTitleOverride = "Refactoring (2nd Edition)"
        )

        val highlights = listOf(
            HighlightEntity(
                id = "hl1",
                bookId = "b1",
                locatorJson = "{}",
                text = "Any fool can write code that a computer can understand.",
                color = "YELLOW",
                note = "Golden rule of clean code",
                createdAt = now,
                updatedAt = now
            ),
            HighlightEntity(
                id = "hl2",
                bookId = "b1",
                locatorJson = "{}",
                text = "Good programmers write code that humans can understand.",
                color = "GREEN",
                note = null,
                createdAt = now,
                updatedAt = now
            )
        )

        val bookmarks = listOf(
            BookmarkEntity(
                id = "bm1",
                bookId = "b1",
                locatorJson = "{}",
                chapterTitle = "Chapter 1: The First Step",
                snippet = "Where do I begin?",
                createdAt = now,
                isDeleted = false
            ),
            BookmarkEntity(
                id = "bm_del",
                bookId = "b1",
                locatorJson = "{}",
                chapterTitle = "Deleted Chapter Bookmark",
                snippet = "Should not appear",
                createdAt = now,
                isDeleted = true
            )
        )

        val markdown = KnowledgeExporter.formatDocumentKnowledge(book, highlights, bookmarks)

        // Verifications
        assertTrue(markdown.contains("# Refactoring (2nd Edition)"))
        assertTrue(markdown.contains("- **Tác giả:** Martin Fowler"))
        assertTrue(markdown.contains("- **Định dạng:** EPUB"))

        // Notes section
        assertTrue(markdown.contains("## 📝 Ghi chú (1)"))
        assertTrue(markdown.contains("Golden rule of clean code"))
        assertTrue(markdown.contains("Any fool can write code that a computer can understand."))

        // Highlights section
        assertTrue(markdown.contains("## 💡 Đoạn trích (1)"))
        assertTrue(markdown.contains("Good programmers write code that humans can understand."))

        // Bookmarks section
        assertTrue(markdown.contains("## 🔖 Đánh dấu trang (1)"))
        assertTrue(markdown.contains("Chapter 1: The First Step"))
        assertTrue(markdown.contains("Where do I begin?"))

        // Deleted bookmark must NOT appear
        assertFalse(markdown.contains("Deleted Chapter Bookmark"))
    }

    @Test
    fun `test formatAllKnowledge combines multiple documents`() {
        val now = 1_700_000_000_000L
        val book1 = CatalogBookEntity(
            id = "b1", title = "Book One", author = "Author One", description = "",
            coverUrl = "", categoryId = "", fileUrl = "", fileSizeBytes = 0,
            contentVersion = 1, contentHash = null, isFeatured = false, isNew = false,
            isPremium = false, rating = 0.0f, publishedDate = null, updatedAt = now,
            format = PublicationFormat.TXT
        )
        val book2 = CatalogBookEntity(
            id = "b2", title = "Book Two", author = "Author Two", description = "",
            coverUrl = "", categoryId = "", fileUrl = "", fileSizeBytes = 0,
            contentVersion = 1, contentHash = null, isFeatured = false, isNew = false,
            isPremium = false, rating = 0.0f, publishedDate = null, updatedAt = now,
            format = PublicationFormat.MARKDOWN
        )

        val list = listOf(
            Triple(book1, emptyList<HighlightEntity>(), emptyList<BookmarkEntity>()),
            Triple(book2, emptyList<HighlightEntity>(), emptyList<BookmarkEntity>())
        )

        val markdown = KnowledgeExporter.formatAllKnowledge(list)

        assertTrue(markdown.contains("# NoCap — Tổng hợp Ghi chú & Bộ nhớ đọc"))
        assertTrue(markdown.contains("- **Tổng số tài liệu có ghi chú:** 2"))
        assertTrue(markdown.contains("# Book One"))
        assertTrue(markdown.contains("# Book Two"))
    }

    @Test
    fun `test writeMarkdownToStream preserves UTF-8 Vietnamese encoding`() {
        val testString = "# Tiếng Việt có dấu: Trí Tuệ Nhân Tạo, Tối Ưu Hóa & Lập Trình 🚀"
        val out = ByteArrayOutputStream()

        KnowledgeExporter.writeMarkdownToStream(out, testString)

        val resultString = out.toString(StandardCharsets.UTF_8.name())
        assertEquals(testString, resultString)
    }

    @Test
    fun `test formatTopicKnowledge formats topic header and content`() {
        val now = 1_700_000_000_000L
        val book = CatalogBookEntity(
            id = "b1", title = "Atomic Habits", author = "James Clear", description = "",
            coverUrl = "", categoryId = "", fileUrl = "", fileSizeBytes = 0,
            contentVersion = 1, contentHash = null, isFeatured = false, isNew = false,
            isPremium = false, rating = 0.0f, publishedDate = null, updatedAt = now,
            format = PublicationFormat.EPUB
        )
        val hl = HighlightEntity(
            id = "hl1", bookId = "b1", locatorJson = "{}",
            text = "You do not rise to the level of your goals.",
            color = "YELLOW", note = "Focus on systems", createdAt = now, updatedAt = now
        )
        val list = listOf(Triple(book, listOf(hl), emptyList<BookmarkEntity>()))

        val markdown = KnowledgeExporter.formatTopicKnowledge("YELLOW", list)
        assertTrue(markdown.contains("# NoCap — Tổng hợp theo chủ đề: YELLOW"))
        assertTrue(markdown.contains("# Atomic Habits"))
        assertTrue(markdown.contains("Focus on systems"))
    }

    @Test
    fun `test formatAnkiCards produces valid TSV with headers and tags`() {
        val now = 1_700_000_000_000L
        val book = CatalogBookEntity(
            id = "b1", title = "Clean Code", author = "Robert C. Martin", description = "",
            coverUrl = "", categoryId = "", fileUrl = "", fileSizeBytes = 0,
            contentVersion = 1, contentHash = null, isFeatured = false, isNew = false,
            isPremium = false, rating = 0.0f, publishedDate = null, updatedAt = now,
            format = PublicationFormat.EPUB
        )
        val hl = HighlightEntity(
            id = "hl1", bookId = "b1", locatorJson = "{}",
            text = "Clean code always looks like it was written by someone who cares.",
            color = "GREEN", note = "Definition of clean code", createdAt = now, updatedAt = now
        )
        val item = com.nocap.app.domain.model.HighlightWithBook(highlight = hl, book = book)

        val tsv = KnowledgeExporter.formatAnkiCards(listOf(item))

        assertTrue(tsv.contains("#separator:tab"))
        assertTrue(tsv.contains("#html:true"))
        assertTrue(tsv.contains("#tags column:3"))
        assertTrue(tsv.contains("Definition of clean code"))
        assertTrue(tsv.contains("Clean code always looks like it was written by someone who cares."))
        assertTrue(tsv.contains("Clean_Code"))
    }

    @Test
    fun `test formatAnkiCards escapes HTML characters and normalizes CRLF`() {
        val now = 1_700_000_000_000L
        val book = CatalogBookEntity(
            id = "b1", title = "Algorithms & Data Structures", author = "Author <Unknown>", description = "",
            coverUrl = "", categoryId = "", fileUrl = "", fileSizeBytes = 0,
            contentVersion = 1, contentHash = null, isFeatured = false, isNew = false,
            isPremium = false, rating = 0.0f, publishedDate = null, updatedAt = now,
            format = PublicationFormat.EPUB
        )
        val hl = HighlightEntity(
            id = "hl1", bookId = "b1", locatorJson = "{}",
            text = "Line 1\r\nLine 2 with <tag> & \"quotes\"",
            color = "YELLOW", note = "Condition: a < b && b > c", createdAt = now, updatedAt = now
        )
        val item = com.nocap.app.domain.model.HighlightWithBook(highlight = hl, book = book)

        val tsv = KnowledgeExporter.formatAnkiCards(listOf(item))

        assertTrue(tsv.contains("Condition: a &lt; b &amp;&amp; b &gt; c"))
        assertTrue(tsv.contains("Line 1<br>Line 2 with &lt;tag&gt; &amp; &quot;quotes&quot;"))
        assertFalse("Must not contain unescaped raw CRLF", tsv.contains("\r\n"))
    }
}

