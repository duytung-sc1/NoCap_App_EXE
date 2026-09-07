package com.nocap.app.data.parser

import com.nocap.app.domain.model.TextDocument
import com.nocap.app.domain.model.TextDocumentBlock
import com.nocap.app.domain.model.TextSpan
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets

object MarkdownParser {

    fun validateAndParse(file: File, suggestedTitle: String? = null): TextDocument {
        if (!file.exists()) {
            throw IllegalArgumentException("Tệp Markdown không tồn tại: ${file.name}")
        }

        val text = FileInputStream(file).use { fis ->
            fis.bufferedReader(StandardCharsets.UTF_8).readText()
        }

        return parseString(text, suggestedTitle ?: file.nameWithoutExtension)
    }

    fun parseString(content: String, fallbackTitle: String = "Tài liệu Markdown"): TextDocument {
        val lines = content.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val blocks = mutableListOf<TextDocumentBlock>()
        var extractedTitle: String? = null

        var inCodeBlock = false
        var codeLanguage: String? = null
        val codeBuffer = StringBuilder()

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            // Fenced code block check
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    blocks.add(TextDocumentBlock.CodeBlock(codeBuffer.toString().trimEnd(), codeLanguage))
                    codeBuffer.clear()
                    codeLanguage = null
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                    codeLanguage = trimmed.removePrefix("```").trim().takeIf { it.isNotEmpty() }
                }
                i++
                continue
            }

            if (inCodeBlock) {
                if (codeBuffer.isNotEmpty()) codeBuffer.append("\n")
                codeBuffer.append(line)
                i++
                continue
            }

            // Empty line
            if (trimmed.isEmpty()) {
                i++
                continue
            }

            // Horizontal rule
            if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
                blocks.add(TextDocumentBlock.Divider)
                i++
                continue
            }

            // Headings
            if (trimmed.startsWith("#")) {
                val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 6)
                val headingText = trimmed.drop(level).trim()
                if (level == 1 && extractedTitle == null) {
                    extractedTitle = headingText
                }
                blocks.add(TextDocumentBlock.Heading(level, headingText))
                i++
                continue
            }

            // Blockquote
            if (trimmed.startsWith(">")) {
                val quoteText = trimmed.removePrefix(">").trim()
                blocks.add(TextDocumentBlock.Quote(parseInlineSpans(quoteText)))
                i++
                continue
            }

            // Unordered list
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ")) {
                val itemText = trimmed.drop(2).trim()
                blocks.add(TextDocumentBlock.ListItem(ordered = false, index = 0, spans = parseInlineSpans(itemText)))
                i++
                continue
            }

            // Ordered list: e.g. "1. "
            val orderedMatch = Regex("""^(\d+)\.\s+(.*)$""").find(trimmed)
            if (orderedMatch != null) {
                val num = orderedMatch.groupValues[1].toIntOrNull() ?: 1
                val itemText = orderedMatch.groupValues[2]
                blocks.add(TextDocumentBlock.ListItem(ordered = true, index = num, spans = parseInlineSpans(itemText)))
                i++
                continue
            }

            // Regular paragraph (accumulate continuation lines)
            val paragraphBuilder = StringBuilder(line)
            while (i + 1 < lines.size) {
                val next = lines[i + 1]
                val nextTrimmed = next.trim()
                if (nextTrimmed.isEmpty() || nextTrimmed.startsWith("#") ||
                    nextTrimmed.startsWith("```") || nextTrimmed.startsWith("- ") ||
                    nextTrimmed.startsWith("* ") || nextTrimmed.startsWith(">") ||
                    nextTrimmed == "---" || Regex("""^(\d+)\.\s+""").containsMatchIn(nextTrimmed)) {
                    break
                }
                paragraphBuilder.append(" ").append(nextTrimmed)
                i++
            }

            blocks.add(TextDocumentBlock.Paragraph(parseInlineSpans(paragraphBuilder.toString().trim())))
            i++
        }

        if (inCodeBlock && codeBuffer.isNotEmpty()) {
            blocks.add(TextDocumentBlock.CodeBlock(codeBuffer.toString().trimEnd(), codeLanguage))
        }

        val finalTitle = extractedTitle?.ifBlank { null } ?: fallbackTitle.ifBlank { "Tài liệu Markdown" }
        return TextDocument(
            title = finalTitle,
            blocks = if (blocks.isEmpty()) listOf(TextDocumentBlock.Paragraph("")) else blocks
        )
    }

    /**
     * Parses inline markdown tokens: bold (**text**), italic (*text*), code (`text`), links ([text](url))
     */
    fun parseInlineSpans(text: String): List<TextSpan> {
        if (text.isEmpty()) return emptyList()

        val spans = mutableListOf<TextSpan>()
        // Regex matching links: [text](url), bold: \*\*([^\*]+)\*\*, italic: \*([^\*]+)\*, code: `([^`]+)`
        val tokenRegex = Regex("""(\[(.+?)\]\((https?://[^\s)]+|[^\s)]+)\))|(\*\*(.+?)\*\*)|(\*(.+?)\*)|(`(.+?)`)""")
        var lastIndex = 0

        for (match in tokenRegex.findAll(text)) {
            if (match.range.first > lastIndex) {
                spans.add(TextSpan(text.substring(lastIndex, match.range.first)))
            }

            val linkGroup = match.groups[1]
            val boldGroup = match.groups[4]
            val italicGroup = match.groups[6]
            val codeGroup = match.groups[8]

            when {
                linkGroup != null -> {
                    val linkText = match.groups[2]?.value ?: ""
                    val linkUrl = match.groups[3]?.value
                    spans.add(TextSpan(text = linkText, isUnderline = true, linkUrl = linkUrl))
                }
                boldGroup != null -> {
                    val boldText = match.groups[5]?.value ?: ""
                    spans.add(TextSpan(text = boldText, isBold = true))
                }
                italicGroup != null -> {
                    val italicText = match.groups[7]?.value ?: ""
                    spans.add(TextSpan(text = italicText, isItalic = true))
                }
                codeGroup != null -> {
                    val codeText = match.groups[9]?.value ?: ""
                    spans.add(TextSpan(text = codeText, isCode = true))
                }
            }

            lastIndex = match.range.last + 1
        }

        if (lastIndex < text.length) {
            spans.add(TextSpan(text.substring(lastIndex)))
        }

        return if (spans.isEmpty()) listOf(TextSpan(text)) else spans
    }
}
