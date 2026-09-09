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
        assertFalse(EntitlementPolicy.allows(Feature.MULTI_DEVICE_SYNC,pro(),"b",now))
        assertFalse(EntitlementPolicy.allows(Feature.MULTI_DEVICE_SYNC,pro(),null,now))
        assertTrue(EntitlementPolicy.allows(Feature.MULTI_DEVICE_SYNC,Entitlement.parse(text,"a"),"a",now))
    }
    @Test fun `Free and expired plans keep all local features available`(){
        val local=Feature.entries.filter { it !in setOf(Feature.MULTI_DEVICE_SYNC,Feature.PRIVATE_CLOUD,Feature.CLOUD_BACKUP) }
        for(feature in local)for(state in listOf(null,Entitlement("a"),pro().copy(status="EXPIRED",expiresAt=now-1)))
            assertTrue(feature.name,EntitlementPolicy.allows(feature,state,"a",now))
    }
    @Test fun `Pro expiry offline cache and revoke gate only cloud`(){
        for(feature in listOf(Feature.MULTI_DEVICE_SYNC,Feature.PRIVATE_CLOUD,Feature.CLOUD_BACKUP)){
            assertTrue(EntitlementPolicy.allows(feature,pro(),"a",now))
            assertFalse(EntitlementPolicy.allows(feature,Entitlement("a"),"a",now))
            assertFalse(EntitlementPolicy.allows(feature,pro(),"a",now+10001))
            assertFalse(EntitlementPolicy.allows(feature,pro().copy(status="INVALID"),"a",now))
            assertFalse(EntitlementPolicy.allows(feature,pro().copy(expiresAt=Long.MAX_VALUE),"a",now+86_400_001))
            assertFalse(EntitlementPolicy.allows(feature,pro(),"a",now-300001))
        }
    }
    @Test fun `pending cancellation and already owned never grant Pro locally`(){
        assertEquals(PurchaseOutcome.PENDING,PurchasePolicy.outcome(0,2))
        assertEquals(PurchaseOutcome.CANCELLED,PurchasePolicy.outcome(1,1))
        assertEquals(PurchaseOutcome.RESTORE,PurchasePolicy.outcome(7,1))
        assertEquals(PurchaseOutcome.VERIFY,PurchasePolicy.outcome(0,1))
        assertEquals(PurchaseOutcome.ERROR,PurchasePolicy.outcome(6,1))
        assertFalse(EntitlementPolicy.allows(Feature.MULTI_DEVICE_SYNC,pro().copy(status="PENDING"),"a",now))
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
    @Test fun `expired entitlement does not mutate local data or pending outbox`(){
        val db=java.sql.DriverManager.getConnection("jdbc:sqlite::memory:")
        db.use {
            it.createStatement().use { s -> s.execute("CREATE TABLE local_notes(text TEXT)");s.execute("INSERT INTO local_notes VALUES('unsynced note')");s.execute("CREATE TABLE outbox(revision INTEGER)");s.execute("INSERT INTO outbox VALUES(7)") }
            assertThrows(ProRequired::class.java){
                EntitlementPolicy.withAccess(Feature.MULTI_DEVICE_SYNC,pro(),"a",now+10001){
                    it.createStatement().use { s -> s.execute("DELETE FROM outbox") }
                }
            }
            it.createStatement().use { s -> s.executeQuery("SELECT text,revision FROM local_notes,outbox").use { rows -> assertTrue(rows.next());assertEquals("unsynced note",rows.getString(1));assertEquals(7,rows.getInt(2)) } }
            EntitlementPolicy.withAccess(Feature.MULTI_DEVICE_SYNC,pro(),"a",now){
                it.createStatement().use { s -> s.execute("DELETE FROM outbox") }
            }
            it.createStatement().use { s -> s.executeQuery("SELECT COUNT(*) FROM outbox").use { rows -> assertTrue(rows.next());assertEquals(0,rows.getInt(1)) } }
        }
    }
}
