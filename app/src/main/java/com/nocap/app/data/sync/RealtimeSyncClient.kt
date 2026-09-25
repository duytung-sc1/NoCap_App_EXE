package com.nocap.app.data.sync

import android.content.Context
import com.nocap.app.BuildConfig
import com.nocap.app.core.datastore.DevicePreferencesDataStore
import com.nocap.app.data.auth.CloudAuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Foreground-only signal channel. D1/Room remain the source of truth. */
internal object RealtimeSyncClient {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .pingInterval(25, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private var generation = 0L
    private var profile: String? = null
    private var socket: WebSocket? = null
    private var connectJob: Job? = null
    private var signalJob: Job? = null
    private var reconnectDelayMs = 1_000L
    private var foreground = false

    fun start(context: Context, requestedProfile: String) {
        synchronized(this) {
            foreground = true
            if (!requestedProfile.startsWith("ACCOUNT:") || requestedProfile != Profiles.active.value) {
                disconnectLocked(clearForeground = false)
                return@synchronized
            }
            if (profile == requestedProfile && (socket != null || connectJob?.isActive == true)) return@synchronized
            disconnectLocked(clearForeground = false)
            profile = requestedProfile
            generation += 1
            reconnectDelayMs = 1_000L
            connect(context.applicationContext, requestedProfile, generation, 0)
        }
    }

    fun stop() = synchronized(this) { disconnectLocked(clearForeground = true) }

    private fun disconnectLocked(clearForeground: Boolean) {
        generation += 1
        connectJob?.cancel()
        connectJob = null
        signalJob?.cancel()
        signalJob = null
        socket?.close(1000, "App backgrounded")
        socket = null
        profile = null
        reconnectDelayMs = 1_000L
        if (clearForeground) foreground = false
    }

    private fun connect(context: Context, expectedProfile: String, expectedGeneration: Long, delayMs: Long) {
        connectJob?.cancel()
        connectJob = scope.launch {
            if (delayMs > 0) delay(delayMs)
            val auth = CloudAuthRepository.getInstance(context)
            val token = auth.getIdToken(false)
            val installationId = DevicePreferencesDataStore(context).getOrCreateInstallationId()
            synchronized(this@RealtimeSyncClient) {
                if (!foreground || generation != expectedGeneration || profile != expectedProfile || Profiles.active.value != expectedProfile || token == null) return@launch
                val request = Request.Builder()
                    .url(webSocketUrl(BuildConfig.BACKEND_BASE_URL))
                    .header("Authorization", "Bearer $token")
                    .header("X-NoCap-Device", installationId)
                    .build()
                socket = client.newWebSocket(request, listener(context, expectedProfile, expectedGeneration, token))
            }
        }
    }

    private fun listener(context: Context, expectedProfile: String, expectedGeneration: Long, expectedToken: String) = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            synchronized(this@RealtimeSyncClient) {
                if (generation != expectedGeneration || profile != expectedProfile || !foreground) {
                    webSocket.close(1000, "Stale connection")
                } else {
                    socket = webSocket
                    connectJob = null
                    reconnectDelayMs = 1_000L
                }
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            if (!isSyncRequired(text)) return
            synchronized(this@RealtimeSyncClient) {
                if (generation != expectedGeneration || profile != expectedProfile || !foreground) return
                signalJob?.cancel()
                signalJob = scope.launch {
                    delay(150)
                    if (Profiles.active.value == expectedProfile) SyncScheduler.immediate(context, expectedProfile)
                }
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (shouldEndSession(webSocketCloseCode = code)) {
                endSession(webSocket, context, expectedProfile, expectedGeneration, expectedToken)
            } else {
                reconnect(webSocket, context, expectedProfile, expectedGeneration)
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (shouldEndSession(httpCode = response?.code)) {
                endSession(webSocket, context, expectedProfile, expectedGeneration, expectedToken)
            } else {
                reconnect(webSocket, context, expectedProfile, expectedGeneration)
            }
        }
    }

    private fun endSession(webSocket: WebSocket, context: Context, expectedProfile: String, expectedGeneration: Long, expectedToken: String) {
        val shouldSignOut = synchronized(this) {
            if (socket !== webSocket || generation != expectedGeneration || profile != expectedProfile) false
            else {
                disconnectLocked(clearForeground = false)
                true
            }
        }
        if (shouldSignOut) scope.launch { CloudAuthRepository.getInstance(context).invalidateSession(expectedToken) }
    }

    private fun reconnect(webSocket: WebSocket, context: Context, expectedProfile: String, expectedGeneration: Long) {
        synchronized(this) {
            if (socket === webSocket) socket = null
            if (!foreground || generation != expectedGeneration || profile != expectedProfile || Profiles.active.value != expectedProfile) return@synchronized
            if (connectJob?.isActive == true) return@synchronized
            val wait = reconnectDelayMs
            reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(30_000L)
            connect(context, expectedProfile, expectedGeneration, wait)
        }
    }

    internal fun webSocketUrl(baseUrl: String): String {
        val normalized = baseUrl.trimEnd('/')
        val socketBase = when {
            normalized.startsWith("https://") -> "wss://${normalized.removePrefix("https://")}"
            normalized.startsWith("http://") -> "ws://${normalized.removePrefix("http://")}"
            else -> error("Backend URL must use HTTP(S)")
        }
        return "$socketBase/api/v1/sync/live"
    }

    internal fun isSyncRequired(message: String): Boolean = runCatching {
        val value = JSONObject(message)
        value.length() <= 3 && value.optString("type") == "sync_required"
    }.getOrDefault(false)

    internal fun shouldEndSession(httpCode: Int? = null, webSocketCloseCode: Int? = null): Boolean =
        httpCode == 401 || httpCode == 403 || webSocketCloseCode == 1008
}
