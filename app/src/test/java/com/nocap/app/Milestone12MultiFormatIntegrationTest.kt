package com.nocap.app

import com.nocap.app.core.datastore.LibrarySmartView
import com.nocap.app.data.importer.DocumentFormatDetector
import com.nocap.app.data.importer.FormatSniffer
import com.nocap.app.domain.model.DocumentReaderKind
import com.nocap.app.domain.model.PublicationFormat
import com.nocap.app.domain.model.toReaderKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class Milestone12MultiFormatIntegrationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `test PublicationFormat properties categorization`() {
        // Text based
        assertTrue(PublicationFormat.TXT.isTextBased)
        assertTrue(PublicationFormat.MARKDOWN.isTextBased)
        assertTrue(PublicationFormat.HTML.isTextBased)
        assertTrue(PublicationFormat.DOCX.isTextBased)
        assertFalse(PublicationFormat.EPUB.isTextBased)
        assertFalse(PublicationFormat.PDF.isTextBased)
        assertFalse(PublicationFormat.JPEG.isTextBased)

        // Single image
        assertTrue(PublicationFormat.JPEG.isSingleImage)
        assertTrue(PublicationFormat.PNG.isSingleImage)
        assertTrue(PublicationFormat.WEBP.isSingleImage)
        assertFalse(PublicationFormat.CBZ.isSingleImage)
        assertFalse(PublicationFormat.TXT.isSingleImage)

        // Comic archive
        assertTrue(PublicationFormat.CBZ.isComicArchive)
        assertFalse(PublicationFormat.PNG.isComicArchive)
        assertFalse(PublicationFormat.EPUB.isComicArchive)

        // Reader Kind mappings
        assertEquals(DocumentReaderKind.EPUB_REFLOWABLE, PublicationFormat.EPUB.toReaderKind())
        assertEquals(DocumentReaderKind.PDF_FIXED, PublicationFormat.PDF.toReaderKind())
        assertEquals(DocumentReaderKind.TEXT_REFLOWABLE, PublicationFormat.TXT.toReaderKind())
        assertEquals(DocumentReaderKind.TEXT_REFLOWABLE, PublicationFormat.MARKDOWN.toReaderKind())
        assertEquals(DocumentReaderKind.TEXT_REFLOWABLE, PublicationFormat.HTML.toReaderKind())
        assertEquals(DocumentReaderKind.TEXT_REFLOWABLE, PublicationFormat.DOCX.toReaderKind())
        assertEquals(DocumentReaderKind.IMAGE_SINGLE, PublicationFormat.JPEG.toReaderKind())
        assertEquals(DocumentReaderKind.IMAGE_SINGLE, PublicationFormat.PNG.toReaderKind())
        assertEquals(DocumentReaderKind.IMAGE_SINGLE, PublicationFormat.WEBP.toReaderKind())
        assertEquals(DocumentReaderKind.IMAGE_ARCHIVE, PublicationFormat.CBZ.toReaderKind())
    }

    @Test
    fun `test FormatSniffer extension and mime mapping for all M12 formats`() {
        val formats = listOf(
            PublicationFormat.EPUB,
            PublicationFormat.PDF,
            PublicationFormat.TXT,
            PublicationFormat.MARKDOWN,
            PublicationFormat.HTML,
            PublicationFormat.DOCX,
            PublicationFormat.JPEG,
            PublicationFormat.PNG,
            PublicationFormat.WEBP,
            PublicationFormat.CBZ
        )

        for (format in formats) {
            val ext = FormatSniffer.extensionFor(format)
            assertNotNull("Extension for $format must not be null", ext)
            val sniffedByExt = FormatSniffer.sniffFromExtension("file.$ext")
            assertEquals("Sniff by extension should match for $format", format, sniffedByExt)

            val mime = FormatSniffer.mimeTypeFor(format)
            assertNotNull("Mime for $format must not be null", mime)
            val sniffedByMime = FormatSniffer.sniffFromMimeType(mime)
            assertEquals("Sniff by mime should match for $format", format, sniffedByMime)
        }
    }

    @Test
    fun `test DocumentFormatDetector detection by magic bytes and content`() {
        // PDF
        val pdf = tempFolder.newFile("test.pdf")
        pdf.writeBytes(byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34))
        assertEquals(PublicationFormat.PDF, DocumentFormatDetector.detect(pdf))

        // PNG
        val png = tempFolder.newFile("test.png")
        FileOutputStream(png).use { fos ->
            fos.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
            fos.write(ByteArray(20))
        }
        assertEquals(PublicationFormat.PNG, DocumentFormatDetector.detect(png))

        // JPEG
        val jpg = tempFolder.newFile("test.jpg")
        FileOutputStream(jpg).use { fos ->
            fos.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()))
            fos.write(ByteArray(20))
        }
        assertEquals(PublicationFormat.JPEG, DocumentFormatDetector.detect(jpg))

        // Markdown
        val md = tempFolder.newFile("notes.md")
        md.writeText("# Title\n\nBody text")
        assertEquals(PublicationFormat.MARKDOWN, DocumentFormatDetector.detect(md, suggestedFilename = "notes.md"))

        // HTML
        val html = tempFolder.newFile("page.html")
        html.writeText("<!DOCTYPE html><html><body><h1>Hi</h1></body></html>")
        assertEquals(PublicationFormat.HTML, DocumentFormatDetector.detect(html))

        // TXT
        val txt = tempFolder.newFile("plain.txt")
        txt.writeText("Just standard text file.")
        assertEquals(PublicationFormat.TXT, DocumentFormatDetector.detect(txt))
    }

    @Test
    fun `test DocumentFormatDetector rejects spoofed zip archive`() {
        // Empty zip renamed to .epub
        val fakeEpub = tempFolder.newFile("fake.epub")
        ZipOutputStream(FileOutputStream(fakeEpub)).use { zos ->
            zos.putNextEntry(ZipEntry("something.txt"))
            zos.write("not an epub".toByteArray())
            zos.closeEntry()
        }
        assertNull(DocumentFormatDetector.detect(fakeEpub, suggestedFilename = "fake.epub"))

        // Empty zip renamed to .docx
        val fakeDocx = tempFolder.newFile("fake.docx")
        ZipOutputStream(FileOutputStream(fakeDocx)).use { zos ->
            zos.putNextEntry(ZipEntry("something.txt"))
            zos.write("not a docx".toByteArray())
            zos.closeEntry()
        }
        assertNull(DocumentFormatDetector.detect(fakeDocx, suggestedFilename = "fake.docx"))
    }

    @Test
    fun `test LibrarySmartView supports all M12 formats`() {
        val views = listOf(
            LibrarySmartView.ALL,
            LibrarySmartView.FAVORITES,
            LibrarySmartView.EPUB,
            LibrarySmartView.PDF,
            LibrarySmartView.TXT,
            LibrarySmartView.MARKDOWN,
            LibrarySmartView.HTML,
            LibrarySmartView.DOCX,
            LibrarySmartView.IMAGE,
            LibrarySmartView.CBZ
        )
        for (view in views) {
            assertNotNull(view.name)
        }
    }
}
