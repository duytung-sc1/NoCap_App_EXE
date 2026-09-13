package com.nocap.app.domain.model

import org.json.JSONObject

sealed interface DocumentLocator {
    val progression: Float

    fun toJson(): String

    companion object {
        fun fromJson(jsonStr: String): DocumentLocator? {
            if (jsonStr.isBlank()) return null
            return try {
                val json = JSONObject(jsonStr)
                when (json.optString("type")) {
                    "TEXT" -> TextLocator(
                        blockIndex = json.optInt("blockIndex", 0),
                        characterOffset = json.optInt("characterOffset", 0),
                        scrollOffsetPx = json.optInt("scrollOffsetPx", 0).coerceAtLeast(0),
                        progression = json.optDouble("progression", 0.0).toFloat(),
                        snippet = json.optString("snippet").takeIf { it.isNotEmpty() }
                    )
                    "ARCHIVE" -> ArchiveLocator(
                        pageIndex = json.optInt("pageIndex", 0),
                        entryName = json.optString("entryName", ""),
                        progression = json.optDouble("progression", 0.0).toFloat()
                    )
                    "IMAGE" -> ImageLocator(
                        progression = json.optDouble("progression", 0.0).toFloat()
                    )
                    "PDF" -> PdfAnnotationLocator(
                        pageIndex = json.optInt("pageIndex", 0),
                        pageNumber = json.optInt("pageNumber", json.optInt("pageIndex", 0) + 1),
                        progression = json.optDouble("progression", 0.0).toFloat(),
                        selectedText = json.optString("selectedText", ""),
                        startOffset = json.optInt("startOffset", 0),
                        endOffset = json.optInt("endOffset", 0),
                        contextSnippet = json.optString("contextSnippet").takeIf { it.isNotEmpty() }
                    )
                    else -> null
                }
            } catch (_: Exception) {
                null
            }
        }
    }
}

data class TextLocator(
    val blockIndex: Int = 0,
    val characterOffset: Int = 0,
    override val progression: Float = 0f,
    val snippet: String? = null,
    val scrollOffsetPx: Int = 0
) : DocumentLocator {
    override fun toJson(): String = JSONObject().apply {
        put("type", "TEXT")
        put("version", 1)
        put("blockIndex", blockIndex)
        put("characterOffset", characterOffset)
        if (scrollOffsetPx > 0) put("scrollOffsetPx", scrollOffsetPx)
        put("progression", progression.toDouble())
        if (!snippet.isNullOrBlank()) put("snippet", snippet)
    }.toString()

    companion object {
        fun fromJson(jsonStr: String): TextLocator? =
            DocumentLocator.fromJson(jsonStr) as? TextLocator
    }
}

data class ArchiveLocator(
    val pageIndex: Int = 0,
    val entryName: String = "",
    override val progression: Float = 0f
) : DocumentLocator {
    override fun toJson(): String = JSONObject().apply {
        put("type", "ARCHIVE")
        put("version", 1)
        put("pageIndex", pageIndex)
        put("entryName", entryName)
        put("progression", progression.toDouble())
    }.toString()

    companion object {
        fun fromJson(jsonStr: String): ArchiveLocator? =
            DocumentLocator.fromJson(jsonStr) as? ArchiveLocator
    }
}

data class ImageLocator(
    override val progression: Float = 0f
) : DocumentLocator {
    override fun toJson(): String = JSONObject().apply {
        put("type", "IMAGE")
        put("version", 1)
        put("progression", progression.toDouble())
    }.toString()

    companion object {
        fun fromJson(jsonStr: String): ImageLocator? =
            DocumentLocator.fromJson(jsonStr) as? ImageLocator
    }
}

data class PdfAnnotationLocator(
    val pageIndex: Int = 0,
    val pageNumber: Int = pageIndex + 1,
    override val progression: Float = 0f,
    val selectedText: String = "",
    val startOffset: Int = 0,
    val endOffset: Int = 0,
    val contextSnippet: String? = null
) : DocumentLocator {
    override fun toJson(): String = JSONObject().apply {
        put("type", "PDF")
        put("version", 1)
        put("pageIndex", pageIndex)
        put("pageNumber", pageNumber)
        put("progression", progression.toDouble())
        put("selectedText", selectedText)
        put("startOffset", startOffset)
        put("endOffset", endOffset)
        if (!contextSnippet.isNullOrBlank()) put("contextSnippet", contextSnippet)
    }.toString()

    companion object {
        fun fromJson(jsonStr: String): PdfAnnotationLocator? =
            DocumentLocator.fromJson(jsonStr) as? PdfAnnotationLocator
    }
}
