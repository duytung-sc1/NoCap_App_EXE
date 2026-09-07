package com.nocap.app

import com.nocap.app.data.parser.TxtParser
import com.nocap.app.domain.model.TextDocumentBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

class TxtParserTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `test parse standard UTF-8 text with mixed CRLF and LF`() {
        val file = tempFolder.newFile("notes.txt")
        file.writeText("Đoạn văn thứ nhất.\r\nTiếp tục đoạn một.\r\n\r\nĐoạn văn thứ hai.\nTiếp tục đoạn hai.")

        val doc = TxtParser.validateAndParse(file)
        assertEquals("notes", doc.title)
        assertEquals(2, doc.blocks.size)

        val block1 = doc.blocks[0] as TextDocumentBlock.Paragraph
        assertTrue(block1.spans[0].text.contains("Đoạn văn thứ nhất."))
        assertTrue(block1.spans[0].text.contains("Tiếp tục đoạn một."))

        val block2 = doc.blocks[1] as TextDocumentBlock.Paragraph
        assertTrue(block2.spans[0].text.contains("Đoạn văn thứ hai."))
    }

    @Test
    fun `test parse UTF-8 with BOM`() {
        val file = tempFolder.newFile("bom.txt")
        FileOutputStream(file).use { fos ->
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            fos.write("Nội dung sau BOM".toByteArray(StandardCharsets.UTF_8))
        }

        val doc = TxtParser.validateAndParse(file)
        assertEquals(1, doc.blocks.size)
        val block = doc.blocks[0] as TextDocumentBlock.Paragraph
        assertEquals("Nội dung sau BOM", block.spans[0].text)
    }

    @Test
    fun `test parse UTF-16LE with BOM`() {
        val file = tempFolder.newFile("utf16le.txt")
        FileOutputStream(file).use { fos ->
            fos.write(byteArrayOf(0xFF.toByte(), 0xFE.toByte()))
            fos.write("Văn bản UTF-16 LE".toByteArray(StandardCharsets.UTF_16LE))
        }

        val doc = TxtParser.validateAndParse(file)
        val block = doc.blocks[0] as TextDocumentBlock.Paragraph
        assertEquals("Văn bản UTF-16 LE", block.spans[0].text)
    }

    @Test
    fun `test parse UTF-16BE with BOM`() {
        val file = tempFolder.newFile("utf16be.txt")
        FileOutputStream(file).use { fos ->
            fos.write(byteArrayOf(0xFE.toByte(), 0xFF.toByte()))
            fos.write("Văn bản UTF-16 BE".toByteArray(StandardCharsets.UTF_16BE))
        }

        val doc = TxtParser.validateAndParse(file)
        val block = doc.blocks[0] as TextDocumentBlock.Paragraph
        assertEquals("Văn bản UTF-16 BE", block.spans[0].text)
    }

    @Test
    fun `test reject binary file containing null bytes`() {
        val file = tempFolder.newFile("binary.txt")
        FileOutputStream(file).use { fos ->
            fos.write("Hello\u0000World".toByteArray(StandardCharsets.UTF_8))
        }

        val ex = assertThrows(IllegalArgumentException::class.java) {
            TxtParser.validateAndParse(file)
        }
        assertTrue(ex.message?.contains("nhị phân") == true)
    }

    @Test
    fun `test reject file with excessive control characters`() {
        val file = tempFolder.newFile("corrupt.txt")
        val bytes = ByteArray(200) { 0x07 } // BEL control character
        file.writeBytes(bytes)

        val ex = assertThrows(IllegalArgumentException::class.java) {
            TxtParser.validateAndParse(file)
        }
        assertTrue(ex.message?.contains("ký tự điều khiển") == true)
    }

    @Test
    fun `test suggested title is used when provided`() {
        val file = tempFolder.newFile("random_name.txt")
        file.writeText("Nội dung bài viết")

        val doc = TxtParser.validateAndParse(file, suggestedTitle = "Bài viết tùy chỉnh.txt")
        assertEquals("Bài viết tùy chỉnh", doc.title)
    }

    @Test
    fun `test throws for missing file`() {
        val missing = File(tempFolder.root, "not_found.txt")
        assertThrows(IllegalArgumentException::class.java) {
            TxtParser.validateAndParse(missing)
        }
    }
}
