package com.nocap.app.data.cloud

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import androidx.room.withTransaction
import com.nocap.app.BuildConfig
import com.nocap.app.core.database.AppDatabase
import com.nocap.app.data.auth.CloudAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import com.nocap.app.data.sync.SyncEngine
import com.nocap.app.data.sync.SyncScheduler

data class CloudBackupSnapshot(
    val id: String,
    val size: Long,
    val updatedAt: String?,
    val deviceName: String?
)

class CloudBackupRepository(private val context: Context) {
    private val profile = com.nocap.app.data.sync.Profiles.active.value
    private val profileFiles = com.nocap.app.data.sync.Profiles.files(context, profile)

    private val database = AppDatabase.getInstance(context, profile)
    private val client = OkHttpClient.Builder().readTimeout(120, TimeUnit.SECONDS).writeTimeout(120, TimeUnit.SECONDS).build()

    private suspend fun token(requireCloud: Boolean = true): String {
        val auth = CloudAuthRepository.getInstance(context)
        val token = auth.getIdToken(false) ?: error("Vui lòng đăng nhập")
        val user = client.newCall(Request.Builder().url("${BuildConfig.BACKEND_BASE_URL}/api/v1/me").header("Authorization", "Bearer $token").build()).execute().use {
            check(it.isSuccessful) { "Phiên đăng nhập hết hạn" }; JSONObject(it.body!!.string()).getString("id")
        }
        check(profile == "ACCOUNT:$user") { "Tài khoản đã thay đổi; vui lòng mở lại trang sao lưu" }
        if (requireCloud) com.nocap.app.data.billing.EntitlementRepository.get(context).require(com.nocap.app.data.billing.Feature.CLOUD_BACKUP, profile)
        return token
    }

    private fun call(path: String, token: String, method: String = "GET", body: RequestBody? = null): okhttp3.Response {
        val response = client.newCall(Request.Builder().url("${BuildConfig.BACKEND_BASE_URL}/api/v1/cloud/$path")
            .header("Authorization", "Bearer $token").method(method, body).build()).execute()
        if (!response.isSuccessful) {
            val message = response.use { runCatching { JSONObject(it.body?.string().orEmpty()).getJSONObject("error").getString("message") }.getOrDefault("Không kết nối được kho lưu trữ (${it.code})") }
            error(message)
        }
        return response
    }

    suspend fun listBackups(): Result<List<CloudBackupSnapshot>> = withContext(Dispatchers.IO) { runCatching {
        val session = token()
        val response = call("backups", session)
        val json = JSONObject(response.use { it.body!!.string() })
        val array = json.optJSONArray("backups") ?: JSONArray()
        val list = mutableListOf<CloudBackupSnapshot>()
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            list.add(
                CloudBackupSnapshot(
                    id = item.getString("id"),
                    size = item.optLong("size", 0L),
                    updatedAt = item.optString("updatedAt").takeIf { it.isNotBlank() },
                    deviceName = item.optString("deviceName").takeIf { it.isNotBlank() }
                )
            )
        }
        list
    } }

    private fun tables(): List<String> = database.openHelper.writableDatabase.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'sync_%' AND name NOT IN ('android_metadata','room_master_table')").use { cursor ->
        buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
    }

    private fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun requireIdleDownloads() {
        database.openHelper.writableDatabase.query("SELECT COUNT(*) FROM downloaded_books WHERE download_status IN ('PENDING','DOWNLOADING')").use {
            it.moveToFirst(); check(it.getInt(0) == 0) { "Vui lòng hoàn tất hoặc hủy các lượt tải sách trước khi sao lưu/khôi phục." }
        }
    }

    suspend fun backup(): Result<String> = withContext(Dispatchers.IO) { runCatching {
        val session = token()
        val file = File.createTempFile("cloud-backup-", ".zip", context.cacheDir)
        try {
            val data = database.withTransaction {
                requireIdleDownloads()
                JSONObject().put("version", database.openHelper.writableDatabase.version).put("filesRoot", profileFiles.absolutePath).put("tables", JSONObject().apply {
                    for (table in tables()) {
                        val rows = JSONArray()
                        database.openHelper.writableDatabase.query("SELECT * FROM \"$table\"").use { cursor ->
                            while (cursor.moveToNext()) {
                                val row = JSONObject()
                                cursor.columnNames.forEachIndexed { i, name ->
                                    row.put(name, when (cursor.getType(i)) {
                                        Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
                                        Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i)
                                        Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(i)
                                        Cursor.FIELD_TYPE_BLOB -> JSONObject().put("blob", Base64.encodeToString(cursor.getBlob(i), Base64.NO_WRAP))
                                        else -> cursor.getString(i)
                                    })
                                }
                                rows.put(row)
                            }
                        }
                        put(table, rows)
                    }
                })
            }
            data.put("preferences", CloudPreferences(context).snapshot())
            check(data.toString().toByteArray().size <= 64 * 1024 * 1024) { "Dữ liệu thư viện quá lớn" }
            ZipOutputStream(file.outputStream().buffered()).use { zip ->
                zip.putNextEntry(ZipEntry("database.json")); zip.write(data.toString().toByteArray()); zip.closeEntry()
                var total = 0L
                profileFiles.walkTopDown().onEnter { it.name !in setOf("temp", "datastore", "profiles") }.filter { it.isFile }.forEach { source ->
                    total += source.length(); check(total <= 2L * 1024 * 1024 * 1024) { "Thư viện vượt giới hạn sao lưu 2 GiB" }
                    val relative = source.relativeTo(profileFiles).invariantSeparatorsPath
                    zip.putNextEntry(ZipEntry("files/$relative")); source.inputStream().use { it.copyTo(zip) }; zip.closeEntry()
                }
            }
            val chunks = JSONArray()
            check(file.length() <= 2000L * 1024 * 1024) { "Bản sao lưu nén vượt giới hạn 2000 MiB" }
            file.inputStream().use { input ->
                val buffer = ByteArray(20 * 1024 * 1024)
                while (true) {
                    var size = 0
                    while (size < buffer.size) { val read = input.read(buffer, size, buffer.size - size); if (read < 0) break; size += read }
                    if (size == 0) break
                    val bytes = buffer.copyOf(size); val digest = hash(bytes)
                    call("objects/$digest", session, "PUT", bytes.toRequestBody("application/octet-stream".toMediaType())).close()
                    chunks.put(digest)
                }
            }
            val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                .trim().ifBlank { "Android" }.take(100)
            val manifest = JSONObject().put("version", 1).put("chunks", chunks).put("size", file.length()).put("deviceName", deviceName)
            call("backup", session, "PUT", manifest.toString().toRequestBody("application/json".toMediaType())).close()
            "Đã sao lưu thư viện lên đám mây (${file.length() / 1024 / 1024} MiB)."
        } finally { file.delete() }
    } }

    suspend fun restore(snapshotId: String? = null): Result<String> = withContext(Dispatchers.IO) { SyncScheduler.lock.withLock { runCatching {
        val session = token()
        // Pull the latest tombstones before replacing the local snapshot. This lets
        // restored rows explicitly recreate records deleted on another device or
        // before a reinstall instead of being removed again moments later.
        SyncEngine(context, profile).run()
        val path = if (snapshotId != null && snapshotId != "latest") "backups/$snapshotId" else "backup"
        val manifest = call(path, session).use { JSONObject(it.body!!.string()) }
        check(manifest.getInt("version") == 1) { "Phiên bản sao lưu không được hỗ trợ" }
        val chunks = manifest.getJSONArray("chunks")
        check(chunks.length() in 1..100)
        val zipFile = File.createTempFile("cloud-restore-", ".zip", context.cacheDir)
        val staging = File(profileFiles, "restored-${UUID.randomUUID()}").apply { mkdirs() }
        var committed = false
        val restoredFonts = mutableListOf<File>()
        try {
            zipFile.outputStream().use { output ->
                for (i in 0 until chunks.length()) {
                    val digest = chunks.getString(i); check(Regex("[a-f0-9]{64}").matches(digest))
                    val bytes = call("objects/$digest", session).use { response ->
                        val body = response.body ?: error("Tệp trống"); check(body.contentLength() in 1..20L * 1024 * 1024); body.bytes()
                    }
                    check(hash(bytes) == digest) { "Bản sao lưu bị hỏng" }; output.write(bytes)
                }
            }
            check(zipFile.length() == manifest.getLong("size")) { "Bản sao lưu thiếu dữ liệu" }
            CloudArchive.extract(zipFile, staging)
            val dbFile = File(staging, "database.json"); check(dbFile.length() <= 64L * 1024 * 1024) { "Dữ liệu sao lưu quá lớn" }
            val data = JSONObject(dbFile.readText())
            check(data.getInt("version") == database.openHelper.writableDatabase.version) { "Cần dùng cùng phiên bản ứng dụng để khôi phục" }
            val oldRoot = data.getString("filesRoot").trimEnd('/') + "/"
            val newRoot = File(staging, "files").absolutePath + "/"
            val rows = data.getJSONObject("tables")
            database.withTransaction {
                requireIdleDownloads()
                val db = database.openHelper.writableDatabase
                val known = tables(); check(rows.keys().asSequence().toSet() == known.toSet()) { "Cấu trúc dữ liệu không hợp lệ" }
                db.execSQL("PRAGMA defer_foreign_keys=ON")
                BackupRestoreSyncState.prepare(db)
                known.reversed().forEach { db.execSQL("DELETE FROM \"$it\"") }
                BackupRestoreSyncState.observeRestoredRows(db)
                for (table in known) {
                    val columns = db.query("PRAGMA table_info(\"$table\")").use { cursor -> buildSet { while (cursor.moveToNext()) add(cursor.getString(1)) } }
                    val blobColumns = db.query("PRAGMA table_info(\"$table\")").use { cursor -> buildSet {
                        while (cursor.moveToNext()) if (cursor.getString(2).equals("BLOB", ignoreCase = true)) add(cursor.getString(1))
                    } }
                    val values = rows.getJSONArray(table)
                    for (i in 0 until values.length()) {
                        val row = values.getJSONObject(i); check(row.keys().asSequence().toSet() == columns)
                        val content = ContentValues()
                        for (column in columns) {
                            when (val value = row.get(column)) {
                                JSONObject.NULL -> content.putNull(column)
                                is JSONObject -> {
                                    check(column in blobColumns) { "Kiểu dữ liệu sao lưu không hợp lệ" }
                                    content.put(column, Base64.decode(value.getString("blob"), Base64.NO_WRAP))
                                }
                                is Int -> content.put(column, value)
                                is Long -> content.put(column, value)
                                is Number -> content.put(column, value.toDouble())
                                is String -> {
                                    var restored = CloudArchive.restoredPath(table, column, value, oldRoot, File(newRoot))
                                    if (table == "catalog_books" && column == "id") {
                                        require(com.nocap.app.core.util.DocumentIds.isSafe(value)) { "Mã tài liệu không hợp lệ" }
                                    }
                                    if (table == "custom_fonts" && column == "file_name") {
                                        val source = File(newRoot, "custom_fonts/$value").canonicalFile
                                        check(source.path.startsWith(File(newRoot).canonicalPath + File.separator) && source.isFile) { "Thiếu tệp phông chữ" }
                                        val target = File(File(profileFiles, "custom_fonts").apply { mkdirs() }, "${UUID.randomUUID()}-${source.name}")
                                        source.copyTo(target); restoredFonts.add(target); restored = target.name
                                    }
                                    content.put(column, restored)
                                }
                                else -> error("Giá trị sao lưu không hợp lệ")
                            }
                        }
                        db.insert(table, SQLiteDatabase.CONFLICT_ABORT, content)
                    }
                }
                db.query("PRAGMA foreign_key_check").use { check(!it.moveToFirst()) { "Liên kết dữ liệu sao lưu bị lỗi" } }
            }
            committed = true; dbFile.delete()
            data.optJSONObject("preferences")?.let { CloudPreferences(context).restore(it) }
            database.invalidationTracker.refreshVersionsAsync()
            SyncScheduler.now(context, profile)
            "Đã khôi phục thư viện. Đóng và mở lại ứng dụng để tải lại toàn bộ dữ liệu."
        } finally { zipFile.delete(); if (!committed) { staging.deleteRecursively(); restoredFonts.forEach { it.delete() } } }
    } } }

    suspend fun deleteBackup(snapshotId: String? = null): Result<String> = withContext(Dispatchers.IO) { runCatching {
        if (snapshotId != null && snapshotId != "latest") {
            call("backups/$snapshotId", token(requireCloud = false), "DELETE").close()
            "Đã xóa bản sao lưu."
        } else {
            call("backup", token(requireCloud = false), "DELETE").close()
            "Đã xóa bản sao lưu trên đám mây."
        }
    } }
}
