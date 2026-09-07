package com.nocap.app.data.parser

import android.util.Xml
import com.nocap.app.core.util.ZipSecurityUtils
import com.nocap.app.domain.model.TextDocument
import com.nocap.app.domain.model.TextDocumentBlock
import com.nocap.app.domain.model.TextSpan
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

object DocxParser {

    const val NOTICE_UNSUPPORTED_ELEMENTS = "Tài liệu có một số thành phần chưa được hỗ trợ hoàn toàn."

    fun validateStructure(file: File): Boolean {
        if (!file.exists() || file.length() < 100) return false
        return try {
            ZipSecurityUtils.validateZipStructure(file)
            var hasContentTypes = false
            var hasDocumentXml = false

            FileInputStream(file).use { fis ->
                ZipInputStream(fis).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name.lowercase()
                        if (name == "[content_types].xml") hasContentTypes = true
                        if (name == "word/document.xml") hasDocumentXml = true
                        // Reject macros or active content
                        if (name.endsWith(".bin") && name.contains("vba")) {
                            return false
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
            hasContentTypes && hasDocumentXml
        } catch (_: Exception) {
            false
        }
    }

    fun parse(file: File, cacheDir: File? = null, fallbackTitle: String? = null): TextDocument {
        if (!validateStructure(file)) {
            throw IllegalArgumentException("Tệp không phải là định dạng DOCX hợp lệ hoặc đã bị lỗi cấu trúc.")
        }

        var title: String? = null
        var author: String? = null
        val blocks = mutableListOf<TextDocumentBlock>()
        var hadUnsupportedFeatures = false

        val zip = ZipFile(file)
        try {
            // 1. Extract metadata from docProps/core.xml if present
            val coreEntry = zip.getEntry("docProps/core.xml")
            if (coreEntry != null) {
                zip.getInputStream(coreEntry).use { stream ->
                    val (extractedTitle, extractedAuthor) = parseCoreProperties(stream)
                    title = extractedTitle
                    author = extractedAuthor
                }
            }

            // 2. Extract media images if cacheDir provided
            val imageMap = mutableMapOf<String, String>()
            if (cacheDir != null) {
                val mediaEntries = zip.entries()
                while (mediaEntries.hasMoreElements()) {
                    val entry = mediaEntries.nextElement()
                    if (entry.name.startsWith("word/media/") && !entry.isDirectory) {
                        val imgFile = File(cacheDir, "docx_img_${System.currentTimeMillis()}_${File(entry.name).name}")
                        try {
                            zip.getInputStream(entry).use { input ->
                                imgFile.outputStream().use { output -> input.copyTo(output) }
                            }
                            imageMap[entry.name] = imgFile.absolutePath
                        } catch (_: Exception) {}
                    }
                }
            }

            // 3. Parse word/document.xml
            val docEntry = zip.getEntry("word/document.xml")
                ?: throw IllegalArgumentException("Không tìm thấy word/document.xml trong tệp DOCX")

            zip.getInputStream(docEntry).use { stream ->
                val parser = newPullParser()
                parser.setInput(stream, "UTF-8")

                var eventType = parser.eventType
                var inParagraph = false
                var inTable = false
                var inTableRow = false
                var inTableCell = false

                var currentHeadingLevel: Int? = null
                var isListItem = false
                var currentSpans = mutableListOf<TextSpan>()
                var isRunBold = false
                var isRunItalic = false
                var isRunUnderline = false

                val tableRows = mutableListOf<MutableList<String>>()
                var currentRow = mutableListOf<String>()
                val cellText = StringBuilder()

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    val tag = parser.name ?: ""

                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            when (tag) {
                                "tbl", "w:tbl" -> {
                                    inTable = true
                                    tableRows.clear()
                                }
                                "tr", "w:tr" -> {
                                    if (inTable) {
                                        inTableRow = true
                                        currentRow = mutableListOf()
                                    }
                                }
                                "tc", "w:tc" -> {
                                    if (inTableRow) {
                                        inTableCell = true
                                        cellText.clear()
                                    }
                                }
                                "p", "w:p" -> {
                                    inParagraph = true
                                    currentHeadingLevel = null
                                    isListItem = false
                                    currentSpans = mutableListOf()
                                }
                                "pStyle", "w:pStyle" -> {
                                    val valAttr = parser.getAttributeValue(null, "val") ?: parser.getAttributeValue(null, "w:val") ?: ""
                                    val lower = valAttr.lowercase()
                                    if (lower.startsWith("heading") || lower.startsWith("tieude")) {
                                        val level = lower.filter { it.isDigit() }.toIntOrNull() ?: 1
                                        currentHeadingLevel = level.coerceIn(1, 6)
                                    }
                                }
                                "numPr", "w:numPr" -> {
                                    isListItem = true
                                }
                                "r", "w:r" -> {
                                    isRunBold = false
                                    isRunItalic = false
                                    isRunUnderline = false
                                }
                                "b", "w:b" -> {
                                    val valAttr = parser.getAttributeValue(null, "val") ?: parser.getAttributeValue(null, "w:val")
                                    isRunBold = valAttr == null || valAttr == "1" || valAttr == "true"
                                }
                                "i", "w:i" -> {
                                    val valAttr = parser.getAttributeValue(null, "val") ?: parser.getAttributeValue(null, "w:val")
                                    isRunItalic = valAttr == null || valAttr == "1" || valAttr == "true"
                                }
                                "u", "w:u" -> {
                                    isRunUnderline = true
                                }
                                "t", "w:t" -> {
                                    val text = parser.nextText()
                                    if (text.isNotEmpty()) {
                                        if (inTableCell) {
                                            cellText.append(text)
                                        } else {
                                            currentSpans.add(
                                                TextSpan(
                                                    text = text,
                                                    isBold = isRunBold,
                                                    isItalic = isRunItalic,
                                                    isUnderline = isRunUnderline
                                                )
                                            )
                                        }
                                    }
                                }
                                "drawing", "w:drawing" -> {
                                    // Embedded shape or image
                                    if (imageMap.isNotEmpty()) {
                                        val firstImg = imageMap.values.firstOrNull()
                                        if (firstImg != null) {
                                            blocks.add(TextDocumentBlock.ImageBlock(firstImg, "Hình ảnh trong tài liệu"))
                                        }
                                    }
                                }
                                "object", "w:object", "smartTag", "w:smartTag" -> {
                                    hadUnsupportedFeatures = true
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            when (tag) {
                                "p", "w:p" -> {
                                    inParagraph = false
                                    if (!inTableCell) {
                                        if (currentHeadingLevel != null) {
                                            val headingText = currentSpans.joinToString("") { it.text }.trim()
                                            if (headingText.isNotEmpty()) {
                                                if (currentHeadingLevel == 1 && title == null) {
                                                    title = headingText
                                                }
                                                blocks.add(TextDocumentBlock.Heading(currentHeadingLevel, headingText))
                                            }
                                        } else if (isListItem) {
                                            if (currentSpans.any { it.text.isNotBlank() }) {
                                                blocks.add(TextDocumentBlock.ListItem(ordered = false, index = 0, spans = currentSpans))
                                            }
                                        } else {
                                            if (currentSpans.any { it.text.isNotBlank() }) {
                                                blocks.add(TextDocumentBlock.Paragraph(currentSpans))
                                            }
                                        }
                                    }
                                }
                                "tc", "w:tc" -> {
                                    inTableCell = false
                                    currentRow.add(cellText.toString().trim())
                                }
                                "tr", "w:tr" -> {
                                    inTableRow = false
                                    if (currentRow.isNotEmpty()) {
                                        tableRows.add(currentRow)
                                    }
                                }
                                "tbl", "w:tbl" -> {
                                    inTable = false
                                    if (tableRows.isNotEmpty()) {
                                        blocks.add(TextDocumentBlock.Table(tableRows.map { it.toList() }))
                                    }
                                }
                            }
                        }
                    }
                    eventType = parser.next()
                }
            }
        } finally {
            zip.close()
        }

        val resolvedTitle = title?.ifBlank { null }
            ?: fallbackTitle?.ifBlank { null }
            ?: file.nameWithoutExtension.ifBlank { "Tài liệu DOCX" }

        return TextDocument(
            title = resolvedTitle,
            author = author?.ifBlank { null },
            blocks = if (blocks.isEmpty()) listOf(TextDocumentBlock.Paragraph("")) else blocks,
            warningMessage = if (hadUnsupportedFeatures) NOTICE_UNSUPPORTED_ELEMENTS else null
        )
    }

    private fun parseCoreProperties(stream: InputStream): Pair<String?, String?> {
        var title: String? = null
        var creator: String? = null
        try {
            val parser = newPullParser()
            parser.setInput(stream, "UTF-8")
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "title", "dc:title" -> title = parser.nextText()
                        "creator", "dc:creator" -> creator = parser.nextText()
                    }
                }
                event = parser.next()
            }
        } catch (_: Exception) {}
        return Pair(title?.trim()?.ifBlank { null }, creator?.trim()?.ifBlank { null })
    }

    private fun newPullParser(): XmlPullParser {
        try {
            val factory = XmlPullParserFactory.newInstance()
            return factory.newPullParser()
        } catch (_: Throwable) {}

        try {
            val kxmlClass = Class.forName("org.kxml2.io.KXmlParser")
            return kxmlClass.getDeclaredConstructor().newInstance() as XmlPullParser
        } catch (_: Throwable) {}

        val parser = try {
            Xml.newPullParser()
        } catch (_: Throwable) {
            null
        }

        return parser ?: throw IllegalStateException("Không tìm thấy bộ phân tích XML phù hợp.")
    }
}
