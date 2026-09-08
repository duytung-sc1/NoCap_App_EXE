package com.nocap.app.data.parser

import com.nocap.app.domain.model.TextDocument
import com.nocap.app.domain.model.TextDocumentBlock
import com.nocap.app.domain.model.TextSpan
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

object HtmlSanitizerParser {

    private val DANGEROUS_TAGS = listOf(
        "script", "iframe", "object", "embed", "applet", "form", "base", "input", "button"
    )

    fun validateAndParse(file: File, suggestedTitle: String? = null, mainContentOnly: Boolean = false): TextDocument {
        if (!file.exists()) {
            throw IllegalArgumentException("Tệp HTML không tồn tại: ${file.name}")
        }
        if (mainContentOnly && file.length() > 8L * 1024 * 1024) {
            throw IllegalArgumentException("Trang HTML quá lớn để trích xuất nội dung.")
        }

        val rawHtml = FileInputStream(file).use { fis ->
            fis.bufferedReader(StandardCharsets.UTF_8).readText()
        }

        val readableHtml = if (mainContentOnly) WebArticleExtractor.extract(rawHtml, suggestedTitle ?: file.nameWithoutExtension) else rawHtml
        return parseString(readableHtml, suggestedTitle ?: file.nameWithoutExtension)
    }

    fun parseString(rawHtml: String, fallbackTitle: String = "Tài liệu HTML"): TextDocument {
        // 1. Sanitize: remove dangerous tags and their contents
        var sanitized = rawHtml
        for (tag in DANGEROUS_TAGS) {
            val pattern = Pattern.compile("<$tag\\b[^>]*>.*?</$tag>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
            sanitized = pattern.matcher(sanitized).replaceAll("")
            // Also self-closing or unclosed
            val selfClosing = Pattern.compile("<$tag\\b[^>]*>", Pattern.CASE_INSENSITIVE)
            sanitized = selfClosing.matcher(sanitized).replaceAll("")
        }

        // Remove dangerous event handlers (e.g. onclick=..., onload=...)
        val eventHandlerPattern = Pattern.compile("\\s+on[a-zA-Z]+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)", Pattern.CASE_INSENSITIVE)
        sanitized = eventHandlerPattern.matcher(sanitized).replaceAll("")

        // Remove javascript: URLs
        val jsHrefPattern = Pattern.compile("href\\s*=\\s*(\"javascript:[^\"]*\"|'javascript:[^']*')", Pattern.CASE_INSENSITIVE)
        sanitized = jsHrefPattern.matcher(sanitized).replaceAll("href=\"#\"")

        // 2. Extract title: <title>...</title>
        var extractedTitle: String? = null
        val titleMatcher = Pattern.compile("<title\\b[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL).matcher(sanitized)
        if (titleMatcher.find()) {
            val titleText = decodeHtmlEntities(titleMatcher.group(1) ?: "").trim()
            if (titleText.isNotBlank()) {
                extractedTitle = titleText
            }
        }

        // 3. Parse readable blocks
        val blocks = mutableListOf<TextDocumentBlock>()

        // Extract body content if <body> tag exists
        val bodyMatcher = Pattern.compile("<body\\b[^>]*>(.*?)</body>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL).matcher(sanitized)
        val content = if (bodyMatcher.find()) bodyMatcher.group(1) ?: sanitized else sanitized

        // Pattern matching block-level elements: h1..h6, p, blockquote, pre, ul, ol, table
        val blockRegex = Regex("""<(h[1-6]|p|blockquote|pre|table|ul|ol|hr)\b[^>]*>(.*?)(?:</\1>|$)""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val matches = blockRegex.findAll(content).toList()

        if (matches.isNotEmpty()) {
            for (match in matches) {
                val tag = match.groupValues[1].lowercase()
                val inner = match.groupValues[2]

                when {
                    tag.startsWith("h") -> {
                        val level = tag.drop(1).toIntOrNull()?.coerceIn(1, 6) ?: 1
                        val text = stripTags(inner).trim()
                        if (text.isNotEmpty()) {
                            if (level == 1 && extractedTitle == null) {
                                extractedTitle = text
                            }
                            blocks.add(TextDocumentBlock.Heading(level, text))
                        }
                    }
                    tag == "p" -> {
                        val spans = parseFormattedHtmlSpans(inner)
                        if (spans.any { it.text.isNotBlank() }) {
                            blocks.add(TextDocumentBlock.Paragraph(spans))
                        }
                    }
                    tag == "blockquote" -> {
                        val spans = parseFormattedHtmlSpans(inner)
                        if (spans.any { it.text.isNotBlank() }) {
                            blocks.add(TextDocumentBlock.Quote(spans))
                        }
                    }
                    tag == "pre" -> {
                        val codeText = stripTags(inner).trimEnd()
                        if (codeText.isNotEmpty()) {
                            blocks.add(TextDocumentBlock.CodeBlock(codeText))
                        }
                    }
                    tag == "hr" -> {
                        blocks.add(TextDocumentBlock.Divider)
                    }
                    tag == "ul" -> {
                        val liRegex = Regex("""<li\b[^>]*>(.*?)(?:</li>|$)""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                        for (li in liRegex.findAll(inner)) {
                            val spans = parseFormattedHtmlSpans(li.groupValues[1])
                            if (spans.any { it.text.isNotBlank() }) {
                                blocks.add(TextDocumentBlock.ListItem(ordered = false, index = 0, spans = spans))
                            }
                        }
                    }
                    tag == "ol" -> {
                        val liRegex = Regex("""<li\b[^>]*>(.*?)(?:</li>|$)""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                        var index = 1
                        for (li in liRegex.findAll(inner)) {
                            val spans = parseFormattedHtmlSpans(li.groupValues[1])
                            if (spans.any { it.text.isNotBlank() }) {
                                blocks.add(TextDocumentBlock.ListItem(ordered = true, index = index++, spans = spans))
                            }
                        }
                    }
                    tag == "table" -> {
                        val rowList = mutableListOf<List<String>>()
                        val trRegex = Regex("""<tr\b[^>]*>(.*?)(?:</tr>|$)""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                        val cellRegex = Regex("""<(td|th)\b[^>]*>(.*?)(?:</\1>|$)""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                        for (tr in trRegex.findAll(inner)) {
                            val cells = cellRegex.findAll(tr.groupValues[1]).map { stripTags(it.groupValues[2]).trim() }.toList()
                            if (cells.isNotEmpty()) {
                                rowList.add(cells)
                            }
                        }
                        if (rowList.isNotEmpty()) {
                            blocks.add(TextDocumentBlock.Table(rowList))
                        }
                    }
                }
            }
        } else {
            // Fallback: strip tags and split into paragraphs
            val plain = stripTags(content)
            for (p in plain.split("\n\n")) {
                val trimmed = p.trim()
                if (trimmed.isNotEmpty()) {
                    blocks.add(TextDocumentBlock.Paragraph(listOf(TextSpan(trimmed))))
                }
            }
        }

        val finalTitle = extractedTitle ?: fallbackTitle.ifBlank { "Tài liệu HTML" }
        return TextDocument(
            title = finalTitle,
            blocks = if (blocks.isEmpty()) listOf(TextDocumentBlock.Paragraph("")) else blocks
        )
    }

    fun stripTags(html: String): String {
        val withoutTags = html.replace(Regex("<[^>]+>"), " ")
        return decodeHtmlEntities(withoutTags).replace(Regex("""\s+"""), " ")
    }

    private fun parseFormattedHtmlSpans(html: String): List<TextSpan> {
        val spans = mutableListOf<TextSpan>()
        val tagRegex = Regex("""<(/?)([a-zA-Z0-9]+)(\s+[^>]*)?>""")
        var lastIdx = 0

        var isBold = false
        var isItalic = false
        var isUnderline = false
        var isCode = false
        var currentLink: String? = null

        val matches = tagRegex.findAll(html)
        for (m in matches) {
            if (m.range.first > lastIdx) {
                val textChunk = decodeHtmlEntities(html.substring(lastIdx, m.range.first))
                if (textChunk.isNotEmpty()) {
                    spans.add(
                        TextSpan(
                            text = textChunk,
                            isBold = isBold,
                            isItalic = isItalic,
                            isUnderline = isUnderline || currentLink != null,
                            isCode = isCode,
                            linkUrl = currentLink
                        )
                    )
                }
            }

            val isClosing = m.groupValues[1] == "/"
            val tagName = m.groupValues[2].lowercase()
            val attributes = m.groupValues[3]

            when (tagName) {
                "br" -> spans.add(TextSpan("\n"))
                "b", "strong" -> isBold = !isClosing
                "i", "em" -> isItalic = !isClosing
                "u" -> isUnderline = !isClosing
                "code" -> isCode = !isClosing
                "a" -> {
                    if (isClosing) {
                        currentLink = null
                    } else {
                        val hrefMatch = Regex("""href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(attributes)
                        currentLink = hrefMatch?.groupValues?.get(1)
                    }
                }
            }

            lastIdx = m.range.last + 1
        }

        if (lastIdx < html.length) {
            val textChunk = decodeHtmlEntities(html.substring(lastIdx))
            if (textChunk.isNotEmpty()) {
                spans.add(
                    TextSpan(
                        text = textChunk,
                        isBold = isBold,
                        isItalic = isItalic,
                        isUnderline = isUnderline || currentLink != null,
                        isCode = isCode,
                        linkUrl = currentLink
                    )
                )
            }
        }

        return if (spans.isEmpty()) listOf(TextSpan("")) else spans
    }

    fun decodeHtmlEntities(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
    }
}
