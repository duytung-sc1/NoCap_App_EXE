package com.nocap.app

import com.nocap.app.data.parser.MarkdownParser
import com.nocap.app.domain.model.TextDocumentBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MarkdownParserTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `test markdown headings and title extraction`() {
        val markdown = """
            # Tiêu đề tài liệu
            
            Đây là phần mở đầu.
            
            ## Chương 1: Giới thiệu
            
            ### Mục 1.1: Chi tiết
        """.trimIndent()

        val doc = MarkdownParser.parseString(markdown)
        assertEquals("Tiêu đề tài liệu", doc.title)

        val h1 = doc.blocks[0] as TextDocumentBlock.Heading
        assertEquals(1, h1.level)
        assertEquals("Tiêu đề tài liệu", h1.text)

        val p = doc.blocks[1] as TextDocumentBlock.Paragraph
        assertEquals("Đây là phần mở đầu.", p.spans[0].text)

        val h2 = doc.blocks[2] as TextDocumentBlock.Heading
        assertEquals(2, h2.level)
        assertEquals("Chương 1: Giới thiệu", h2.text)

        val h3 = doc.blocks[3] as TextDocumentBlock.Heading
        assertEquals(3, h3.level)
        assertEquals("Mục 1.1: Chi tiết", h3.text)
    }

    @Test
    fun `test inline formatting spans`() {
        val line = "Văn bản **in đậm** và *in nghiêng* cùng với `code inline` và [liên kết](https://example.com)."
        val spans = MarkdownParser.parseInlineSpans(line)

        val boldSpan = spans.find { it.isBold }
        assertNotNull(boldSpan)
        assertEquals("in đậm", boldSpan?.text)

        val italicSpan = spans.find { it.isItalic }
        assertNotNull(italicSpan)
        assertEquals("in nghiêng", italicSpan?.text)

        val codeSpan = spans.find { it.isCode }
        assertNotNull(codeSpan)
        assertEquals("code inline", codeSpan?.text)

        val linkSpan = spans.find { it.linkUrl == "https://example.com" }
        assertNotNull(linkSpan)
        assertEquals("liên kết", linkSpan?.text)
        assertTrue(linkSpan?.isUnderline == true)
    }

    @Test
    fun `test fenced code blocks`() {
        val md = """
            ```kotlin
            fun main() {
                println("Hello Kotlin")
            }
            ```
        """.trimIndent()

        val doc = MarkdownParser.parseString(md)
        assertEquals(1, doc.blocks.size)

        val codeBlock = doc.blocks[0] as TextDocumentBlock.CodeBlock
        assertEquals("kotlin", codeBlock.language)
        assertTrue(codeBlock.code.contains("println(\"Hello Kotlin\")"))
    }

    @Test
    fun `test blockquote and lists`() {
        val md = """
            > Trích dẫn quan trọng
            
            - Mục danh sách 1
            - Mục danh sách 2
            
            1. Bước thứ nhất
            2. Bước thứ hai
        """.trimIndent()

        val doc = MarkdownParser.parseString(md)
        val quote = doc.blocks.find { it is TextDocumentBlock.Quote } as TextDocumentBlock.Quote
        assertEquals("Trích dẫn quan trọng", quote.spans[0].text)

        val listItems = doc.blocks.filterIsInstance<TextDocumentBlock.ListItem>()
        assertEquals(4, listItems.size)
        assertEquals(false, listItems[0].ordered)
        assertEquals(false, listItems[1].ordered)
        assertEquals(true, listItems[2].ordered)
        assertEquals(1, listItems[2].index)
        assertEquals(true, listItems[3].ordered)
        assertEquals(2, listItems[3].index)
    }

    @Test
    fun `test divider rule`() {
        val md = """
            Đoạn trên
            ---
            Đoạn dưới
        """.trimIndent()

        val doc = MarkdownParser.parseString(md)
        assertEquals(3, doc.blocks.size)
        assertTrue(doc.blocks[1] is TextDocumentBlock.Divider)
    }

    @Test
    fun `test validateAndParse from file`() {
        val file = tempFolder.newFile("guide.md")
        file.writeText("# Hướng dẫn sử dụng\n\nNội dung chi tiết.")

        val doc = MarkdownParser.validateAndParse(file)
        assertEquals("Hướng dẫn sử dụng", doc.title)
    }
}
