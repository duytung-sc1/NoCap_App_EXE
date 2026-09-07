package com.nocap.app

import com.nocap.app.domain.model.ArchiveLocator
import com.nocap.app.domain.model.DocumentLocator
import com.nocap.app.domain.model.ImageLocator
import com.nocap.app.domain.model.TextLocator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentLocatorTest {

    @Test
    fun `test TextLocator serialization and deserialization`() {
        val original = TextLocator(
            blockIndex = 4,
            characterOffset = 120,
            progression = 0.35f,
            snippet = "Đoạn văn trích dẫn"
        )
        val json = original.toJson()
        assertTrue(json.contains("\"type\":\"TEXT\""))

        val deserialized = DocumentLocator.fromJson(json) as? TextLocator
        assertNotNull(deserialized)
        assertEquals(4, deserialized?.blockIndex)
        assertEquals(120, deserialized?.characterOffset)
        assertEquals(0.35f, deserialized?.progression ?: 0f, 0.001f)
        assertEquals("Đoạn văn trích dẫn", deserialized?.snippet)

        val directDeserialized = TextLocator.fromJson(json)
        assertEquals(original, directDeserialized)
    }

    @Test
    fun `test ArchiveLocator serialization and deserialization`() {
        val original = ArchiveLocator(
            pageIndex = 12,
            entryName = "chapter1/page_12.jpg",
            progression = 0.5f
        )
        val json = original.toJson()
        assertTrue(json.contains("\"type\":\"ARCHIVE\""))

        val deserialized = DocumentLocator.fromJson(json) as? ArchiveLocator
        assertNotNull(deserialized)
        assertEquals(12, deserialized?.pageIndex)
        assertEquals("chapter1/page_12.jpg", deserialized?.entryName)
        assertEquals(0.5f, deserialized?.progression ?: 0f, 0.001f)

        val directDeserialized = ArchiveLocator.fromJson(json)
        assertEquals(original, directDeserialized)
    }

    @Test
    fun `test ImageLocator serialization and deserialization`() {
        val original = ImageLocator(progression = 1.0f)
        val json = original.toJson()
        assertTrue(json.contains("\"type\":\"IMAGE\""))

        val deserialized = DocumentLocator.fromJson(json) as? ImageLocator
        assertNotNull(deserialized)
        assertEquals(1.0f, deserialized?.progression ?: 0f, 0.001f)

        val directDeserialized = ImageLocator.fromJson(json)
        assertEquals(original, directDeserialized)
    }

    @Test
    fun `test invalid or empty json returns null`() {
        assertNull(DocumentLocator.fromJson(""))
        assertNull(DocumentLocator.fromJson("   "))
        assertNull(DocumentLocator.fromJson("{not valid json}"))
        assertNull(DocumentLocator.fromJson("{\"type\":\"UNKNOWN\"}"))
    }
}
