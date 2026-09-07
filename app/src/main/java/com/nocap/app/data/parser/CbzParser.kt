package com.nocap.app.data.parser

import com.nocap.app.core.util.ZipSecurityUtils
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

object CbzParser {

    private val SUPPORTED_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")

    data class CbzPage(
        val index: Int,
        val entryName: String,
        val fileName: String
    )

    fun validateAndListPages(file: File): List<CbzPage> {
        if (!file.exists() || file.length() < 100) {
            throw IllegalArgumentException("Tệp CBZ không tồn tại hoặc không hợp lệ: ${file.name}")
        }

        // Validate ZIP security constraints (size, traversal, ratio)
        ZipSecurityUtils.validateZipStructure(file)

        val imageEntries = mutableListOf<String>()

        FileInputStream(file).use { fis ->
            ZipInputStream(fis).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (!entry.isDirectory && isSupportedImageEntry(name)) {
                        imageEntries.add(name)
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }

        if (imageEntries.isEmpty()) {
            throw IllegalArgumentException("Tệp CBZ không chứa trang ảnh hợp lệ (JPG, PNG, WebP).")
        }

        // Natural sort
        val sortedEntries = imageEntries.sortedWith(NaturalOrderComparator)

        return sortedEntries.mapIndexed { idx, entryName ->
            CbzPage(
                index = idx,
                entryName = entryName,
                fileName = File(entryName).name
            )
        }
    }

    fun extractPageToFile(cbzFile: File, entryName: String, targetFile: File) {
        ZipFile(cbzFile).use { zip ->
            val entry = zip.getEntry(entryName)
                ?: throw IllegalArgumentException("Không tìm thấy trang $entryName trong tệp CBZ")
            zip.getInputStream(entry).use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    fun extractFirstPageThumbnail(cbzFile: File, targetFile: File): Boolean {
        return try {
            val pages = validateAndListPages(cbzFile)
            if (pages.isEmpty()) return false
            extractPageToFile(cbzFile, pages.first().entryName, targetFile)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun isSupportedImageEntry(entryName: String): Boolean {
        val normalized = entryName.replace('\\', '/')
        if (normalized.startsWith("__MACOSX/") || normalized.contains("/.DS_Store") || normalized.startsWith(".")) {
            return false
        }
        val ext = normalized.substringAfterLast('.', "").lowercase()
        return ext in SUPPORTED_EXTENSIONS
    }

    /**
     * Natural Order Comparator to ensure '1.jpg', '2.jpg', '10.jpg' sorts correctly.
     */
    object NaturalOrderComparator : Comparator<String> {
        private val SPLIT_REGEX = Regex("""(?<=\D)(?=\d)|(?<=\d)(?=\D)""")

        override fun compare(s1: String, s2: String): Int {
            val parts1 = s1.split(SPLIT_REGEX)
            val parts2 = s2.split(SPLIT_REGEX)

            val minLen = minOf(parts1.size, parts2.size)
            for (i in 0 until minLen) {
                val p1 = parts1[i]
                val p2 = parts2[i]

                val n1 = p1.toLongOrNull()
                val n2 = p2.toLongOrNull()

                val result = if (n1 != null && n2 != null) {
                    n1.compareTo(n2)
                } else {
                    p1.compareTo(p2, ignoreCase = true)
                }

                if (result != 0) return result
            }
            return parts1.size.compareTo(parts2.size)
        }
    }
}
