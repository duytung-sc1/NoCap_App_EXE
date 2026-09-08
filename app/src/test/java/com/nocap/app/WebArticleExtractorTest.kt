package com.nocap.app

import com.nocap.app.data.parser.WebArticleExtractor
import com.nocap.app.data.parser.HtmlSanitizerParser
import com.nocap.app.domain.model.TextDocumentBlock
import org.jsoup.Jsoup
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class WebArticleExtractorTest {
    private val prose = "Đây là đoạn nội dung chính của chương sách. Nhân vật đang kể lại hành trình và những điều đã trải qua. ".repeat(4)
    @Test fun `HTTPS downloader persists only cleaned article`() = kotlinx.coroutines.runBlocking {
        val folder = java.nio.file.Files.createTempDirectory("article-download-test").toFile()
        try {
            val client=okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
                okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1).code(200).message("OK")
                    .header("Content-Type","text/html; charset=utf-8")
                    .body(okhttp3.ResponseBody.create(null as okhttp3.MediaType?, "<title>Kiểm thử</title><article><p>$prose</p></article><footer><a href='https://ad.test'>QUẢNG CÁO</a></footer>"))
                    .build()
            }.build()
            val result=com.nocap.app.data.importer.RemotePublicationDownloader(baseClient=client,cacheDirectory=folder).download("https://example.org/chapter").getOrThrow()
            val stored=result.tempFile.readText()
            assertTrue(stored.contains(prose.trim()))
            assertFalse(stored.contains("QUẢNG CÁO"))
            assertFalse(stored.contains("href="))
            assertEquals(result.tempFile.length(),result.totalBytes)
        } finally { folder.deleteRecursively() }
    }
    @Test fun `keeps article and formatting but drops page chrome and all active links`() {
        val html = """<title>Chương thử</title><nav>MENU</nav><main><article class="chapter-content"><p><b>Mở đầu</b> $prose <a href="https://example.org">từ trong câu</a></p><div class="ads"><a href="https://ad.test">QUẢNG CÁO</a></div><p hidden>NỘI DUNG ẨN</p><p>$prose</p></article><section class="related">GỢI Ý</section></main><footer>CHÂN TRANG</footer>"""
        val clean = WebArticleExtractor.extract(html)
        val doc = Jsoup.parse(clean)
        assertEquals("Chương thử", doc.title())
        assertEquals(2, doc.select("p").size)
        assertTrue(doc.text().contains("từ trong câu"))
        assertFalse(doc.text().contains("QUẢNG CÁO"))
        assertFalse(doc.text().contains("NỘI DUNG ẨN"))
        assertFalse(doc.text().contains("MENU"))
        assertFalse(doc.text().contains("GỢI Ý"))
        assertTrue(doc.select("a, script, img, iframe, [href], [src], [onclick]").isEmpty())
        assertEquals(1, doc.select("b").size)
    }
    @Test fun `div and br prose remains separate readable paragraphs`() {
        val clean = WebArticleExtractor.extract("<main><div>$prose<br>$prose<div>$prose</div></div></main>")
        val parsed = HtmlSanitizerParser.parseString(clean)
        assertEquals(3, parsed.blocks.filterIsInstance<TextDocumentBlock.Paragraph>().size)
    }
    @Test fun `rejects login navigation and link directories`() {
        for (html in listOf("<form>Đăng nhập</form>", "<main><p><a href='/x'>${prose.repeat(5)}</a></p></main>")) {
            assertThrows(IllegalArgumentException::class.java) { WebArticleExtractor.extract(html) }
        }
    }
    @Test fun `preserves inline line breaks in reader`() {
        val parsed=HtmlSanitizerParser.parseString("<p>Dòng một<br>Dòng hai</p>")
        assertEquals("Dòng một\nDòng hai", (parsed.blocks.single() as TextDocumentBlock.Paragraph).spans.joinToString(""){it.text})
    }
    @Test fun `actual downloaded chapter retains every paragraph and excludes unrelated blocks`() {
        val source=File("../build/html-import-check/source.html")
        assumeTrue(source.exists())
        val raw=source.readText();val original=Jsoup.parse(raw).selectFirst("article.chapter-content")!!
        val clean=WebArticleExtractor.extract(raw);val doc=Jsoup.parse(clean)
        assertEquals(original.select("p").map{it.text()},doc.select("p").map{it.text()})
        assertEquals(57,doc.select("p").size)
        assertTrue(doc.select("a,[href],[src]").isEmpty())
        assertFalse(doc.text().contains("Có thể bạn cũng muốn đọc"))
        assertFalse(doc.text().contains("MB66"))
        assertFalse(doc.text().contains("Bình luận Facebook"))
        val parsed=HtmlSanitizerParser.parseString(clean)
        assertEquals(57,parsed.blocks.filterIsInstance<TextDocumentBlock.Paragraph>().size)
        File("../build/html-import-check/clean.html").writeText(clean)
    }
}
