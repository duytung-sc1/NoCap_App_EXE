package com.nocap.app

import com.nocap.app.data.parser.ImageValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream

class ImageValidatorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `test valid PNG bounds detection`() {
        val file = tempFolder.newFile("sample.png")
        FileOutputStream(file).use { fos ->
            // PNG signature
            fos.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
            // IHDR chunk: length (13), type (IHDR)
            fos.write(byteArrayOf(0x00, 0x00, 0x00, 0x0D))
            fos.write("IHDR".toByteArray())
            // Width: 100 (0x64), Height: 200 (0xC8)
            fos.write(byteArrayOf(0x00, 0x00, 0x00, 0x64))
            fos.write(byteArrayOf(0x00, 0x00, 0x00, 0xC8.toByte()))
            // Bit depth, color type, compression, filter, interlace, CRC
            fos.write(ByteArray(9))
        }

        val bounds = ImageValidator.validateImageBounds(file)
        assertEquals(100, bounds.width)
        assertEquals(200, bounds.height)
        assertEquals("image/png", bounds.mimeType)
    }

    @Test
    fun `test valid JPEG bounds detection`() {
        val file = tempFolder.newFile("sample.jpg")
        FileOutputStream(file).use { fos ->
            // SOI
            fos.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte()))
            // SOF0 marker
            fos.write(byteArrayOf(0xFF.toByte(), 0xC0.toByte()))
            // Length (17), precision (8)
            fos.write(byteArrayOf(0x00, 0x11, 0x08))
            // Height: 60 (0x3C), Width: 80 (0x50)
            fos.write(byteArrayOf(0x00, 0x3C, 0x00, 0x50))
            // Components + EOI
            fos.write(ByteArray(10))
            fos.write(byteArrayOf(0xFF.toByte(), 0xD9.toByte()))
        }

        val bounds = ImageValidator.validateImageBounds(file)
        assertEquals(80, bounds.width)
        assertEquals(60, bounds.height)
        assertEquals("image/jpeg", bounds.mimeType)
    }

    @Test
    fun `test valid WebP VP8X bounds detection`() {
        val file = tempFolder.newFile("sample.webp")
        FileOutputStream(file).use { fos ->
            // RIFF header
            fos.write("RIFF".toByteArray())
            fos.write(byteArrayOf(0x20, 0x00, 0x00, 0x00))
            fos.write("WEBP".toByteArray())
            // VP8X chunk
            fos.write("VP8X".toByteArray())
            fos.write(byteArrayOf(0x0A, 0x00, 0x00, 0x00)) // size 10
            fos.write(byteArrayOf(0x00, 0x00, 0x00, 0x00)) // flags
            // Canvas width - 1 = 119 (0x77) -> 24-bit LE: 77 00 00
            fos.write(byteArrayOf(0x77, 0x00, 0x00))
            // Canvas height - 1 = 89 (0x59) -> 24-bit LE: 59 00 00
            fos.write(byteArrayOf(0x59, 0x00, 0x00))
        }

        val bounds = ImageValidator.validateImageBounds(file)
        assertEquals(120, bounds.width)
        assertEquals(90, bounds.height)
        assertEquals("image/webp", bounds.mimeType)
    }

    @Test
    fun `test reject oversized dimensions`() {
        val file = tempFolder.newFile("huge.png")
        FileOutputStream(file).use { fos ->
            fos.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
            fos.write(byteArrayOf(0x00, 0x00, 0x00, 0x0D))
            fos.write("IHDR".toByteArray())
            // Width: 20,000 (0x4E20)
            fos.write(byteArrayOf(0x00, 0x00, 0x4E, 0x20))
            // Height: 20,000 (0x4E20)
            fos.write(byteArrayOf(0x00, 0x00, 0x4E, 0x20))
            fos.write(ByteArray(9))
        }

        val ex = assertThrows(IllegalArgumentException::class.java) {
            ImageValidator.validateImageBounds(file)
        }
        assertTrue(ex.message?.contains("vượt quá giới hạn an toàn") == true)
    }

    @Test
    fun `test reject corrupt or zero byte file`() {
        val file = tempFolder.newFile("empty.png")
        val ex = assertThrows(IllegalArgumentException::class.java) {
            ImageValidator.validateImageBounds(file)
        }
        assertTrue(ex.message?.contains("không tồn tại") == true || ex.message?.contains("không hợp lệ") == true)
    }
}
