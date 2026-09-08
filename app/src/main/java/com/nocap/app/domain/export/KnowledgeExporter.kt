package com.nocap.app.domain.export

import com.nocap.app.core.database.entity.BookmarkEntity
import com.nocap.app.core.database.entity.CatalogBookEntity
import com.nocap.app.core.database.entity.HighlightEntity
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object KnowledgeExporter {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    fun formatDocumentKnowledge(
        book: CatalogBookEntity,
        highlights: List<HighlightEntity>,
        bookmarks: List<BookmarkEntity>
    ): String {
        val sb = StringBuilder()
        val title = book.userTitleOverride ?: book.title
        val author = book.userAuthorOverride ?: book.author
        val nowFormatted = dateFormat.format(Date())

        sb.appendLine("# $title")
        sb.appendLine("- **Tác giả:** $author")
        sb.appendLine("- **Định dạng:** ${book.format.name}")
        sb.appendLine("- **Thời gian xuất:** $nowFormatted")
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()

        val notes = highlights.filter { !it.note.isNullOrBlank() }
        val plainHighlights = highlights.filter { it.note.isNullOrBlank() }

        if (notes.isNotEmpty()) {
            sb.appendLine("## 📝 Ghi chú (${notes.size})")
            sb.appendLine()
            notes.forEachIndexed { index, item ->
                sb.appendLine("### ${index + 1}. ${item.note}")
                sb.appendLine("> ${item.text.trim()}")
                sb.appendLine()
                sb.appendLine("*Màu sắc: ${item.color} | Tạo lúc: ${dateFormat.format(Date(item.createdAt))}*")
                sb.appendLine()
            }
            sb.appendLine("---")
            sb.appendLine()
        }

        if (plainHighlights.isNotEmpty()) {
            sb.appendLine("## 💡 Đoạn trích (${plainHighlights.size})")
            sb.appendLine()
            plainHighlights.forEachIndexed { index, item ->
                sb.appendLine("${index + 1}. > ${item.text.trim()}")
                sb.appendLine("   *Màu sắc: ${item.color} | Tạo lúc: ${dateFormat.format(Date(item.createdAt))}*")
                sb.appendLine()
            }
            sb.appendLine("---")
            sb.appendLine()
        }

        val activeBookmarks = bookmarks.filter { !it.isDeleted }
        if (activeBookmarks.isNotEmpty()) {
            sb.appendLine("## 🔖 Đánh dấu trang (${activeBookmarks.size})")
            sb.appendLine()
            activeBookmarks.forEachIndexed { index, item ->
                sb.appendLine("${index + 1}. **${item.chapterTitle}**")
                if (!item.snippet.isNullOrBlank()) {
                    sb.appendLine("   *\"${item.snippet.trim()}\"*")
                }
                sb.appendLine("   *Tạo lúc: ${dateFormat.format(Date(item.createdAt))}*")
                sb.appendLine()
            }
        }

        return sb.toString()
    }

    fun formatAllKnowledge(
        booksWithData: List<Triple<CatalogBookEntity, List<HighlightEntity>, List<BookmarkEntity>>>
    ): String {
        val sb = StringBuilder()
        val nowFormatted = dateFormat.format(Date())

        sb.appendLine("# NoCap — Tổng hợp Ghi chú & Bộ nhớ đọc")
        sb.appendLine("- **Ngày xuất:** $nowFormatted")
        sb.appendLine("- **Tổng số tài liệu có ghi chú:** ${booksWithData.size}")
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()

        for ((book, highlights, bookmarks) in booksWithData) {
            val content = formatDocumentKnowledge(book, highlights, bookmarks)
            sb.appendLine(content)
            sb.appendLine()
            sb.appendLine("==========================================")
            sb.appendLine()
        }

        return sb.toString()
    }

    fun writeMarkdownToStream(outputStream: OutputStream, markdown: String) {
        OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
            writer.write(markdown)
            writer.flush()
        }
    }
}
