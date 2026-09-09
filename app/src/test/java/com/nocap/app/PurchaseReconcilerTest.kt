package com.nocap.app

import com.nocap.app.data.billing.reconcilePurchases
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class PurchaseReconcilerTest {
    @Test fun `foreign token does not prevent own token and server restore`() = runBlocking {
        val events=mutableListOf<String>()
        val complete=reconcilePurchases(listOf("foreign","own"),{
            events+=it;if(it=="foreign")error("owned by another account")
        },{events+="server"})
        assertFalse(complete);assertEquals(listOf("foreign","own","server"),events)
    }
    @Test fun `empty Play query still recovers server entitlement on second device`() = runBlocking {
        var restored=false
        assertTrue(reconcilePurchases(emptyList<String>(),{error("unexpected")},{restored=true}))
        assertTrue(restored)
    }
    @Test fun `process cancellation does not continue verifying old account`() = runBlocking {
        var restored=false
        try {reconcilePurchases(listOf("a","b"),{throw CancellationException()},{restored=true});fail("Expected cancellation")}
        catch(_: CancellationException){assertFalse(restored)}
    }
    @Test fun `server restore error cannot be reported as successful`() = runBlocking {
        try {reconcilePurchases(listOf("own"),{}, {error("network unavailable")});fail("Expected error")}
        catch(e: IllegalStateException){assertEquals("network unavailable",e.message)}
    }
}
