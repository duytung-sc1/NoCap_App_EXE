package com.nocap.app.data.parser

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

object PdfTextExtractor {

    data class PageTextResult(
        val pageIndex: Int,
        val text: String,
        val wordCount: Int
    )

    /**
     * Checks if the given PDF contains selectable text (returns false for scanned / image-only PDFs).
     */
    fun hasSelectableText(file: File): Boolean {
        if (!file.exists() || file.length() < 100) return false
        return try {
            val pages = extractPagesText(file, maxPagesToSample = 5)
            pages.any { it.text.isNotBlank() && it.wordCount > 3 }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Extracts text from a specific page of a PDF (0-indexed).
     */
    fun extractPageText(file: File, targetPageIndex: Int): PageTextResult {
        if (!file.exists() || file.length() < 100) {
            return PageTextResult(targetPageIndex, "", 0)
        }
        return try {
            val allPages = extractPagesText(file, maxPagesToSample = targetPageIndex + 1)
            allPages.find { it.pageIndex == targetPageIndex }
                ?: PageTextResult(targetPageIndex, "", 0)
        } catch (_: Exception) {
            PageTextResult(targetPageIndex, "", 0)
        }
    }

    /**
     * Samples and extracts text across PDF pages up to [maxPagesToSample].
     */
    fun extractPagesText(file: File, maxPagesToSample: Int = 100): List<PageTextResult> {
        val results = mutableListOf<PageTextResult>()
        try {
            val bytes = file.readBytes()
            val contentStreams = extractAllContentStreams(bytes)

            for ((idx, streamBytes) in contentStreams.withIndex()) {
                if (idx >= maxPagesToSample) break
                val pageText = parseStreamText(streamBytes)
                val words = pageText.split(Regex("""\s+""")).filter { it.isNotBlank() }
                results.add(
                    PageTextResult(
                        pageIndex = idx,
                        text = pageText.trim(),
                        wordCount = words.size
                    )
                )
            }
        } catch (_: Exception) {}

        return results
    }

    /**
     * Scans PDF byte array for stream ... endstream blocks and decompresses them.
     */
    private fun extractAllContentStreams(bytes: ByteArray): List<ByteArray> {
        val streams = mutableListOf<ByteArray>()
        val streamKeyword = "stream".toByteArray(StandardCharsets.US_ASCII)
        val endStreamKeyword = "endstream".toByteArray(StandardCharsets.US_ASCII)

        var pos = 0
        while (pos < bytes.size - 10) {
            val streamStart = indexOf(bytes, streamKeyword, pos)
            if (streamStart == -1) break

            var dataStart = streamStart + streamKeyword.size
            if (dataStart < bytes.size && bytes[dataStart] == '\r'.code.toByte()) dataStart++
            if (dataStart < bytes.size && bytes[dataStart] == '\n'.code.toByte()) dataStart++

            val endStream = indexOf(bytes, endStreamKeyword, dataStart)
            if (endStream == -1) break

            var dataEnd = endStream
            if (dataEnd > dataStart && bytes[dataEnd - 1] == '\n'.code.toByte()) dataEnd--
            if (dataEnd > dataStart && bytes[dataEnd - 1] == '\r'.code.toByte()) dataEnd--

            val length = dataEnd - dataStart
            if (length > 0) {
                val streamSlice = bytes.copyOfRange(dataStart, dataEnd)
                // Look back to check if stream was compressed with /FlateDecode
                val headerSnippet = String(bytes, maxOf(0, streamStart - 200), streamStart - maxOf(0, streamStart - 200), StandardCharsets.US_ASCII)
                val isFlate = headerSnippet.contains("/FlateDecode")

                val decompressed = if (isFlate) {
                    inflateBytes(streamSlice) ?: streamSlice
                } else {
                    streamSlice
                }

                // Check if the stream contains text drawing operators (BT ... ET, Tj, TJ)
                val streamStr = String(decompressed, StandardCharsets.ISO_8859_1)
                if (streamStr.contains("BT") || streamStr.contains("Tj") || streamStr.contains("TJ")) {
                    streams.add(decompressed)
                }
            }

            pos = endStream + endStreamKeyword.size
        }

        return streams
    }

    /**
     * Parses PDF text operators inside a decompressed content stream.
     */
    fun parseStreamText(streamBytes: ByteArray): String {
        val raw = String(streamBytes, StandardCharsets.ISO_8859_1)
        val sb = StringBuilder()

        // Regex for Tj: (string) Tj or <hex> Tj
        // Regex for TJ: [ array ] TJ
        var idx = 0
        while (idx < raw.length) {
            // Find next Tj or TJ operator
            val nextTj = raw.indexOf("Tj", idx)
            val nextTJ = raw.indexOf("TJ", idx)

            if (nextTj == -1 && nextTJ == -1) break

            val isTJ = if (nextTj == -1) true else if (nextTJ == -1) false else nextTJ < nextTj
            val opIndex = if (isTJ) nextTJ else nextTj

            if (!isTJ) {
                // Look backwards for string literal ( ... ) or < ... >
                val chunk = raw.substring(maxOf(0, opIndex - 500), opIndex).trimEnd()
                val extracted = extractStringBeforeOperator(chunk)
                if (extracted.isNotBlank()) {
                    sb.append(extracted).append(" ")
                }
                idx = opIndex + 2
            } else {
                // TJ array: e.g. [ (Part 1) 20 (Part 2) ] TJ
                val startBracket = raw.lastIndexOf('[', opIndex)
                if (startBracket != -1 && startBracket > opIndex - 2000) {
                    val arrayContent = raw.substring(startBracket + 1, opIndex).trimEnd()
                    val bracketEnd = arrayContent.lastIndexOf(']')
                    val inside = if (bracketEnd != -1) arrayContent.substring(0, bracketEnd) else arrayContent
                    val extracted = parseTjArray(inside)
                    if (extracted.isNotBlank()) {
                        sb.append(extracted).append(" ")
                    }
                }
                idx = opIndex + 2
            }
        }

        // Also normalize multiple spaces into single space and trim
        return sb.toString().replace(Regex("""\s+"""), " ").trim()
    }

    private fun extractStringBeforeOperator(chunk: String): String {
        if (chunk.endsWith(")")) {
            val start = findMatchingParenBackwards(chunk, chunk.length - 1)
            if (start != -1) {
                return decodePdfString(chunk.substring(start + 1, chunk.length - 1))
            }
        } else if (chunk.endsWith(">")) {
            val start = chunk.lastIndexOf('<')
            if (start != -1) {
                return decodeHexPdfString(chunk.substring(start + 1, chunk.length - 1))
            }
        }
        return ""
    }

    private fun parseTjArray(content: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < content.length) {
            val ch = content[i]
            if (ch == '(') {
                val end = findMatchingParenForwards(content, i)
                if (end != -1) {
                    val str = decodePdfString(content.substring(i + 1, end))
                    sb.append(str)
                    i = end + 1
                    continue
                }
            } else if (ch == '<') {
                val end = content.indexOf('>', i)
                if (end != -1) {
                    val str = decodeHexPdfString(content.substring(i + 1, end))
                    sb.append(str)
                    i = end + 1
                    continue
                }
            }
            i++
        }
        return sb.toString()
    }

    private fun decodePdfString(escaped: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < escaped.length) {
            val c = escaped[i]
            if (c == '\\' && i + 1 < escaped.length) {
                when (val next = escaped[i + 1]) {
                    'n' -> { sb.append('\n'); i += 2 }
                    'r' -> { sb.append('\r'); i += 2 }
                    't' -> { sb.append('\t'); i += 2 }
                    'b' -> { sb.append('\b'); i += 2 }
                    'f' -> { sb.append('\u000C'); i += 2 }
                    '(', ')', '\\' -> { sb.append(next); i += 2 }
                    in '0'..'7' -> {
                        // Octal sequence up to 3 digits
                        var octal = "" + next
                        var k = i + 2
                        while (k < escaped.length && k < i + 4 && escaped[k] in '0'..'7') {
                            octal += escaped[k]
                            k++
                        }
                        val code = octal.toIntOrNull(8) ?: 0
                        sb.append(code.toChar())
                        i = k
                    }
                    else -> { sb.append(next); i += 2 }
                }
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    private fun decodeHexPdfString(hex: String): String {
        val clean = hex.replace(Regex("""\s+"""), "")
        if (clean.isEmpty()) return ""
        val bytes = ByteArray(clean.length / 2)
        for (i in 0 until clean.length - 1 step 2) {
            val byteVal = clean.substring(i, i + 2).toIntOrNull(16) ?: 0
            bytes[i / 2] = byteVal.toByte()
        }

        // UTF-16 BE with BOM: 0xFE 0xFF
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16BE)
        }
        // 2-byte sequence (UTF-16 BE without BOM)
        if (bytes.size >= 4 && bytes[0] == 0.toByte() && bytes[2] == 0.toByte()) {
            return String(bytes, StandardCharsets.UTF_16BE)
        }

        return String(bytes, StandardCharsets.ISO_8859_1)
    }

    private fun findMatchingParenBackwards(str: String, closeIdx: Int): Int {
        var depth = 1
        var idx = closeIdx - 1
        while (idx >= 0) {
            val ch = str[idx]
            if (ch == ')' && (idx == 0 || str[idx - 1] != '\\')) {
                depth++
            } else if (ch == '(' && (idx == 0 || str[idx - 1] != '\\')) {
                depth--
                if (depth == 0) return idx
            }
            idx--
        }
        return -1
    }

    private fun findMatchingParenForwards(str: String, openIdx: Int): Int {
        var depth = 1
        var idx = openIdx + 1
        while (idx < str.length) {
            val ch = str[idx]
            if (ch == '(' && str[idx - 1] != '\\') {
                depth++
            } else if (ch == ')' && str[idx - 1] != '\\') {
                depth--
                if (depth == 0) return idx
            }
            idx++
        }
        return -1
    }

    private fun inflateBytes(compressed: ByteArray): ByteArray? {
        // Try standard zlib wrapper first
        return try {
            val inflater = Inflater(false)
            inflater.setInput(compressed)
            val buffer = ByteArray(4096)
            val out = ByteArrayOutputStream()
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count == 0 && inflater.needsInput()) break
                out.write(buffer, 0, count)
            }
            inflater.end()
            out.toByteArray()
        } catch (_: Exception) {
            // Fallback: raw deflate without zlib header
            try {
                val rawInflater = Inflater(true)
                rawInflater.setInput(compressed)
                val buffer = ByteArray(4096)
                val out = ByteArrayOutputStream()
                while (!rawInflater.finished()) {
                    val count = rawInflater.inflate(buffer)
                    if (count == 0 && rawInflater.needsInput()) break
                    out.write(buffer, 0, count)
                }
                rawInflater.end()
                out.toByteArray()
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun indexOf(source: ByteArray, target: ByteArray, fromIndex: Int): Int {
        if (target.isEmpty() || fromIndex >= source.size) return -1
        for (i in fromIndex..(source.size - target.size)) {
            var found = true
            for (j in target.indices) {
                if (source[i + j] != target[j]) {
                    found = false
                    break
                }
            }
            if (found) return i
        }
        return -1
    }
}
