package com.nocap.app

import com.nocap.app.data.sync.RealtimeSyncClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RealtimeSyncClientTest {
    @Test fun `backend HTTP URLs become authenticated websocket endpoint`() {
        assertEquals("wss://api.example.test/api/v1/sync/live", RealtimeSyncClient.webSocketUrl("https://api.example.test/"))
        assertEquals("ws://127.0.0.1:8787/api/v1/sync/live", RealtimeSyncClient.webSocketUrl("http://127.0.0.1:8787"))
    }

    @Test fun `only explicit sync signal schedules a pull`() {
        assertTrue(RealtimeSyncClient.isSyncRequired("{\"type\":\"sync_required\",\"at\":1}"))
        assertFalse(RealtimeSyncClient.isSyncRequired("{\"type\":\"ready\"}"))
        assertFalse(RealtimeSyncClient.isSyncRequired("not-json"))
    }

    @Test fun `authentication failures end realtime session instead of retrying forever`() {
        assertTrue(RealtimeSyncClient.shouldEndSession(httpCode = 401))
        assertTrue(RealtimeSyncClient.shouldEndSession(httpCode = 403))
        assertTrue(RealtimeSyncClient.shouldEndSession(webSocketCloseCode = 1008))
        assertFalse(RealtimeSyncClient.shouldEndSession(httpCode = 429))
        assertFalse(RealtimeSyncClient.shouldEndSession(webSocketCloseCode = 1000))
        assertFalse(RealtimeSyncClient.shouldEndSession(webSocketCloseCode = 1011))
    }
}
