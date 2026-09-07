package com.nocap.app

import com.nocap.app.data.importer.FormatSniffer
import com.nocap.app.domain.model.PublicationFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream

class FormatDetectorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `test detect PDF magic bytes`() {
        val pdfHeader = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34) // %PDF-1.4
        val format = FormatSniffer.sniff(pdfHeader)
        assertEquals(PublicationFormat.PDF, format)
    }

    @Test
    fun `test detect EPUB zip magic bytes`() {
        val zipHeader = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00, 0x00, 0x00, 0x00) // PK\x03\x04
        val format = FormatSniffer.sniff(zipHeader)
        assertEquals(PublicationFormat.EPUB, format)
    }

    @Test
    fun `test unknown header returns null`() {
        val randomHeader = byteArrayOf(0x7F, 0x45, 0x4C, 0x46) // ELF binary
        val format = FormatSniffer.sniff(randomHeader)
        assertNull(format)
    }

    @Test
    fun `test short byte array returns null`() {
        val shortHeader = byteArrayOf(0x25, 0x50)
        val format = FormatSniffer.sniff(shortHeader)
        assertNull(format)
    }

    @Test
    fun `test detect format from file on disk`() {
        val pdfFile = tempFolder.newFile("sample.pdf")
        FileOutputStream(pdfFile).use {
            it.write(byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x37))
        }
        val format = FormatSniffer.sniff(pdfFile)
        assertEquals(PublicationFormat.PDF, format)
    }

    @Test
    fun `test detect format from extension`() {
        assertEquals(PublicationFormat.EPUB, FormatSniffer.sniffFromExtension("book.epub"))
        assertEquals(PublicationFormat.PDF, FormatSniffer.sniffFromExtension("document.PDF"))
        assertEquals(PublicationFormat.CBZ, FormatSniffer.sniffFromExtension("comic.cbz"))
        assertEquals(PublicationFormat.TXT, FormatSniffer.sniffFromExtension("file.txt"))
        assertEquals(PublicationFormat.MARKDOWN, FormatSniffer.sniffFromExtension("notes.md"))
        assertEquals(PublicationFormat.HTML, FormatSniffer.sniffFromExtension("page.html"))
        assertEquals(PublicationFormat.DOCX, FormatSniffer.sniffFromExtension("doc.docx"))
        assertEquals(PublicationFormat.JPEG, FormatSniffer.sniffFromExtension("photo.jpg"))
        assertEquals(PublicationFormat.PNG, FormatSniffer.sniffFromExtension("image.png"))
        assertEquals(PublicationFormat.WEBP, FormatSniffer.sniffFromExtension("graphic.webp"))
        assertEquals(PublicationFormat.EPUB, FormatSniffer.sniffFromExtension("https://site.org/file.epub?token=abc#hash"))
        assertNull(FormatSniffer.sniffFromExtension("archive.zip"))
        assertNull(FormatSniffer.sniffFromExtension("spreadsheet.xlsx"))
    }

    @Test
    fun `test detect format from mime type`() {
        assertEquals(PublicationFormat.EPUB, FormatSniffer.sniffFromMimeType("application/epub+zip"))
        assertEquals(PublicationFormat.EPUB, FormatSniffer.sniffFromMimeType("application/epub+zip; charset=utf-8"))
        assertEquals(PublicationFormat.PDF, FormatSniffer.sniffFromMimeType("application/pdf"))
        assertEquals(PublicationFormat.CBZ, FormatSniffer.sniffFromMimeType("application/x-cbz"))
        assertEquals(PublicationFormat.TXT, FormatSniffer.sniffFromMimeType("text/plain"))
        assertEquals(PublicationFormat.MARKDOWN, FormatSniffer.sniffFromMimeType("text/markdown"))
        assertEquals(PublicationFormat.HTML, FormatSniffer.sniffFromMimeType("text/html"))
        assertEquals(PublicationFormat.DOCX, FormatSniffer.sniffFromMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        assertEquals(PublicationFormat.JPEG, FormatSniffer.sniffFromMimeType("image/jpeg"))
        assertEquals(PublicationFormat.PNG, FormatSniffer.sniffFromMimeType("image/png"))
        assertEquals(PublicationFormat.WEBP, FormatSniffer.sniffFromMimeType("image/webp"))
        assertNull(FormatSniffer.sniffFromMimeType("application/octet-stream"))
        assertNull(FormatSniffer.sniffFromMimeType(null))
    }

    @Test
    fun `test format to extension and mime mapping`() {
        assertEquals("epub", FormatSniffer.extensionFor(PublicationFormat.EPUB))
        assertEquals("pdf", FormatSniffer.extensionFor(PublicationFormat.PDF))
        assertEquals("application/epub+zip", FormatSniffer.mimeTypeFor(PublicationFormat.EPUB))
        assertEquals("application/pdf", FormatSniffer.mimeTypeFor(PublicationFormat.PDF))
    }

    @Test
    fun `test generic zip renamed to epub is rejected by file sniffing`() {
        val genericZip = tempFolder.newFile("generic.epub")
        java.util.zip.ZipOutputStream(FileOutputStream(genericZip)).use { zos ->
            zos.putNextEntry(java.util.zip.ZipEntry("hello.txt"))
            zos.write("Hello World".toByteArray())
            zos.closeEntry()
        }
        val isEpub = FormatSniffer.isEpubZip(genericZip)
        org.junit.Assert.assertFalse(isEpub)
        val format = FormatSniffer.sniff(genericZip)
        assertNull(format)
    }

    @Test
    fun `test valid epub zip with mimetype is accepted`() {
        val validEpub = tempFolder.newFile("valid.epub")
        java.util.zip.ZipOutputStream(FileOutputStream(validEpub)).use { zos ->
            zos.putNextEntry(java.util.zip.ZipEntry("mimetype"))
            zos.write("application/epub+zip".toByteArray())
            zos.closeEntry()
        }
        val isEpub = FormatSniffer.isEpubZip(validEpub)
        org.junit.Assert.assertTrue(isEpub)
        val format = FormatSniffer.sniff(validEpub)
        assertEquals(PublicationFormat.EPUB, format)
    }

    @Test
    fun `test valid epub zip with container xml is accepted`() {
        val validEpub = tempFolder.newFile("valid_container.epub")
        java.util.zip.ZipOutputStream(FileOutputStream(validEpub)).use { zos ->
            zos.putNextEntry(java.util.zip.ZipEntry("META-INF/container.xml"))
            zos.write("<container/>".toByteArray())
            zos.closeEntry()
        }
        val isEpub = FormatSniffer.isEpubZip(validEpub)
        org.junit.Assert.assertTrue(isEpub)
        val format = FormatSniffer.sniff(validEpub)
        assertEquals(PublicationFormat.EPUB, format)
    }

    @Test
    fun `test corrupt zip file returns null`() {
        val corruptZip = tempFolder.newFile("corrupt.epub")
        FileOutputStream(corruptZip).use {
            it.write(byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00, 0x01, 0x02, 0x03))
        }
        val isEpub = FormatSniffer.isEpubZip(corruptZip)
        org.junit.Assert.assertFalse(isEpub)
        val format = FormatSniffer.sniff(corruptZip)
        assertNull(format)
    }

    @Test
    fun `test non pdf file with pdf extension returns null`() {
        val fakePdf = tempFolder.newFile("fake.pdf")
        fakePdf.writeText("This is plain text pretending to be a PDF")
        val format = FormatSniffer.sniff(fakePdf)
        assertNull(format)
    }
}
