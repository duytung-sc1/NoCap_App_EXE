package com.nocap.app

import com.nocap.app.core.database.dao.BookmarkDao
import com.nocap.app.core.database.dao.CatalogDao
import com.nocap.app.core.database.dao.DownloadDao
import com.nocap.app.core.database.dao.HighlightDao
import com.nocap.app.core.database.entity.BookmarkEntity
import com.nocap.app.core.database.entity.CatalogBookEntity
import com.nocap.app.core.database.entity.HighlightEntity
import com.nocap.app.core.util.VietnameseUtils
import com.nocap.app.data.search.LocalKnowledgeSearchRepository
import com.nocap.app.domain.model.KnowledgeItemType
import com.nocap.app.domain.model.PublicationFormat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgeSearchTest {

    @Test
    fun `test VietnameseUtils removeAccents correctly handles complex Vietnamese text`() {
        assertEquals("Tri Tue Nhan Tao", VietnameseUtils.removeAccents("Trí Tuệ Nhân Tạo"))
        assertEquals("Lap trinh huong doi tuong", VietnameseUtils.removeAccents("Lập trình hướng đối tượng"))
        assertEquals("Duong vao khoa hoc", VietnameseUtils.removeAccents("Đường vào khoa học"))
        assertEquals("Tieng Viet co dau", VietnameseUtils.removeAccents("Tiếng Việt có dấu"))
        assertEquals("", VietnameseUtils.removeAccents(null))
        assertEquals("", VietnameseUtils.removeAccents(""))
    }

    @Test
    fun `test VietnameseUtils containsNormalized matches accent-insensitively and case-insensitively`() {
        val text = "Trí Tuệ Nhân Tạo và Tương Lai Nhân Loại"

        assertTrue(VietnameseUtils.containsNormalized(text, "tri tue"))
        assertTrue(VietnameseUtils.containsNormalized(text, "TRÍ TUỆ"))
        assertTrue(VietnameseUtils.containsNormalized(text, "nhan loai"))
        assertTrue(VietnameseUtils.containsNormalized(text, "tuong lai"))
        assertFalse(VietnameseUtils.containsNormalized(text, "kinh te"))
    }

    @Test
    fun `test KnowledgeSearch across documents, highlights, notes, and bookmarks with filters`() = runBlocking {
        val now = System.currentTimeMillis()
        val books = listOf(
            CatalogBookEntity(
                id = "b1",
                title = "Học Máy Thực Hành",
                author = "Nguyễn Văn A",
                description = "Sách về AI",
                coverUrl = "",
                categoryId = "c1",
                fileUrl = "/f1",
                fileSizeBytes = 100,
                contentVersion = 1,
                contentHash = null,
                isFeatured = false,
                isNew = false,
                isPremium = false,
                rating = 4.5f,
                publishedDate = "2024",
                updatedAt = now,
                format = PublicationFormat.EPUB,
                userTitleOverride = "Machine Learning Nâng Cao"
            )
        )

        val highlights = listOf(
            HighlightEntity(
                id = "hl1",
                bookId = "b1",
                locatorJson = "{}",
                text = "Gradient Descent là thuật toán tối ưu hóa cốt lõi",
                color = "YELLOW",
                note = "Cần ôn kỹ thuật toán này cho phỏng vấn",
                createdAt = now,
                updatedAt = now
            ),
            HighlightEntity(
                id = "hl2",
                bookId = "b1",
                locatorJson = "{}",
                text = "Overfitting xảy ra khi mô hình quá khớp dữ liệu huấn luyện",
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
                chapterTitle = "Chương 4: Mạng Nơ-ron Tích chập",
                snippet = "Giới thiệu về kiến trúc CNN",
                createdAt = now
            )
        )

        val fakeCatalogDao = object : CatalogDao by makeMockProxy(CatalogDao::class.java) {
            override suspend fun getAllBooks(): List<CatalogBookEntity> = books
            override suspend fun getBookById(bookId: String): CatalogBookEntity? = books.find { it.id == bookId }
        }
        val fakeHighlightDao = object : HighlightDao by makeMockProxy(HighlightDao::class.java) {
            override suspend fun getAllHighlights(): List<HighlightEntity> = highlights
            override suspend fun getHighlightsForBook(bookId: String): List<HighlightEntity> = highlights.filter { it.bookId == bookId }
        }
        val fakeBookmarkDao = object : BookmarkDao by makeMockProxy(BookmarkDao::class.java) {
            override suspend fun getAllBookmarks(): List<BookmarkEntity> = bookmarks
            override suspend fun getBookmarksForBook(bookId: String): List<BookmarkEntity> = bookmarks.filter { it.bookId == bookId }
        }
        val fakeDownloadDao = makeMockProxy(DownloadDao::class.java)

        val repository = LocalKnowledgeSearchRepository(fakeCatalogDao, fakeHighlightDao, fakeBookmarkDao, fakeDownloadDao)

        // 1. Search with accent-free query "toi uu" should match highlight "tối ưu hóa"
        val hlResults = repository.search("toi uu", KnowledgeItemType.ALL)
        assertEquals(1, hlResults.size)
        assertEquals(KnowledgeItemType.NOTE, hlResults[0].type) // hl1 has a note

        // 2. Search note content "phong van"
        val noteResults = repository.search("phong van", KnowledgeItemType.NOTE)
        assertEquals(1, noteResults.size)
        assertEquals("hl1", noteResults[0].id.removePrefix("note_"))

        // 3. Search document override title "machine learning"
        val docResults = repository.search("machine learning", KnowledgeItemType.DOCUMENT)
        assertEquals(1, docResults.size)
        assertEquals("b1", docResults[0].bookId)

        // 4. Search bookmark "no-ron" (accent-free: "no-ron" matches "Nơ-ron")
        val bmResults = repository.search("no-ron", KnowledgeItemType.BOOKMARK)
        assertEquals(1, bmResults.size)
        assertEquals("bm1", bmResults[0].id.removePrefix("bm_"))

        // 5. Query matching nothing
        val emptyResults = repository.search("blockchain", KnowledgeItemType.ALL)
        assertTrue(emptyResults.isEmpty())
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> makeMockProxy(clazz: Class<T>): T {
        return java.lang.reflect.Proxy.newProxyInstance(
            clazz.classLoader,
            arrayOf(clazz)
        ) { _, _, _ -> null } as T
    }
}
