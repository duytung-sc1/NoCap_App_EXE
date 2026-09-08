package com.nocap.app.data.parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.safety.Safelist

/** Extract remote HTML before persistence. Never follows links or executes page scripts. */
object WebArticleExtractor {
    private val noise = Regex("(^|[-_\\s])(ads?|advert(?:isement|ising)?|banner|promo|related|recommend(?:ed|ations)?|comments?|social|share|breadcrumb|pagination|chapter-nav|menu|sidebar|cookie|popup|modal)([-_\\s]|$)", RegexOption.IGNORE_CASE)
    private val allowed = Safelist.none().addTags("p", "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "pre", "code", "ul", "ol", "li", "table", "thead", "tbody", "tr", "th", "td", "br", "hr", "strong", "b", "em", "i", "u")

    fun extract(html: String, fallbackTitle: String = "Tài liệu từ liên kết"): String {
        require(html.length <= 8 * 1024 * 1024) { "Trang HTML quá lớn để trích xuất nội dung." }
        val doc = Jsoup.parse(html)
        val title = doc.selectFirst("meta[property=og:title]")?.attr("content")?.takeIf { it.isNotBlank() }
            ?: doc.title().takeIf { it.isNotBlank() } ?: fallbackTitle
        doc.select("script, style, noscript, template, iframe, object, embed, form, input, button, select, nav, aside, footer, header, [hidden], [aria-hidden=true]").remove()
        doc.getAllElements().toList().filter { e ->
            noise.containsMatchIn(e.id() + " " + e.className()) ||
                Regex("(?:display\\s*:\\s*none|visibility\\s*:\\s*hidden)", RegexOption.IGNORE_CASE).containsMatchIn(e.attr("style"))
        }.forEach { it.remove() }

        fun score(e: Element): Double {
            val size = e.text().length
            val links = e.select("a").sumOf { it.text().length }
            if (size < 160 || links > size * .45) return 0.0
            return (size - links).toDouble() / (1 + e.select("div, section, article").size * .04)
        }
        val explicit = doc.select(".chapter-content, #chapter-content, #chapterContent, .chapter-text, #chapterText, [itemprop=articleBody], .entry-content, .post-content, article")
        val root = explicit.filter { score(it) > 0 }.maxByOrNull { score(it) }
            ?: doc.select("main, [role=main], #content, .content, section, div").filter { score(it) > 0 }.maxByOrNull { score(it) }
            ?: doc.body().takeIf { score(it) > 0 }
            ?: throw IllegalArgumentException("Không xác định được nội dung chính. Trang có thể yêu cầu đăng nhập hoặc chỉ chứa liên kết.")

        // Link-only blocks are navigation/promotion; inline linked words remain as plain text.
        root.select("p, li, div, section").toList().filter { e ->
            val text = e.text(); text.isNotBlank() && e.select("a").sumOf { it.text().length } >= text.length * .8
        }.forEach { it.remove() }
        root.select("a").unwrap()
        root.select("img, video, audio, source, svg, canvas").remove()
        // Convert div/br-based prose into paragraphs so the document reader keeps line breaks.
        val blockTags = setOf("p", "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "pre", "ul", "ol", "table", "hr")
        fun normalize(container: Element) {
            var paragraph: Element? = null
            for (child in container.childNodes().toList()) {
                if (child is Element && child.normalName() in setOf("div", "section", "article", "main")) {
                    paragraph = null; normalize(child); child.unwrap()
                } else if (child is Element && child.normalName() in blockTags) {
                    paragraph = null
                } else if (child is Element && child.normalName() == "br") {
                    paragraph = null; child.remove()
                } else if (child !is TextNode || !child.isBlank) {
                    if (paragraph == null) { paragraph = Element("p"); child.before(paragraph) }
                    paragraph.appendChild(child)
                }
            }
        }
        normalize(root)
        val clean = Jsoup.clean(root.html(), allowed)
        val output = Jsoup.parseBodyFragment(clean)
        require(output.body().text().length >= 160) { "Trang không có đủ nội dung văn bản để đọc." }
        output.head().appendElement("meta").attr("charset", "utf-8")
        output.title(title)
        output.outputSettings().prettyPrint(false)
        return "<!DOCTYPE html>" + output.outerHtml()
    }
}
