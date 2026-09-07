package com.nocap.app

import android.net.Uri
import com.nocap.app.core.database.entity.CatalogBookEntity
import com.nocap.app.core.database.toDomain
import com.nocap.app.core.database.toEntity
import com.nocap.app.domain.model.CatalogBook
import com.nocap.app.domain.model.DownloadProgress
import com.nocap.app.domain.model.EntitlementType
import com.nocap.app.domain.model.PublicationFormat
import com.nocap.app.domain.model.PublicationSource
import com.nocap.app.domain.model.PublicationSourceType
import com.nocap.app.domain.repository.ImportException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Milestone9aMultiFormatTest {

    @Test
    fun `test PublicationFormat enum values`() {
        val formats = PublicationFormat.entries
        assertEquals(10, formats.size)
        assertTrue(formats.contains(PublicationFormat.EPUB))
        assertTrue(formats.contains(PublicationFormat.PDF))
        assertTrue(formats.contains(PublicationFormat.CBZ))
    }

    @Test
    fun `test PublicationSource types`() {
        val remote = PublicationSource.RemoteUrl("https://example.com/doc.pdf")
        assertEquals(PublicationSourceType.REMOTE_URL, remote.sourceType)
        assertEquals("https://example.com/doc.pdf", remote.url)

        val sharedUrl = PublicationSource.SharedUrl("https://example.com/book.epub")
        assertEquals(PublicationSourceType.SHARED_URL, sharedUrl.sourceType)
        assertEquals("https://example.com/book.epub", sharedUrl.url)
    }

    @Test
    fun `test DownloadProgress percentage calculations`() {
        val progressKnown = DownloadProgress(bytesRead = 50L, totalBytes = 100L)
        assertEquals(0.5f, progressKnown.percentage ?: 0f, 0.001f)
        assertEquals(false, progressKnown.isIndeterminate)

        val progressIndeterminate = DownloadProgress(bytesRead = 1024L, totalBytes = -1L)
        assertNull(progressIndeterminate.percentage)
        assertEquals(true, progressIndeterminate.isIndeterminate)

        val progressClamped = DownloadProgress(bytesRead = 150L, totalBytes = 100L)
        assertEquals(1.0f, progressClamped.percentage ?: 0f, 0.001f)
    }

    @Test
    fun `test ImportException hierarchy`() {
        val dup = ImportException.DuplicateBook(existingBookId = "book_1", existingTitle = "Tên Sách")
        assertEquals("book_1", dup.existingBookId)
        assertEquals("Tên Sách", dup.existingTitle)
        assertTrue(dup.message.contains("Tên Sách"))

        val corruptPdf = ImportException.CorruptPdf("Lỗi tệp PDF")
        assertEquals("Lỗi tệp PDF", corruptPdf.message)

        val corruptEpub = ImportException.CorruptEpub("Lỗi tệp EPUB")
        assertEquals("Lỗi tệp EPUB", corruptEpub.message)

        val sizeLimit = ImportException.FileSizeLimitExceeded()
        assertTrue(sizeLimit.message?.contains("250 MB") == true)

        val insecure = ImportException.InsecureScheme()
        assertTrue(insecure.message?.contains("HTTPS") == true)

        val cancelled = ImportException.DownloadCancelled()
        assertTrue(cancelled.message?.contains("hủy") == true)
    }

    @Test
    fun `test CatalogBook domain and entity mapping preserves format and source`() {
        val domainBook = CatalogBook(
            id = "imported_test123",
            title = "Machine Learning in Action",
            author = "Peter Harrington",
            description = "ML Guide",
            coverUrl = "",
            categoryId = "imported",
            fileUrl = "",
            fileSizeBytes = 15000000L,
            contentHash = "hash123",
            isFeatured = false,
            isNew = false,
            isPremium = false,
            entitlementType = EntitlementType.FREE,
            rating = 4.8f,
            publishedDate = "2024",
            format = PublicationFormat.PDF,
            mediaType = "application/pdf",
            sourceType = PublicationSourceType.REMOTE_URL,
            sourceUrl = "https://example.com/ml.pdf"
        )

        val entity = domainBook.toEntity()
        assertEquals(PublicationFormat.PDF, entity.format)
        assertEquals("application/pdf", entity.mediaType)
        assertEquals(PublicationSourceType.REMOTE_URL, entity.sourceType)
        assertEquals("https://example.com/ml.pdf", entity.sourceUrl)

        val backToDomain = entity.toDomain()
        assertEquals(domainBook.id, backToDomain.id)
        assertEquals(domainBook.format, backToDomain.format)
        assertEquals(domainBook.mediaType, backToDomain.mediaType)
        assertEquals(domainBook.sourceType, backToDomain.sourceType)
        assertEquals(domainBook.sourceUrl, backToDomain.sourceUrl)
    }

    @Test
    fun `test CatalogBook default values are backward compatible with EPUB`() {
        val entity = CatalogBookEntity(
            id = "legacy_1",
            title = "Legacy Book",
            author = "Author",
            description = "Desc",
            coverUrl = "",
            categoryId = "cat1",
            fileUrl = "https://example.com/legacy.epub",
            fileSizeBytes = 1000L
        )

        assertEquals(PublicationFormat.EPUB, entity.format)
        assertEquals("application/epub+zip", entity.mediaType)
        assertEquals(PublicationSourceType.LOCAL_FILE, entity.sourceType)
        assertNull(entity.sourceUrl)

        val domain = entity.toDomain()
        assertEquals(PublicationFormat.EPUB, domain.format)
        assertEquals("application/epub+zip", domain.mediaType)
        assertEquals(PublicationSourceType.LOCAL_FILE, domain.sourceType)
        assertNull(domain.sourceUrl)
    }
}
