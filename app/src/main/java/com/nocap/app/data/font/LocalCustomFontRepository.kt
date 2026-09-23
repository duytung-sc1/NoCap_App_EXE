package com.nocap.app.data.font

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.nocap.app.core.database.dao.CustomFontDao
import com.nocap.app.core.database.entity.CustomFontEntity
import com.nocap.app.domain.repository.CustomFontRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class LocalCustomFontRepository(
    private val context: Context,
    private val customFontDao: CustomFontDao
) : CustomFontRepository {
    private val profile get() = com.nocap.app.data.sync.Profiles.active.value
    private val profileFiles get() = com.nocap.app.data.sync.Profiles.files(context, profile)


    companion object {
        const val FONTS_DIR_NAME = "custom_fonts"
        const val MAX_FONT_SIZE_BYTES = 15L * 1024L * 1024L // 15MB cap
        val TTF_MAGIC_1 = byteArrayOf(0x00, 0x01, 0x00, 0x00)
        val TTF_MAGIC_2 = byteArrayOf(0x74, 0x72, 0x75, 0x65) // 'true'
        val OTF_MAGIC = byteArrayOf(0x4F, 0x54, 0x54, 0x4F)   // 'OTTO'
        val WOFF_MAGIC = byteArrayOf(0x77, 0x4F, 0x46, 0x46)  // 'wOFF'
        val WOFF2_MAGIC = byteArrayOf(0x77, 0x4F, 0x46, 0x32) // 'wOF2'

        fun isValidFontHeader(header: ByteArray): Boolean {
            if (header.size < 4) return false
            return matchesMagic(header, TTF_MAGIC_1) ||
                    matchesMagic(header, TTF_MAGIC_2) ||
                    matchesMagic(header, OTF_MAGIC) ||
                    matchesMagic(header, WOFF_MAGIC) ||
                    matchesMagic(header, WOFF2_MAGIC)
        }

        private fun matchesMagic(data: ByteArray, magic: ByteArray): Boolean {
            if (data.size < magic.size) return false
            for (i in magic.indices) {
                if (data[i] != magic[i]) return false
            }
            return true
        }

        fun sanitizeFontFileName(name: String): String {
            val base = File(name.trim()).name
            val sanitized = base.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            return if (sanitized.isBlank() || sanitized.replace("_", "").isBlank()) {
                "font_${System.currentTimeMillis()}.ttf"
            } else {
                sanitized
            }
        }
    }

    override fun observeFonts(): Flow<List<CustomFontEntity>> {
        return customFontDao.observeFonts()
    }

    override suspend fun getAllFonts(): List<CustomFontEntity> {
        return customFontDao.getAllFonts()
    }

    override suspend fun importFont(uri: Uri, suggestedName: String?): Result<CustomFontEntity> = withContext(Dispatchers.IO) {
        try {
            val fileName = queryFileName(uri) ?: suggestedName ?: "custom_font.ttf"
            val sanitizedFileName = sanitizeFontFileName(fileName)
            val fontName = sanitizedFileName.substringBeforeLast(".").replace("_", " ").trim()

            val fontsDir = File(profileFiles, FONTS_DIR_NAME).apply { if (!exists()) mkdirs() }
            val tempFile = File(fontsDir, "temp_${UUID.randomUUID()}.tmp")

            var totalBytes = 0L
            val headerBytes = ByteArray(4)
            var bytesReadInHeader = 0

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        if (bytesReadInHeader < 4) {
                            val copyLen = minOf(4 - bytesReadInHeader, read)
                            System.arraycopy(buffer, 0, headerBytes, bytesReadInHeader, copyLen)
                            bytesReadInHeader += copyLen
                        }
                        totalBytes += read
                        if (totalBytes > MAX_FONT_SIZE_BYTES) {
                            tempFile.delete()
                            return@withContext Result.failure(IllegalArgumentException("Kích thước phông chữ vượt quá giới hạn 15MB"))
                        }
                        output.write(buffer, 0, read)
                    }
                }
            } ?: run {
                tempFile.delete()
                return@withContext Result.failure(IllegalArgumentException("Không thể đọc tệp phông chữ"))
            }

            if (totalBytes == 0L || !isValidFontHeader(headerBytes)) {
                tempFile.delete()
                return@withContext Result.failure(IllegalArgumentException("Tệp không phải là định dạng phông chữ hợp lệ (TTF/OTF)"))
            }

            // Deduplicate font by name
            val existing = customFontDao.getFontByName(fontName)
            if (existing != null) {
                tempFile.delete()
                return@withContext Result.success(existing)
            }

            val finalFile = File(fontsDir, sanitizedFileName)
            if (finalFile.exists()) {
                finalFile.delete()
            }
            tempFile.renameTo(finalFile)

            val id = UUID.randomUUID().toString()
            val entity = CustomFontEntity(
                id = id,
                fontFamily = fontName,
                fileName = sanitizedFileName,
                fileSize = totalBytes,
                createdAt = System.currentTimeMillis()
            )
            customFontDao.insertFont(entity)
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteFont(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val fonts = customFontDao.getAllFonts()
            val target = fonts.find { it.id == id }
            if (target != null) {
                val file = File(File(profileFiles, FONTS_DIR_NAME), target.fileName)
                if (file.exists()) {
                    file.delete()
                }
                customFontDao.deleteFont(id)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun queryFileName(uri: Uri): String? {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            return cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return uri.lastPathSegment?.let { File(it).name }
    }
}
