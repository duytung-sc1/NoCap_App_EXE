package com.nocap.app.data.parser

import com.nocap.app.domain.model.TextDocument
import com.nocap.app.domain.model.TextDocumentBlock
import com.nocap.app.domain.model.TextSpan
import java.io.File
import java.io.FileInputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

object TxtParser {

    const val MAX_TXT_BYTES: Long = 50L * 1024 * 1024 // 50 MB

    fun validateAndParse(file: File, suggestedTitle: String? = null): TextDocument {
        if (!file.exists()) {
            throw IllegalArgumentException("Tệp văn bản không tồn tại: ${file.name}")
        }
        if (file.length() > MAX_TXT_BYTES) {
            throw IllegalArgumentException("Tệp văn bản quá lớn (${file.length()} bytes), vượt quá giới hạn 50MB")
        }

        val (charset, offset) = detectCharsetAndBom(file)

        val text = FileInputStream(file).use { fis ->
            if (offset > 0) {
                fis.skip(offset.toLong())
            }
            fis.bufferedReader(charset).readText()
        }

        // Check binary characteristics on the decoded text
        var controlCount = 0
        val sampleLen = minOf(text.length, 4096)
        for (i in 0 until sampleLen) {
            val ch = text[i]
            if (ch == '\u0000') {
                throw IllegalArgumentException("Tệp chứa ký tự nhị phân (null byte), không phải là tệp văn bản hợp lệ.")
            }
            if (ch.code < 32 && ch != '\n' && ch != '\r' && ch != '\t') {
                controlCount++
            }
        }
        if (sampleLen > 0 && (controlCount.toFloat() / sampleLen) > 0.05f) {
            throw IllegalArgumentException("Tệp có tỷ lệ ký tự điều khiển bất thường, nghi ngờ là tệp nhị phân.")
        }

        // Normalize line endings: CRLF -> LF, CR -> LF
        val normalized = text.replace("\r\n", "\n").replace('\r', '\n')
        val lines = normalized.split('\n')

        val blocks = mutableListOf<TextDocumentBlock>()
        val currentParagraph = StringBuilder()

        for (line in lines) {
            val trimmed = line.trimEnd()
            if (trimmed.isBlank()) {
                if (currentParagraph.isNotBlank()) {
                    blocks.add(TextDocumentBlock.Paragraph(listOf(TextSpan(currentParagraph.toString().trim()))))
                    currentParagraph.clear()
                }
            } else {
                if (currentParagraph.isNotEmpty()) {
                    currentParagraph.append("\n")
                }
                currentParagraph.append(trimmed)
            }
        }
        if (currentParagraph.isNotBlank()) {
            blocks.add(TextDocumentBlock.Paragraph(listOf(TextSpan(currentParagraph.toString().trim()))))
        }

        val title = suggestedTitle?.substringBeforeLast('.')?.ifBlank { null }
            ?: file.nameWithoutExtension.ifBlank { "Tài liệu văn bản" }

        return TextDocument(
            title = title,
            blocks = if (blocks.isEmpty()) listOf(TextDocumentBlock.Paragraph("")) else blocks
        )
    }

    private fun detectCharsetAndBom(file: File): Pair<Charset, Int> {
        val header = ByteArray(4)
        val read = try {
            FileInputStream(file).use { it.read(header) }
        } catch (_: Exception) {
            0
        }

        if (read >= 3 && header[0] == 0xEF.toByte() && header[1] == 0xBB.toByte() && header[2] == 0xBF.toByte()) {
            return Pair(StandardCharsets.UTF_8, 3)
        }
        if (read >= 2 && header[0] == 0xFF.toByte() && header[1] == 0xFE.toByte()) {
            return Pair(StandardCharsets.UTF_16LE, 2)
        }
        if (read >= 2 && header[0] == 0xFE.toByte() && header[1] == 0xFF.toByte()) {
            return Pair(StandardCharsets.UTF_16BE, 2)
        }

        return Pair(StandardCharsets.UTF_8, 0)
    }
}
