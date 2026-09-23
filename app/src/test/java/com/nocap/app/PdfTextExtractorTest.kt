package com.nocap.app

import com.nocap.app.data.parser.PdfTextExtractor
import com.nocap.app.domain.model.DocumentLocator
import com.nocap.app.domain.model.PdfAnnotationLocator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PdfTextExtractorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `test non-existent or small file returns no selectable text`() {
        val nonExistent = File(tempFolder.root, "does_not_exist.pdf")
        assertFalse(PdfTextExtractor.hasSelectableText(nonExistent))

        val dummyFile = tempFolder.newFile("dummy.pdf")
        dummyFile.writeBytes(ByteArray(20))
        assertFalse(PdfTextExtractor.hasSelectableText(dummyFile))
    }

    @Test
    fun `test extractPageText on invalid file returns empty result`() {
        val dummyFile = tempFolder.newFile("empty.pdf")
        dummyFile.writeBytes(ByteArray(10))

        val result = PdfTextExtractor.extractPageText(dummyFile, targetPageIndex = 0)
        assertEquals(0, result.pageIndex)
        assertEquals("", result.text)
        assertEquals(0, result.wordCount)
        assertTrue(result.errorMessage?.contains("không hợp lệ") == true)
    }

    @Test
    fun `malformed PDF reports extraction error instead of looking like a scanned page`() {
        val malformed = tempFolder.newFile("malformed.pdf")
        malformed.writeText("not-a-pdf".repeat(20))

        val result = PdfTextExtractor.extractPageText(malformed, targetPageIndex = 0)

        assertTrue(result.text.isEmpty())
        assertTrue(result.errorMessage?.contains("cấu trúc PDF") == true)
    }

    @Test
    fun `test uncompressed text stream extraction from valid PDF bytes`() {
        val pdfContent = """
            %PDF-1.4
            1 0 obj
            << /Type /Catalog /Pages 2 0 R >>
            endobj
            2 0 obj
            << /Type /Pages /Kids [3 0 R] /Count 1 >>
            endobj
            3 0 obj
            << /Type /Page /Parent 2 0 R /Contents 4 0 R >>
            endobj
            4 0 obj
            << /Length 50 >>
            stream
            BT
            /F1 12 Tf
            (Xin chao NoCap Reader) Tj
            ET
            endstream
            endobj
            xref
            0 5
            trailer
            << /Root 1 0 R >>
            %%EOF
        """.trimIndent()

        val pdfFile = tempFolder.newFile("sample_text.pdf")
        pdfFile.writeText(pdfContent, Charsets.US_ASCII)

        val pages = PdfTextExtractor.extractPagesText(pdfFile)
        assertTrue(pages.isNotEmpty())
        assertEquals("Xin chao NoCap Reader", pages[0].text)
        assertEquals(4, pages[0].wordCount)
        assertTrue(PdfTextExtractor.hasSelectableText(pdfFile))
    }

    @Test
    fun `test kerning array TJ operator extraction`() {
        val pdfContent = """
            %PDF-1.4
            1 0 obj
            << /Length 60 >>
            stream
            BT
            [(Kien) -10 ( truc) 20 ( May) -10 ( Tinh)] TJ
            ET
            endstream
            endobj
            %%EOF
        """.trimIndent()

        val pdfFile = tempFolder.newFile("sample_tj.pdf")
        pdfFile.writeText(pdfContent, Charsets.US_ASCII)

        val pages = PdfTextExtractor.extractPagesText(pdfFile)
        assertTrue(pages.isNotEmpty())
        assertEquals("Kien truc May Tinh", pages[0].text)
    }

    @Test
    fun `test PdfAnnotationLocator serialization and deserialization`() {
        val locator = PdfAnnotationLocator(
            pageIndex = 2,
            pageNumber = 3,
            progression = 0.25f,
            selectedText = "Design patterns provide reusable solutions",
            startOffset = 10,
            endOffset = 52,
            contextSnippet = "...reusable solutions to common software problems..."
        )

        val json = locator.toJson()
        assertTrue(json.contains("\"type\":\"PDF\""))
        assertTrue(json.contains("\"pageIndex\":2"))
        assertTrue(json.contains("\"pageNumber\":3"))

        val parsed = DocumentLocator.fromJson(json) as? PdfAnnotationLocator
        assertNotNull(parsed)
        assertEquals(2, parsed?.pageIndex)
        assertEquals(3, parsed?.pageNumber)
        assertEquals(0.25f, parsed?.progression ?: 0f, 0.001f)
        assertEquals("Design patterns provide reusable solutions", parsed?.selectedText)
        assertEquals(10, parsed?.startOffset)
        assertEquals(52, parsed?.endOffset)
        assertEquals("...reusable solutions to common software problems...", parsed?.contextSnippet)
    }
}
