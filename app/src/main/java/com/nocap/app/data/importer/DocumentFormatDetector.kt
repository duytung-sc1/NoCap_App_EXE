package com.nocap.app.data.importer

import com.nocap.app.domain.model.PublicationFormat
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream

object DocumentFormatDetector {

    private val PDF_MAGIC = byteArrayOf(0x25, 0x50, 0x44, 0x46) // %PDF
    private val ZIP_MAGIC = byteArrayOf(0x50, 0x4B, 0x03, 0x04) // PK\x03\x04
    private val PNG_MAGIC = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    private val JPEG_MAGIC = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    private val RIFF_MAGIC = byteArrayOf(0x52, 0x49, 0x46, 0x46) // RIFF
    private val WEBP_MAGIC = byteArrayOf(0x57, 0x45, 0x42, 0x50) // WEBP

    fun detect(file: File, suggestedFilename: String? = null, sourceMimeType: String? = null): PublicationFormat? {
        if (!file.exists() || file.length() == 0L) return null

        val header = ByteArray(64)
        val bytesRead = try {
            FileInputStream(file).use { it.read(header) }
        } catch (_: Exception) {
            return null
        }
        if (bytesRead < 3) return null

        // 1. PDF
        if (matchesPrefix(header, PDF_MAGIC)) {
            return PublicationFormat.PDF
        }

        // 2. PNG
        if (matchesPrefix(header, PNG_MAGIC)) {
            return PublicationFormat.PNG
        }

        // 3. JPEG
        if (matchesPrefix(header, JPEG_MAGIC)) {
            return PublicationFormat.JPEG
        }

        // 4. WebP
        if (matchesPrefix(header, RIFF_MAGIC) && bytesRead >= 12) {
            if (header[8] == WEBP_MAGIC[0] && header[9] == WEBP_MAGIC[1] &&
                header[10] == WEBP_MAGIC[2] && header[11] == WEBP_MAGIC[3]) {
                return PublicationFormat.WEBP
            }
        }

        // 5. ZIP-based formats (EPUB, DOCX, CBZ)
        if (matchesPrefix(header, ZIP_MAGIC)) {
            return detectZipFormat(file)
        }

        // 6. Text-based detection (HTML, Markdown, TXT)
        return detectTextFormat(file, header.copyOf(bytesRead), suggestedFilename)
    }

    private fun detectZipFormat(file: File): PublicationFormat? {
        var hasEpubMime = false
        var hasEpubStructure = false
        var hasDocxContentTypes = false
        var hasDocxDocument = false
        var hasImageEntry = false

        try {
            FileInputStream(file).use { fis ->
                ZipInputStream(fis).use { zis ->
                    var entry = zis.nextEntry
                    var count = 0
                    while (entry != null && count < 500) {
                        count++
                        val name = entry.name.lowercase()
                        if (name == "mimetype") {
                            val content = zis.bufferedReader().readText().trim()
                            if (content.startsWith("application/epub+zip")) {
                                hasEpubMime = true
                            }
                        }
                        if (name.contains("meta-inf/container.xml") || name.endsWith(".opf")) {
                            hasEpubStructure = true
                        }
                        if (name == "[content_types].xml") {
                            hasDocxContentTypes = true
                        }
                        if (name == "word/document.xml") {
                            hasDocxDocument = true
                        }
                        if (name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                            name.endsWith(".png") || name.endsWith(".webp")) {
                            hasImageEntry = true
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
        } catch (_: Exception) {
            return null
        }

        if (hasEpubMime || hasEpubStructure) {
            return PublicationFormat.EPUB
        }
        if (hasDocxContentTypes && hasDocxDocument) {
            return PublicationFormat.DOCX
        }
        if (hasImageEntry && !hasDocxDocument && !hasDocxContentTypes) {
            return PublicationFormat.CBZ
        }

        // Generic ZIP without recognized book/document payload is rejected
        return null
    }

    private fun detectTextFormat(file: File, header: ByteArray, suggestedFilename: String?): PublicationFormat? {
        // Binary ratio check: if null bytes exist in header or control chars exceed threshold, reject
        var nullBytes = 0
        var controlBytes = 0
        for (b in header) {
            val unsigned = b.toInt() and 0xFF
            if (unsigned == 0) nullBytes++
            else if (unsigned < 32 && unsigned != 9 && unsigned != 10 && unsigned != 13) {
                // Not TAB, LF, CR
                controlBytes++
            }
        }
        if (nullBytes > 0 || controlBytes > 2) {
            // Check UTF-16 LE / BE BOM
            val hasUtf16LeBom = header.size >= 2 && header[0] == 0xFF.toByte() && header[1] == 0xFE.toByte()
            val hasUtf16BeBom = header.size >= 2 && header[0] == 0xFE.toByte() && header[1] == 0xFF.toByte()
            if (!hasUtf16LeBom && !hasUtf16BeBom) {
                return null // Binary file
            }
        }

        val textSample = try {
            val lengthToRead = minOf(file.length(), 4096L).toInt()
            val sampleBytes = ByteArray(lengthToRead)
            FileInputStream(file).use { it.read(sampleBytes) }
            String(sampleBytes, Charsets.UTF_8).trim()
        } catch (_: Exception) {
            return null
        }

        val lowerSample = textSample.lowercase()
        val ext = (suggestedFilename ?: file.name).substringAfterLast('.', "").lowercase()

        // Reject if file has a non-text or binary extension claiming to be a format it is not
        val nonTextExtensions = setOf(
            "pdf", "epub", "docx", "doc", "docm", "cbz", "jpg", "jpeg", "png", "webp",
            "gif", "bmp", "ico", "zip", "rar", "7z", "tar", "gz", "xlsx", "xls", "pptx",
            "ppt", "exe", "apk", "bin", "iso", "mp3", "mp4"
        )
        if (ext in nonTextExtensions) {
            return null
        }

        // HTML check
        if (lowerSample.startsWith("<!doctype html") ||
            lowerSample.startsWith("<html") ||
            (lowerSample.contains("<head") && lowerSample.contains("<body")) ||
            (ext in setOf("html", "htm") && lowerSample.contains("<") && lowerSample.contains(">"))) {
            return PublicationFormat.HTML
        }

        // Markdown check
        if (ext in setOf("md", "markdown")) {
            return PublicationFormat.MARKDOWN
        }

        // TXT check
        if (ext in setOf("txt", "text") || ext.isEmpty()) {
            return PublicationFormat.TXT
        }

        // If unknown extension, do not blindly classify as TXT
        return null
    }

    private fun matchesPrefix(data: ByteArray, magic: ByteArray): Boolean {
        if (data.size < magic.size) return false
        for (i in magic.indices) {
            if (data[i] != magic[i]) return false
        }
        return true
    }
}
