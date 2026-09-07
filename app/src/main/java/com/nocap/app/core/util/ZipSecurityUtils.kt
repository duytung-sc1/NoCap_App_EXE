package com.nocap.app.core.util

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object ZipSecurityUtils {

    const val MAX_ARCHIVE_BYTES: Long = 100L * 1024 * 1024 // 100 MB
    const val MAX_TOTAL_UNCOMPRESSED_BYTES: Long = 250L * 1024 * 1024 // 250 MB
    const val MAX_ENTRY_BYTES: Long = 50L * 1024 * 1024 // 50 MB
    const val MAX_ENTRY_COUNT: Int = 10_000
    const val MAX_COMPRESSION_RATIO: Int = 100 // 100:1

    class ZipSecurityException(message: String) : SecurityException(message)

    fun isPathTraversal(entryName: String): Boolean {
        val normalized = entryName.replace('\\', '/')
        if (normalized.startsWith("/") || normalized.startsWith("\\")) return true
        val parts = normalized.split('/')
        return parts.any { it == ".." }
    }

    fun sanitizeEntryName(entryName: String): String {
        return entryName.replace('\\', '/').trimStart('/')
    }

    /**
     * Validates an entire ZIP file for security constraints (size, count, ratio, path traversal).
     * Returns the total entry count and total uncompressed bytes on success.
     */
    fun validateZipStructure(file: File): ZipValidationResult {
        if (!file.exists()) {
            throw ZipSecurityException("Tệp lưu trữ không tồn tại: ${file.name}")
        }
        if (file.length() > MAX_ARCHIVE_BYTES) {
            throw ZipSecurityException("Kích thước tệp nén (${file.length()} bytes) vượt quá giới hạn an toàn ($MAX_ARCHIVE_BYTES bytes)")
        }

        var entryCount = 0
        var totalUncompressedBytes = 0L

        FileInputStream(file).use { fis ->
            ZipInputStream(fis).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    entryCount++
                    if (entryCount > MAX_ENTRY_COUNT) {
                        throw ZipSecurityException("Số lượng tệp con ($entryCount) vượt quá giới hạn an toàn ($MAX_ENTRY_COUNT)")
                    }

                    val name = entry.name
                    if (isPathTraversal(name)) {
                        throw ZipSecurityException("Phát hiện nguy cơ tấn công đường dẫn (Path Traversal): $name")
                    }

                    val buffer = ByteArray(8192)
                    var entryBytes = 0L
                    var read = zis.read(buffer)
                    while (read != -1) {
                        entryBytes += read
                        totalUncompressedBytes += read

                        if (entryBytes > MAX_ENTRY_BYTES) {
                            throw ZipSecurityException("Kích thước tệp con $name vượt quá giới hạn an toàn ($MAX_ENTRY_BYTES bytes)")
                        }
                        if (totalUncompressedBytes > MAX_TOTAL_UNCOMPRESSED_BYTES) {
                            throw ZipSecurityException("Tổng dung lượng giải nén vượt quá giới hạn an toàn ($MAX_TOTAL_UNCOMPRESSED_BYTES bytes)")
                        }

                        read = zis.read(buffer)
                    }

                    val compressedSize = entry.compressedSize
                    if (compressedSize > 0 && entryBytes > 1024 * 64) {
                        val ratio = entryBytes / compressedSize
                        if (ratio > MAX_COMPRESSION_RATIO) {
                            throw ZipSecurityException("Tỷ lệ nén đáng ngờ ($ratio:1) trong tệp $name (nguy cơ Zip Bomb)")
                        }
                    }

                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }

        return ZipValidationResult(entryCount = entryCount, totalUncompressedBytes = totalUncompressedBytes)
    }

    data class ZipValidationResult(val entryCount: Int, val totalUncompressedBytes: Long)
}
