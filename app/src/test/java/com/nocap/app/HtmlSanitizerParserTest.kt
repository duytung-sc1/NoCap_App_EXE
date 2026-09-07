package com.nocap.app

import com.nocap.app.data.parser.HtmlSanitizerParser
import com.nocap.app.domain.model.TextDocumentBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class HtmlSanitizerParserTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `test sanitize strips scripts, iframes, and forms`() {
        val rawHtml = """
            <!DOCTYPE html>
            <html>
            <head><title>Trang web thử nghiệm</title></head>
            <body>
                <script>alert('xss');</script>
                <script src="https://evil.com/hack.js"></script>
                <iframe src="https://evil.com"></iframe>
                <form action="/login"><input type="password"/></form>
                <h1>Tiêu đề an toàn</h1>
                <p>Nội dung an toàn.</p>
            </body>
            </html>
        """.trimIndent()

        val doc = HtmlSanitizerParser.parseString(rawHtml)
        assertEquals("Trang web thử nghiệm", doc.title)

        // Verify no script or iframe content leaked
        val allText = doc.blocks.joinToString(" ") { block ->
            when (block) {
                is TextDocumentBlock.Heading -> block.text
                is TextDocumentBlock.Paragraph -> block.spans.joinToString("") { it.text }
                else -> ""
            }
        }

        assertFalse(allText.contains("alert"))
        assertFalse(allText.contains("hack.js"))
        assertFalse(allText.contains("evil.com"))
        assertTrue(allText.contains("Tiêu đề an toàn"))
        assertTrue(allText.contains("Nội dung an toàn."))
    }

    @Test
    fun `test sanitize strips event handlers and javascript urls`() {
        val html = """
            <div>
                <p onclick="alert(1)" onmouseover="evil()">Đoạn văn có event handler</p>
                <p><a href="javascript:alert('pwned')">Bấm vào đây</a></p>
            </div>
        """.trimIndent()

        val doc = HtmlSanitizerParser.parseString(html)
        val p1 = doc.blocks[0] as TextDocumentBlock.Paragraph
        assertEquals("Đoạn văn có event handler", p1.spans[0].text)

        val p2 = doc.blocks[1] as TextDocumentBlock.Paragraph
        val linkSpan = p2.spans.find { it.text == "Bấm vào đây" }
        assertNotNull(linkSpan)
        assertEquals("#", linkSpan?.linkUrl)
    }

    @Test
    fun `test html entities are properly decoded`() {
        val html = "<p>&lt;Bản tin &amp; Thông báo&gt; &quot;Đặc biệt&quot; &#39;2026&#39; &nbsp;</p>"
        val doc = HtmlSanitizerParser.parseString(html)
        val p = doc.blocks[0] as TextDocumentBlock.Paragraph
        val text = p.spans.joinToString("") { it.text }
        assertTrue(text.contains("<Bản tin & Thông báo> \"Đặc biệt\" '2026'"))
    }

    @Test
    fun `test table parsing`() {
        val html = """
            <table>
                <tr><th>STT</th><th>Họ tên</th></tr>
                <tr><td>1</td><td>Nguyễn Văn A</td></tr>
                <tr><td>2</td><td>Trần Thị B</td></tr>
            </table>
        """.trimIndent()

        val doc = HtmlSanitizerParser.parseString(html)
        val table = doc.blocks.find { it is TextDocumentBlock.Table } as TextDocumentBlock.Table
        assertEquals(3, table.rows.size)
        assertEquals(listOf("STT", "Họ tên"), table.rows[0])
        assertEquals(listOf("1", "Nguyễn Văn A"), table.rows[1])
        assertEquals(listOf("2", "Trần Thị B"), table.rows[2])
    }

    @Test
    fun `test lists parsing`() {
        val html = """
            <ul>
                <li>Mục A</li>
                <li>Mục B</li>
            </ul>
            <ol>
                <li>Bước 1</li>
                <li>Bước 2</li>
            </ol>
        """.trimIndent()

        val doc = HtmlSanitizerParser.parseString(html)
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
    fun `test validateAndParse from file`() {
        val file = tempFolder.newFile("article.html")
        file.writeText("<html><head><title>Bài Báo</title></head><body><h1>Đầu đề</h1><p>Nội dung</p></body></html>")

        val doc = HtmlSanitizerParser.validateAndParse(file)
        assertEquals("Bài Báo", doc.title)
    }
}
