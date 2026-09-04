package com.nocap.app

import com.nocap.app.core.database.Converters
import com.nocap.app.domain.model.DownloadStatus
import com.nocap.app.domain.model.EntitlementType
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `test EntitlementType converter serialization and deserialization`() {
        EntitlementType.entries.forEach { type ->
            val serialized = converters.fromEntitlementType(type)
            val deserialized = converters.toEntitlementType(serialized)
            assertEquals(type, deserialized)
        }
    }

    @Test
    fun `test EntitlementType converter unknown string defaults to FREE`() {
        val deserialized = converters.toEntitlementType("UNKNOWN_TYPE")
        assertEquals(EntitlementType.FREE, deserialized)
    }

    @Test
    fun `test DownloadStatus converter serialization and deserialization`() {
        DownloadStatus.entries.forEach { status ->
            val serialized = converters.fromDownloadStatus(status)
            val deserialized = converters.toDownloadStatus(serialized)
            assertEquals(status, deserialized)
        }
    }

    @Test
    fun `test DownloadStatus converter unknown string defaults to PENDING`() {
        val deserialized = converters.toDownloadStatus("INVALID_STATUS")
        assertEquals(DownloadStatus.PENDING, deserialized)
    }

    @Test
    fun `test PublicationFormat converter serialization and deserialization`() {
        com.nocap.app.domain.model.PublicationFormat.entries.forEach { format ->
            val serialized = converters.fromPublicationFormat(format)
            val deserialized = converters.toPublicationFormat(serialized)
            assertEquals(format, deserialized)
        }
    }

    @Test
    fun `test PublicationFormat converter unknown string defaults to EPUB`() {
        val deserialized = converters.toPublicationFormat("UNKNOWN_FORMAT")
        assertEquals(com.nocap.app.domain.model.PublicationFormat.EPUB, deserialized)
    }

    @Test
    fun `test PublicationSourceType converter serialization and deserialization`() {
        com.nocap.app.domain.model.PublicationSourceType.entries.forEach { source ->
            val serialized = converters.fromPublicationSourceType(source)
            val deserialized = converters.toPublicationSourceType(serialized)
            assertEquals(source, deserialized)
        }
    }

    @Test
    fun `test PublicationSourceType converter unknown string defaults to LOCAL_FILE`() {
        val deserialized = converters.toPublicationSourceType("UNKNOWN_SOURCE")
        assertEquals(com.nocap.app.domain.model.PublicationSourceType.LOCAL_FILE, deserialized)
    }
}
