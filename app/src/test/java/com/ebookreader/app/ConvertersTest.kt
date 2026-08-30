package com.ebookreader.app

import com.ebookreader.app.core.database.Converters
import com.ebookreader.app.domain.model.DownloadStatus
import com.ebookreader.app.domain.model.EntitlementType
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
}
