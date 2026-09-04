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
        if (!file.exists() || file.length() < 4) return null
        return try {
            FileInputStream(file).use { input ->
                sniff(input, file)
            }
        } catch (_: Exception) {
            sniffFromExtension(file.name)
        }
    }

    fun sniff(headerBytes: ByteArray): PublicationFormat? {
        if (headerBytes.size < 4) return null
        if (matchesMagic(headerBytes, PDF_MAGIC)) {
            return PublicationFormat.PDF
        }
        if (matchesMagic(headerBytes, ZIP_MAGIC)) {
            return PublicationFormat.EPUB
        }
        return null
    }

    fun sniff(inputStream: InputStream, fallbackFile: File? = null): PublicationFormat? {
        val header = ByteArray(4)
        val read = inputStream.read(header)
        if (read < 4) return null

        if (matchesMagic(header, PDF_MAGIC)) {
            return PublicationFormat.PDF
        }
        if (matchesMagic(header, ZIP_MAGIC)) {
            if (fallbackFile != null) {
                return if (isEpubZip(fallbackFile)) PublicationFormat.EPUB else null
            }
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
            "application/x-cbz", "application/vnd.comicbook+zip" -> PublicationFormat.CBZ
            else -> null
        }
    }

    fun mimeTypeFor(format: PublicationFormat): String {
        return when (format) {
            PublicationFormat.EPUB -> MIME_EPUB
            PublicationFormat.PDF -> MIME_PDF
            PublicationFormat.CBZ -> "application/x-cbz"
        }
    }

    fun extensionFor(format: PublicationFormat): String {
        return when (format) {
            PublicationFormat.EPUB -> "epub"
            PublicationFormat.PDF -> "pdf"
            PublicationFormat.CBZ -> "cbz"
        }
    }

    private fun matchesMagic(data: ByteArray, magic: ByteArray): Boolean {
        if (data.size < magic.size) return false
        for (i in magic.indices) {
            if (data[i] != magic[i]) return false
        }
        return true
    }
}
