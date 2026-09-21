package com.nocap.app.data.catalog

import android.content.Context
import com.nocap.app.BuildConfig
import com.nocap.app.domain.model.Announcement
import com.nocap.app.domain.model.AnnouncementType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

object CloudAnnouncements {
    val announcements = MutableStateFlow<List<Announcement>>(emptyList())

    fun start(context: Context) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            // Load cache first for instant display
            val cache = File(context.filesDir, "cloud-announcements-v1.json")
            runCatching { if (cache.exists()) applyJson(cache.readText()) }
            // Then fetch fresh from server
            runCatching {
                OkHttpClient().newCall(
                    Request.Builder()
                        .url("${BuildConfig.BACKEND_BASE_URL}/api/v1/announcements")
                        .build()
                ).execute().use { response ->
                    check(response.isSuccessful) { "Không tải được thông báo" }
                    val text = response.body?.string() ?: error("Thông báo trống")
                    applyJson(text)
                    val tmp = File(context.filesDir, "cloud-announcements-v1.tmp")
                    tmp.writeText(text)
                    tmp.renameTo(cache)
                }
            }
        }
    }

    internal fun applyJson(text: String) {
        val json = JSONObject(text)
        val arr = json.optJSONArray("announcements") ?: return
        announcements.value = (0 until arr.length()).mapNotNull { i ->
            runCatching {
                val obj = arr.getJSONObject(i)
                Announcement(
                    id          = obj.getString("id"),
                    title       = obj.getString("title"),
                    message     = obj.getString("message"),
                    type        = AnnouncementType.from(obj.optString("type", "info")),
                    startsAt    = obj.optString("startsAt").takeIf { it.isNotBlank() && it != "null" },
                    endsAt      = obj.optString("endsAt").takeIf { it.isNotBlank() && it != "null" },
                    active      = obj.optBoolean("active", true)
                )
            }.getOrNull()
        }
    }
}
