package com.nocap.app.domain.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.nocap.app.core.database.entity.BookmarkEntity
import com.nocap.app.core.database.entity.CatalogBookEntity
import com.nocap.app.core.database.entity.HighlightEntity
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfKnowledgeExporter {

    private const val PAGE_WIDTH = 595  // A4 width in points (72 dpi)
    private const val PAGE_HEIGHT = 842 // A4 height in points
    private const val MARGIN = 40f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2)
    private const val BOTTOM_LIMIT = PAGE_HEIGHT - MARGIN - 20f

    private fun formatDate(time: Long): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(time))

    fun exportToPdf(
        booksWithData: List<Triple<CatalogBookEntity, List<HighlightEntity>, List<BookmarkEntity>>>,
        outputStream: OutputStream,
        titleOverride: String? = null
    ) {
        val document = PdfDocument()
        try {
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            var y = MARGIN

            val titlePaint = TextPaint().apply {
                color = Color.BLACK
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val sectionPaint = TextPaint().apply {
                color = Color.rgb(26, 115, 232) // Material Blue
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val subSectionPaint = TextPaint().apply {
                color = Color.rgb(60, 64, 67)
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val bodyPaint = TextPaint().apply {
                color = Color.rgb(32, 33, 36)
                textSize = 10f
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            }

            val quotePaint = TextPaint().apply {
                color = Color.rgb(95, 99, 104)
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                isAntiAlias = true
            }

            val metaPaint = TextPaint().apply {
                color = Color.rgb(128, 134, 139)
                textSize = 8.5f
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            }

            val linePaint = Paint().apply {
                color = Color.rgb(218, 220, 224)
                strokeWidth = 1f
            }

            val accentBarPaint = Paint().apply {
                color = Color.rgb(26, 115, 232)
                strokeWidth = 3f
            }

            fun startNextPage() {
                val footer = "- Trang $pageNumber -"
                canvas.drawText(footer, (PAGE_WIDTH - bodyPaint.measureText(footer)) / 2f, PAGE_HEIGHT - 20f, metaPaint)
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = MARGIN
            }

            fun checkPageBreak(requiredHeight: Float) {
                if (y + requiredHeight > BOTTOM_LIMIT) startNextPage()
            }

            fun drawTextLayout(
                text: String,
                paint: TextPaint,
                xOffset: Float = 0f,
                width: Float = CONTENT_WIDTH,
                decoration: ((Canvas, Float, Float) -> Unit)? = null
            ): Float {
                if (text.isEmpty()) return 0f
                val paragraphs = if (text.contains("\n")) text.split("\n") else listOf(text)
                var totalHeight = 0f
                for (p in paragraphs) {
                    if (p.isEmpty()) {
                        checkPageBreak(4f)
                        y += 4f
                        totalHeight += 4f
                        continue
                    }
                    var remaining = p
                    while (remaining.isNotEmpty()) {
                        if (y >= BOTTOM_LIMIT) startNextPage()
                        val layout = StaticLayout.Builder.obtain(remaining, 0, remaining.length, paint, width.toInt())
                            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(2f, 1.1f)
                            .build()
                        val available = (BOTTOM_LIMIT - y).toInt()
                        var fittingLines = 0
                        while (fittingLines < layout.lineCount && layout.getLineBottom(fittingLines) <= available) fittingLines++
                        if (fittingLines == 0) {
                            startNextPage()
                            continue
                        }
                        val end = layout.getLineEnd(fittingLines - 1)
                        val segment = remaining.substring(0, end)
                        val segmentLayout = StaticLayout.Builder.obtain(segment, 0, segment.length, paint, width.toInt())
                            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(2f, 1.1f)
                            .build()
                        val height = segmentLayout.height.toFloat()
                        decoration?.invoke(canvas, y, height)
                        canvas.save()
                        canvas.translate(MARGIN + xOffset, y)
                        segmentLayout.draw(canvas)
                        canvas.restore()
                        y += height
                        totalHeight += height
                        remaining = remaining.substring(end)
                        if (remaining.isNotEmpty()) startNextPage()
                    }
                }
                return totalHeight
            }

        // --- Document Header ---
        val docTitle = titleOverride ?: "NoCap — Tổng hợp Ghi chú & Trích dẫn"
        drawTextLayout(docTitle, titlePaint)
        y += 4f

        val exportMeta = "Ngày xuất: ${formatDate(System.currentTimeMillis())} • Tổng số tài liệu: ${booksWithData.size}"
        drawTextLayout(exportMeta, metaPaint)
        y += 8f

        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 16f

        // --- Content per book ---
        for ((book, highlights, bookmarks) in booksWithData) {
            checkPageBreak(60f)

            val bookTitle = book.userTitleOverride ?: book.title
            val bookAuthor = book.userAuthorOverride ?: book.author
            drawTextLayout(bookTitle, sectionPaint)
            y += 2f
            drawTextLayout("Tác giả: $bookAuthor • Định dạng: ${book.format.name}", metaPaint)
            y += 8f

            val notes = highlights.filter { !it.note.isNullOrBlank() }
            val plainHighlights = highlights.filter { it.note.isNullOrBlank() }
            val activeBookmarks = bookmarks.filter { !it.isDeleted }

            // 1. Notes
            if (notes.isNotEmpty()) {
                checkPageBreak(30f)
                drawTextLayout("Ghi chú (${notes.size})", subSectionPaint)
                y += 6f

                for ((idx, noteItem) in notes.withIndex()) {
                    checkPageBreak(40f)
                    val noteHeader = "${idx + 1}. ${noteItem.note}"
                    drawTextLayout(noteHeader, bodyPaint, xOffset = 0f)
                    y += 2f

                    // Draw quote bar and quote text
                    val quoteText = "\"${noteItem.text.trim()}\""
                    val quoteWidth = CONTENT_WIDTH - 12f
                    drawTextLayout(quoteText, quotePaint, xOffset = 8f, width = quoteWidth) { target, top, height ->
                        target.drawLine(MARGIN + 2f, top, MARGIN + 2f, top + height, accentBarPaint)
                    }
                    y += 4f

                    val itemMeta = "Tạo lúc: ${formatDate(noteItem.createdAt)}"
                    drawTextLayout(itemMeta, metaPaint, xOffset = 8f)
                    y += 10f
                }
            }

            // 2. Highlights
            if (plainHighlights.isNotEmpty()) {
                checkPageBreak(30f)
                drawTextLayout("Đoạn trích (${plainHighlights.size})", subSectionPaint)
                y += 6f

                for ((idx, hlItem) in plainHighlights.withIndex()) {
                    checkPageBreak(30f)
                    val hlText = "${idx + 1}. \"${hlItem.text.trim()}\""
                    drawTextLayout(hlText, bodyPaint)
                    y += 2f
                    val itemMeta = "Tạo lúc: ${formatDate(hlItem.createdAt)}"
                    drawTextLayout(itemMeta, metaPaint, xOffset = 12f)
                    y += 8f
                }
            }

            // 3. Bookmarks
            if (activeBookmarks.isNotEmpty()) {
                checkPageBreak(30f)
                drawTextLayout("Đánh dấu trang (${activeBookmarks.size})", subSectionPaint)
                y += 6f

                for ((idx, bm) in activeBookmarks.withIndex()) {
                    checkPageBreak(25f)
                    val bmTitle = "${idx + 1}. ${bm.chapterTitle.ifBlank { "Trang đánh dấu" }}"
                    drawTextLayout(bmTitle, bodyPaint)
                    y += 2f
                    if (!bm.snippet.isNullOrBlank()) {
                        drawTextLayout("\"${bm.snippet.trim()}\"", quotePaint, xOffset = 12f)
                        y += 2f
                    }
                    val bmMeta = "Tạo lúc: ${formatDate(bm.createdAt)}"
                    drawTextLayout(bmMeta, metaPaint, xOffset = 12f)
                    y += 8f
                }
            }

            y += 8f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += 16f
        }

            // Final page footer
            val footer = "- Trang $pageNumber -"
            canvas.drawText(footer, (PAGE_WIDTH - bodyPaint.measureText(footer)) / 2f, PAGE_HEIGHT - 20f, metaPaint)
            document.finishPage(page)

            document.writeTo(outputStream)
        } finally {
            document.close()
        }
    }
}
