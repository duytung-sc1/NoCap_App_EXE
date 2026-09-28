package com.nocap.app

import com.nocap.app.data.catalog.CloudCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CloudCatalogCacheTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `catalog cache replaces an existing snapshot and removes temporary file`() {
        val cache = temporaryFolder.newFile("cloud-catalog-v2.json").apply {
            writeText("old catalog")
        }

        CloudCatalog.replaceCache(cache, "new catalog")

        assertEquals("new catalog", cache.readText())
        assertFalse(File(temporaryFolder.root, "cloud-catalog-v2.json.tmp").exists())
    }
}
