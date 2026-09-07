package com.nocap.app.domain.model

data class TextDocument(
    val title: String,
    val author: String? = null,
    val blocks: List<TextDocumentBlock> = emptyList(),
    val warningMessage: String? = null
)

sealed interface TextDocumentBlock {
    val plainText: String

    data class Heading(val level: Int, val text: String) : TextDocumentBlock {
        override val plainText: String get() = text
    }

    data class Paragraph(val spans: List<TextSpan>) : TextDocumentBlock {
        constructor(text: String) : this(listOf(TextSpan(text)))
        override val plainText: String get() = spans.joinToString("") { it.text }
    }

    data class ListItem(val ordered: Boolean, val index: Int, val spans: List<TextSpan>) : TextDocumentBlock {
        constructor(ordered: Boolean, index: Int, text: String) : this(ordered, index, listOf(TextSpan(text)))
        override val plainText: String get() = spans.joinToString("") { it.text }
    }

    data class Quote(val spans: List<TextSpan>) : TextDocumentBlock {
        constructor(text: String) : this(listOf(TextSpan(text)))
        override val plainText: String get() = spans.joinToString("") { it.text }
    }

    data class CodeBlock(val code: String, val language: String? = null) : TextDocumentBlock {
        override val plainText: String get() = code
    }

    data class Table(val rows: List<List<String>>) : TextDocumentBlock {
        override val plainText: String get() = rows.joinToString("\n") { row -> row.joinToString(" | ") }
    }

    data class ImageBlock(val localPath: String?, val altText: String? = null) : TextDocumentBlock {
        override val plainText: String get() = altText ?: "[Ảnh]"
    }

    object Divider : TextDocumentBlock {
        override val plainText: String get() = "---"
    }
}

data class TextSpan(
    val text: String,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isCode: Boolean = false,
    val linkUrl: String? = null
)
