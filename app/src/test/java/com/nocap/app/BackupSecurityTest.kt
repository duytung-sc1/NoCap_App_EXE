package com.nocap.app

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

class BackupSecurityTest {
    private fun xml(path: String) = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
    }.newDocumentBuilder().parse(File("src/main/$path"))

    @Test fun systemBackupExcludesSessionAndEntitlementForBothTransports() {
        val manifest = xml("AndroidManifest.xml").getElementsByTagName("application").item(0) as Element
        val ns = "http://schemas.android.com/apk/res/android"
        assertEquals("@xml/backup_rules", manifest.getAttributeNS(ns, "fullBackupContent"))
        assertEquals("@xml/data_extraction_rules", manifest.getAttributeNS(ns, "dataExtractionRules"))
        val legacy = xml("res/xml/backup_rules.xml").documentElement
        val modern = xml("res/xml/data_extraction_rules.xml")
        val policies = listOf(legacy, modern.getElementsByTagName("cloud-backup").item(0) as Element,
            modern.getElementsByTagName("device-transfer").item(0) as Element)
        for (policy in policies) {
            val exclusions = policy.getElementsByTagName("exclude")
            val paths = (0 until exclusions.length).map { exclusions.item(it) as Element }
            for (file in listOf("cloud_auth.xml", "entitlements-v1.xml")) {
                assertTrue(paths.any { it.getAttribute("domain") == "sharedpref" && it.getAttribute("path") == file })
            }
            // Local documents/databases remain eligible for users' existing backup flow.
            assertTrue(paths.none { it.getAttribute("domain") in setOf("database", "file") })
        }
    }
}
