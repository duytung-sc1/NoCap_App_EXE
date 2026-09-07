package com.nocap.app.data.importer

import com.nocap.app.domain.model.PublicationFormat
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

object FormatSniffer {

    private val PDF_MAGIC = byteArrayOf(0x25, 0x50, 0x44, 0x46) // %PDF
    private val ZIP_MAGIC = byteArrayOf(0x50, 0x4B, 0x03, 0x04) // PK\x03\x04

    const val MIME_EPUB = "application/epub+zip"
    const val MIME_PDF = "application/pdf"

    fun sniff(file: File): PublicationFormat? {
        return DocumentFormatDetector.detect(file)
    }

    fun sniff(headerBytes: ByteArray): PublicationFormat? {
        if (headerBytes.size < 3) return null
        if (matchesMagic(headerBytes, PDF_MAGIC)) {
            return PublicationFormat.PDF
        }
        if (matchesMagic(headerBytes, ZIP_MAGIC)) {
            return PublicationFormat.EPUB
        }
        return null
    }

    fun sniff(inputStream: InputStream, fallbackFile: File? = null): PublicationFormat? {
        if (fallbackFile != null) {
            return DocumentFormatDetector.detect(fallbackFile)
        }
        val header = ByteArray(4)
        val read = inputStream.read(header)
        if (read < 4) return null

        if (matchesMagic(header, PDF_MAGIC)) {
            return PublicationFormat.PDF
        }
        if (matchesMagic(header, ZIP_MAGIC)) {
            return PublicationFormat.EPUB
        }
        return null
    }

    fun isEpubZip(file: File): Boolean {
        return try {
            ZipInputStream(FileInputStream(file)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "mimetype") {
                        val content = zis.bufferedReader().readText().trim()
                        if (content.startsWith(MIME_EPUB)) {
                            return true
                        }
                    }
                    if (entry.name.endsWith(".opf", ignoreCase = true) || entry.name.contains("META-INF/container.xml")) {
                        return true
                    }
                    entry = zis.nextEntry
                }
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun sniffFromExtension(filenameOrUrl: String): PublicationFormat? {
        val clean = filenameOrUrl.substringBefore('?').substringBefore('#')
        val ext = clean.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "epub" -> PublicationFormat.EPUB
            "pdf" -> PublicationFormat.PDF
            "txt" -> PublicationFormat.TXT
            "md", "markdown" -> PublicationFormat.MARKDOWN
            "html", "htm" -> PublicationFormat.HTML
            "docx" -> PublicationFormat.DOCX
            "jpg", "jpeg" -> PublicationFormat.JPEG
            "png" -> PublicationFormat.PNG
            "webp" -> PublicationFormat.WEBP
            "cbz" -> PublicationFormat.CBZ
            else -> null
        }
    }

    fun sniffFromMimeType(mimeType: String?): PublicationFormat? {
        if (mimeType.isNullOrBlank()) return null
        val clean = mimeType.substringBefore(';').trim().lowercase()
        return when (clean) {
            MIME_EPUB -> PublicationFormat.EPUB
            MIME_PDF -> PublicationFormat.PDF
            "text/plain" -> PublicationFormat.TXT
            "text/markdown", "text/x-markdown" -> PublicationFormat.MARKDOWN
            "text/html" -> PublicationFormat.HTML
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> PublicationFormat.DOCX
            "image/jpeg" -> PublicationFormat.JPEG
            "image/png" -> PublicationFormat.PNG
            "image/webp" -> PublicationFormat.WEBP
            "application/x-cbz", "application/vnd.comicbook+zip" -> PublicationFormat.CBZ
            else -> null
        }
    }

    fun mimeTypeFor(format: PublicationFormat): String {
        return format.mediaType
    }

    fun extensionFor(format: PublicationFormat): String {
        return format.defaultExtension
    }

    private fun matchesMagic(data: ByteArray, magic: ByteArray): Boolean {
        if (data.size < magic.size) return false
        for (i in magic.indices) {
            if (data[i] != magic[i]) return false
        }
        return true
    }
}
