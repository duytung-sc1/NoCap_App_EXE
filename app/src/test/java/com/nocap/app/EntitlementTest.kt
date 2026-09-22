package com.nocap.app

import com.nocap.app.data.billing.*
import org.junit.Assert.*
import org.junit.Test

class EntitlementTest {
    private val now=1_000_000L
    private fun pro(user: String="a")=Entitlement(user,"PRO","ACTIVE",now+10000,now,"pro","obfuscated",true,true)

    @Test fun `server parsing roundtrip and account cache isolation`(){
        val text=pro().toJson();assertEquals(pro(),Entitlement.parse(text,"a"))
        assertThrows(IllegalArgumentException::class.java){Entitlement.parse(text,"b")}
        // All current features (including MULTI_DEVICE_SYNC) are now free for all users
        assertTrue(EntitlementPolicy.allows(Feature.MULTI_DEVICE_SYNC,pro(),"a",now))
        assertTrue(EntitlementPolicy.allows(Feature.MULTI_DEVICE_SYNC,Entitlement("a"),"a",now))
    }

    @Test fun `Free and expired plans keep all base features available including sync and cloud`(){
        val baseFeatures = Feature.entries.filter { it !in setOf(Feature.ADVANCED_READING_MEMORY, Feature.KNOWLEDGE_EXPORT, Feature.ADVANCED_CLOUD) }
        for(feature in baseFeatures) for(state in listOf(null,Entitlement("a"),pro().copy(status="EXPIRED",expiresAt=now-1)))
            assertTrue(feature.name,EntitlementPolicy.allows(feature,state,"a",now))
    }

    @Test fun `New Pro features require active Pro entitlement`(){
        val proFeatures = listOf(Feature.ADVANCED_READING_MEMORY, Feature.KNOWLEDGE_EXPORT, Feature.ADVANCED_CLOUD)
        for(feature in proFeatures) {
            // Free or expired -> false
            for(state in listOf(null, Entitlement("a"), pro().copy(status="EXPIRED", expiresAt=now-1))) {
                assertFalse(feature.name, EntitlementPolicy.allows(feature, state, "a", now))
            }
            assertFalse("${feature.name}: zero expiry", EntitlementPolicy.allows(feature, pro().copy(expiresAt=0), "a", now))
            assertFalse("${feature.name}: stale cache", EntitlementPolicy.allows(feature, pro().copy(updatedAt=now-24*60*60*1000L-1), "a", now))
            assertFalse("${feature.name}: wrong account", EntitlementPolicy.allows(feature, pro(), "b", now))
            // Active Pro -> true
            assertTrue(feature.name, EntitlementPolicy.allows(feature, pro(), "a", now))
        }
    }

    @Test fun `pending cancellation and already owned purchase outcomes`(){
        assertEquals(PurchaseOutcome.PENDING,PurchasePolicy.outcome(0,2))
        assertEquals(PurchaseOutcome.CANCELLED,PurchasePolicy.outcome(1,1))
        assertEquals(PurchaseOutcome.RESTORE,PurchasePolicy.outcome(7,1))
        assertEquals(PurchaseOutcome.VERIFY,PurchasePolicy.outcome(0,1))
        assertEquals(PurchaseOutcome.ERROR,PurchasePolicy.outcome(6,1))
    }

    @Test fun `restore same server account on another device is equivalent`(){
        val first=pro();val second=Entitlement.parse(first.toJson(),"a")
        assertEquals(EntitlementPolicy.allows(Feature.PRIVATE_CLOUD,first,"a",now),EntitlementPolicy.allows(Feature.PRIVATE_CLOUD,second,"a",now))
    }

    @Test fun `persisted cache survives restart without leaking A Pro to B or accepting corrupt data`(){
        val storage=mutableMapOf<String,String>()
        val cache=UserEntitlementCache({storage[it]},{k,v -> storage[k]=v})
        cache.save(pro());cache.save(Entitlement("b"))
        val reopened=UserEntitlementCache({storage[it]},{k,v -> storage[k]=v})
        assertEquals("PRO",reopened.load("a")!!.plan)
        assertEquals("FREE",reopened.load("b")!!.plan)
        assertNull(reopened.load("guest"))
        storage.keys.toList().forEach { storage[it]="broken JSON" }
        assertNull(reopened.load("a"))
    }

    @Test fun `withAccess executes successfully for free features`(){
        val db=java.sql.DriverManager.getConnection("jdbc:sqlite::memory:")
        db.use {
            it.createStatement().use { s -> s.execute("CREATE TABLE local_notes(text TEXT)");s.execute("INSERT INTO local_notes VALUES('synced note')");s.execute("CREATE TABLE outbox(revision INTEGER)");s.execute("INSERT INTO outbox VALUES(7)") }
            // Since MULTI_DEVICE_SYNC is now free, withAccess succeeds even with expired or free state
            EntitlementPolicy.withAccess(Feature.MULTI_DEVICE_SYNC,Entitlement("a"),"a",now){
                it.createStatement().use { s -> s.execute("DELETE FROM outbox") }
            }
            it.createStatement().use { s -> s.executeQuery("SELECT COUNT(*) FROM outbox").use { rows -> assertTrue(rows.next());assertEquals(0,rows.getInt(1)) } }
        }
    }
}
