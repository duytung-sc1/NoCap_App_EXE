package com.nocap.app.data.catalog

import android.content.Context
import com.nocap.app.BuildConfig
import com.nocap.app.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

object CloudCatalog {
    val books = MutableStateFlow(SeedCatalogDataSource.books)
    val categories = MutableStateFlow(SeedCatalogDataSource.categories)
    fun start(context: Context) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val cache = File(context.filesDir, "cloud-catalog-v2.json")
            runCatching { if (cache.exists()) applyJson(cache.readText()) }
            runCatching {
                OkHttpClient().newCall(Request.Builder().url("${BuildConfig.BACKEND_BASE_URL}/api/v1/catalog").build()).execute().use {
                    check(it.isSuccessful) { "Không tải được danh mục" }
                    val text = it.body?.string() ?: error("Danh mục trống")
                    applyJson(text)
                    val temporary = File(context.filesDir, "cloud-catalog-v2.tmp")
                    temporary.writeText(text); check(temporary.renameTo(cache))
                }
            }
        }
    }
    internal fun applyJson(text: String) {
        val json = JSONObject(text)
        val bookRows = json.getJSONArray("books")
        val categoryRows = json.getJSONArray("categories")
        val parsedBooks = (0 until bookRows.length()).map { i ->
            val row = bookRows.getJSONObject(i)
            fun optional(key: String) = row.optString(key).takeIf { it.isNotBlank() && it != "null" }
            CatalogBook(id=row.getString("id"), title=row.getString("title"), author=row.getString("author"),
                description=row.optString("description"), sourceUrl=optional("sourceUrl"), coverUrl=row.getString("coverUrl"), categoryId=row.getString("categoryId"),
                fileUrl=row.optString("fileUrl"), fileSizeBytes=row.optLong("fileSizeBytes"), contentVersion=row.optLong("contentVersion",1),
                contentHash=optional("contentHash"), isFeatured=row.optBoolean("isFeatured"), isNew=row.optBoolean("isNew"),
                isPremium=row.optBoolean("isPremium"), playProductId=optional("playProductId"),
                entitlementType=runCatching { EntitlementType.valueOf(row.optString("entitlementType")) }.getOrDefault(EntitlementType.FREE),
                rating=row.optDouble("rating",0.0).toFloat(), publishedDate=optional("publishedDate"))
        }
        val parsedCategories = (0 until categoryRows.length()).map { i -> categoryRows.getJSONObject(i).let { Category(id=it.getString("id"),name=it.getString("name"),displayOrder=it.optInt("displayOrder")) } }
        books.value=parsedBooks; categories.value=parsedCategories
    }
}
