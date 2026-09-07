package com.nocap.app

import com.nocap.app.data.parser.DocxParser
import com.nocap.app.domain.model.TextDocumentBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DocxParserTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createSyntheticDocx(
        file: File,
        documentXmlContent: String,
        coreXmlContent: String? = null,
        includeVba: Boolean = false,
        includeContentTypes: Boolean = true
    ) {
        ZipOutputStream(FileOutputStream(file)).use { zos ->
            if (includeContentTypes) {
                zos.putNextEntry(ZipEntry("[Content_Types].xml"))
                zos.write("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"></Types>".toByteArray())
                zos.closeEntry()
            }

            if (coreXmlContent != null) {
                zos.putNextEntry(ZipEntry("docProps/core.xml"))
                zos.write(coreXmlContent.toByteArray())
                zos.closeEntry()
            }

            zos.putNextEntry(ZipEntry("word/document.xml"))
            zos.write(documentXmlContent.toByteArray())
            zos.closeEntry()

            if (includeVba) {
                zos.putNextEntry(ZipEntry("word/vbaProject.bin"))
                zos.write(ByteArray(64))
                zos.closeEntry()
            }
        }
    }

    @Test
    fun `test validateStructure returns true for valid docx`() {
        val file = tempFolder.newFile("sample.docx")
        createSyntheticDocx(
            file = file,
            documentXmlContent = "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body><w:p><w:r><w:t>Xin chào</w:t></w:r></w:p></w:body></w:document>"
        )

        assertTrue(DocxParser.validateStructure(file))
    }

    @Test
    fun `test validateStructure rejects macro-enabled docx`() {
        val file = tempFolder.newFile("macro.docx")
        createSyntheticDocx(
            file = file,
            documentXmlContent = "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body></w:body></w:document>",
            includeVba = true
        )

        assertFalse(DocxParser.validateStructure(file))
    }

    @Test
    fun `test validateStructure rejects zip without content types`() {
        val file = tempFolder.newFile("nocontenttypes.docx")
        createSyntheticDocx(
            file = file,
            documentXmlContent = "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body></w:body></w:document>",
            includeContentTypes = false
        )

        assertFalse(DocxParser.validateStructure(file))
    }

    @Test
    fun `test parse extracts core metadata, headings, paragraphs, and tables`() {
        val coreXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties"
                               xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>Tài Liệu Hợp Đồng</dc:title>
                <dc:creator>Nguyễn Văn Luật</dc:creator>
            </cp:coreProperties>
        """.trimIndent()

        val docXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                <w:body>
                    <w:p>
                        <w:pPr><w:pStyle w:val="Heading1"/></w:pPr>
                        <w:r><w:t>Điều 1: Phạm vi công việc</w:t></w:r>
                    </w:p>
                    <w:p>
                        <w:r>
                            <w:rPr><w:b/></w:rPr>
                            <w:t>Nội dung in đậm</w:t>
                        </w:r>
                        <w:r>
                            <w:t> và nội dung bình thường.</w:t>
                        </w:r>
                    </w:p>
                    <w:tbl>
                        <w:tr>
                            <w:tc><w:p><w:r><w:t>Cột 1</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p><w:r><w:t>Cột 2</w:t></w:r></w:p></w:tc>
                        </w:tr>
                        <w:tr>
                            <w:tc><w:p><w:r><w:t>Dữ liệu 1</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p><w:r><w:t>Dữ liệu 2</w:t></w:r></w:p></w:tc>
                        </w:tr>
                    </w:tbl>
                </w:body>
            </w:document>
        """.trimIndent()

        val file = tempFolder.newFile("contract.docx")
        createSyntheticDocx(file, documentXmlContent = docXml, coreXmlContent = coreXml)

        val doc = DocxParser.parse(file)
        assertEquals("Tài Liệu Hợp Đồng", doc.title)
        assertEquals("Nguyễn Văn Luật", doc.author)

        val heading = doc.blocks[0] as TextDocumentBlock.Heading
        assertEquals(1, heading.level)
        assertEquals("Điều 1: Phạm vi công việc", heading.text)

        val paragraph = doc.blocks[1] as TextDocumentBlock.Paragraph
        assertEquals(2, paragraph.spans.size)
        assertEquals("Nội dung in đậm", paragraph.spans[0].text)
        assertTrue(paragraph.spans[0].isBold)
        assertEquals(" và nội dung bình thường.", paragraph.spans[1].text)
        assertFalse(paragraph.spans[1].isBold)

        val table = doc.blocks[2] as TextDocumentBlock.Table
        assertEquals(2, table.rows.size)
        assertEquals(listOf("Cột 1", "Cột 2"), table.rows[0])
        assertEquals(listOf("Dữ liệu 1", "Dữ liệu 2"), table.rows[1])
    }

    @Test
    fun `test unsupported elements warning flag`() {
        val docXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                <w:body>
                    <w:p><w:r><w:t>Văn bản bình thường</w:t></w:r></w:p>
                    <w:object><w:shape/></w:object>
                </w:body>
            </w:document>
        """.trimIndent()

        val file = tempFolder.newFile("unsupported.docx")
        createSyntheticDocx(file, documentXmlContent = docXml)

        val doc = DocxParser.parse(file)
        assertEquals(DocxParser.NOTICE_UNSUPPORTED_ELEMENTS, doc.warningMessage)
    }
}
