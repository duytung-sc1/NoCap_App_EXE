package com.nocap.app.data.parser

import android.graphics.BitmapFactory
import java.io.File
import java.io.FileInputStream

object ImageValidator {

    const val MAX_IMAGE_FILE_SIZE: Long = 50L * 1024 * 1024 // 50 MB
    const val MAX_DIMENSION: Int = 16_384 // Max 16k width/height
    const val MAX_PIXELS: Long = 100_000_000L // Max 100 Megapixels

    data class ImageBounds(val width: Int, val height: Int, val mimeType: String?)

    fun validateImageBounds(file: File): ImageBounds {
        if (!file.exists() || file.length() == 0L) {
            throw IllegalArgumentException("Tệp ảnh không tồn tại: ${file.name}")
        }
        if (file.length() > MAX_IMAGE_FILE_SIZE) {
            throw IllegalArgumentException("Kích thước tệp ảnh (${file.length()} bytes) vượt quá giới hạn 50MB")
        }

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        try {
            FileInputStream(file).use { fis ->
                BitmapFactory.decodeStream(fis, null, options)
            }
        } catch (_: Exception) {}

        var width = options.outWidth
        var height = options.outHeight
        var mimeType = options.outMimeType

        if (width <= 0 || height <= 0) {
            val fallback = readDimensionsFromHeader(file)
            if (fallback != null) {
                width = fallback.width
                height = fallback.height
                mimeType = fallback.mimeType
            }
        }

        if (width <= 0 || height <= 0) {
            throw IllegalArgumentException("Tệp không phải là định dạng hình ảnh hợp lệ hoặc dữ liệu bị hỏng.")
        }

        if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
            throw IllegalArgumentException("Kích thước ảnh (${width}x${height}) vượt quá giới hạn an toàn ($MAX_DIMENSION px)")
        }

        val totalPixels = width.toLong() * height.toLong()
        if (totalPixels > MAX_PIXELS) {
            throw IllegalArgumentException("Độ phân giải ảnh (${totalPixels} pixels) quá lớn, có nguy cơ gây tràn bộ nhớ (OOM).")
        }

        return ImageBounds(width, height, mimeType)
    }

    private fun readDimensionsFromHeader(file: File): ImageBounds? {
        return try {
            val header = ByteArray(64)
            val read = FileInputStream(file).use { it.read(header) }
            if (read < 12) return null

            // 1. PNG
            if (header[0] == 0x89.toByte() && header[1] == 0x50.toByte() && header[2] == 0x4E.toByte() && header[3] == 0x47.toByte()) {
                if (read >= 24) {
                    val w = ((header[16].toInt() and 0xFF) shl 24) or ((header[17].toInt() and 0xFF) shl 16) or
                            ((header[18].toInt() and 0xFF) shl 8) or (header[19].toInt() and 0xFF)
                    val h = ((header[20].toInt() and 0xFF) shl 24) or ((header[21].toInt() and 0xFF) shl 16) or
                            ((header[22].toInt() and 0xFF) shl 8) or (header[23].toInt() and 0xFF)
                    if (w > 0 && h > 0) return ImageBounds(w, h, "image/png")
                }
            }

            // 2. WebP
            if (header[0] == 'R'.code.toByte() && header[1] == 'I'.code.toByte() && header[2] == 'F'.code.toByte() && header[3] == 'F'.code.toByte() &&
                header[8] == 'W'.code.toByte() && header[9] == 'E'.code.toByte() && header[10] == 'B'.code.toByte() && header[11] == 'P'.code.toByte()
            ) {
                if (read >= 16) {
                    val chunkType = String(header, 12, 4)
                    when (chunkType) {
                        "VP8 " -> {
                            if (read >= 30) {
                                val w = ((header[26].toInt() and 0xFF) or ((header[27].toInt() and 0xFF) shl 8)) and 0x3FFF
                                val h = ((header[28].toInt() and 0xFF) or ((header[29].toInt() and 0xFF) shl 8)) and 0x3FFF
                                if (w > 0 && h > 0) return ImageBounds(w, h, "image/webp")
                            }
                        }
                        "VP8X" -> {
                            if (read >= 30) {
                                val w = 1 + ((header[24].toInt() and 0xFF) or ((header[25].toInt() and 0xFF) shl 8) or ((header[26].toInt() and 0xFF) shl 16))
                                val h = 1 + ((header[27].toInt() and 0xFF) or ((header[28].toInt() and 0xFF) shl 8) or ((header[29].toInt() and 0xFF) shl 16))
                                if (w > 0 && h > 0) return ImageBounds(w, h, "image/webp")
                            }
                        }
                        "VP8L" -> {
                            if (read >= 25 && header[20] == 0x2F.toByte()) {
                                val b0 = header[21].toInt() and 0xFF
                                val b1 = header[22].toInt() and 0xFF
                                val b2 = header[23].toInt() and 0xFF
                                val b3 = header[24].toInt() and 0xFF
                                val w = 1 + (((b1 and 0x3F) shl 8) or b0)
                                val h = 1 + (((b3 and 0x0F) shl 10) or (b2 shl 2) or ((b1 and 0xC0) shr 6))
                                if (w > 0 && h > 0) return ImageBounds(w, h, "image/webp")
                            }
                        }
                    }
                }
            }

                // 3. JPEG
                if (header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte()) {
                    // Scan JPEG markers
                    val fullBytes = file.readBytes()
                    var offset = 2
                    while (offset < fullBytes.size - 8) {
                        if (fullBytes[offset] == 0xFF.toByte()) {
                            val marker = fullBytes[offset + 1].toInt() and 0xFF
                            offset += 2
                            if (marker == 0xD9 || marker == 0xDA) break // EOI or SOS
                            val len = ((fullBytes[offset].toInt() and 0xFF) shl 8) or (fullBytes[offset + 1].toInt() and 0xFF)
                            if (marker in 0xC0..0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC) {
                                val h = ((fullBytes[offset + 3].toInt() and 0xFF) shl 8) or (fullBytes[offset + 4].toInt() and 0xFF)
                                val w = ((fullBytes[offset + 5].toInt() and 0xFF) shl 8) or (fullBytes[offset + 6].toInt() and 0xFF)
                                if (w > 0 && h > 0) return ImageBounds(w, h, "image/jpeg")
                            }
                            offset += len
                        } else {
                            offset++
                        }
                    }
                }

            null
        } catch (_: Exception) {
            null
        }
    }
}
