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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStream
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
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
        .build()

    private suspend fun <T> retryIO(
        times: Int = 3,
        initialDelayMs: Long = 800,
        factor: Double = 1.5,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMs
        repeat(times - 1) {
            try {
                return block()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val msg = e.message.orEmpty()
                if (msg.contains("401") || msg.contains("403") || msg.contains("404")) throw e
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(3000L)
            }
        }
        return block()
    }

    private data class BackupScope(
        val profile: String,
        val files: File,
        val database: AppDatabase
    )

    private fun currentScope(): BackupScope {
        val profile = com.nocap.app.data.sync.Profiles.active.value
        check(profile.startsWith("ACCOUNT:")) { "Vui lòng đăng nhập để sao lưu hoặc khôi phục" }
        return BackupScope(
            profile = profile,
            files = com.nocap.app.data.sync.Profiles.files(context, profile),
            database = AppDatabase.getInstance(context, profile)
        )
    }

    private suspend fun token(profile: String): String {
        val auth = CloudAuthRepository.getInstance(context)
        val token = auth.getIdToken(false) ?: error("Vui lòng đăng nhập")
        val user = retryIO {
            client.newCall(Request.Builder().url("${BuildConfig.BACKEND_BASE_URL}/api/v1/me").header("Authorization", "Bearer $token").build()).execute().use {
                check(it.isSuccessful) { "Phiên đăng nhập hết hạn" }; JSONObject(it.body!!.string()).getString("id")
            }
        }
        check(profile == "ACCOUNT:$user") { "Tài khoản đã thay đổi; vui lòng mở lại trang sao lưu" }
        return token
    }

    private fun executeCall(path: String, token: String, method: String = "GET", body: RequestBody? = null): okhttp3.Response {
        val response = client.newCall(Request.Builder().url("${BuildConfig.BACKEND_BASE_URL}/api/v1/cloud/$path")
            .header("Authorization", "Bearer $token").method(method, body).build()).execute()
        if (!response.isSuccessful) {
            val message = response.use { runCatching { JSONObject(it.body?.string().orEmpty()).getJSONObject("error").getString("message") }.getOrDefault("Không kết nối được kho lưu trữ (${it.code})") }
            error(message)
        }
        return response
    }

    private suspend fun call(path: String, token: String, method: String = "GET", body: RequestBody? = null): okhttp3.Response {
        return retryIO {
            executeCall(path, token, method, body)
        }
    }

    suspend fun listBackups(): Result<List<CloudBackupSnapshot>> = withContext(Dispatchers.IO) { runCatching {
        val scope = currentScope()
        val session = token(scope.profile)
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

    private fun tables(database: AppDatabase): List<String> = database.openHelper.writableDatabase.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'sync_%' AND name NOT IN ('android_metadata','room_master_table')").use { cursor ->
        buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
    }

    private fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun requireIdleDownloads(database: AppDatabase) {
        database.openHelper.writableDatabase.query("SELECT COUNT(*) FROM downloaded_books WHERE download_status IN ('PENDING','DOWNLOADING')").use {
            it.moveToFirst(); check(it.getInt(0) == 0) { "Vui lòng hoàn tất hoặc hủy các lượt tải sách trước khi sao lưu/khôi phục." }
        }
    }

    private fun cleanupStaleRestoreDirectories(database: AppDatabase, profileFiles: File) {
        val root = profileFiles.canonicalFile
        val retained = mutableSetOf<String>()
        fun retainRoots(sql: String) {
            database.openHelper.writableDatabase.query(sql).use { cursor ->
                while (cursor.moveToNext()) {
                    val path = cursor.getString(0)?.takeIf { it.isNotBlank() } ?: continue
                    val file = runCatching { File(path).canonicalFile }.getOrNull() ?: continue
                    if (!file.path.startsWith(root.path + File.separator)) continue
                    val first = file.relativeTo(root).invariantSeparatorsPath.substringBefore('/')
                    if (first.startsWith("restored-")) retained += first
                }
            }
        }
        retainRoots("SELECT local_file_path FROM downloaded_books WHERE local_file_path != ''")
        retainRoots("SELECT custom_cover_path FROM catalog_books WHERE custom_cover_path IS NOT NULL AND custom_cover_path != ''")
        root.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("restored-") && it.name !in retained }
            ?.forEach { it.deleteRecursively() }
    }

    private fun consolidateProfileFiles(database: AppDatabase, profileFiles: File) {
        val root = profileFiles.canonicalFile
        val appFilesDir = context.filesDir.canonicalFile
        val db = database.openHelper.writableDatabase

        db.query("SELECT book_id, local_file_path FROM downloaded_books WHERE local_file_path != ''").use { cursor ->
            while (cursor.moveToNext()) {
                val bookId = cursor.getString(0)
                val path = cursor.getString(1)
                val file = runCatching { File(path).canonicalFile }.getOrNull() ?: continue
                if (!file.exists() || !file.isFile) continue
                if (file.path.startsWith(root.path + File.separator)) continue
                if (file.path.startsWith(appFilesDir.path + File.separator)) {
                    val subDir = if (file.parentFile?.name == "books") "books" else "imported"
                    val targetDir = File(root, subDir).apply { mkdirs() }
                    val targetFile = File(targetDir, file.name)
                    if (!targetFile.exists()) {
                        file.copyTo(targetFile, overwrite = true)
                    }
                    val values = ContentValues().apply { put("local_file_path", targetFile.absolutePath) }
                    db.update("downloaded_books", SQLiteDatabase.CONFLICT_REPLACE, values, "book_id=?", arrayOf(bookId))
                }
            }
        }

        db.query("SELECT id, custom_cover_path FROM catalog_books WHERE custom_cover_path IS NOT NULL AND custom_cover_path != ''").use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                val path = cursor.getString(1)
                val file = runCatching { File(path).canonicalFile }.getOrNull() ?: continue
                if (!file.exists() || !file.isFile) continue
                if (file.path.startsWith(root.path + File.separator)) continue
                if (file.path.startsWith(appFilesDir.path + File.separator)) {
                    val targetDir = File(root, "covers").apply { mkdirs() }
                    val targetFile = File(targetDir, file.name)
                    if (!targetFile.exists()) {
                        file.copyTo(targetFile, overwrite = true)
                    }
                    val values = ContentValues().apply { put("custom_cover_path", targetFile.absolutePath) }
                    db.update("catalog_books", SQLiteDatabase.CONFLICT_REPLACE, values, "id=?", arrayOf(id))
                }
            }
        }
    }

    suspend fun backup(): Result<String> = withContext(Dispatchers.IO) { runCatching {
        val scope = currentScope()
        val database = scope.database
        val profileFiles = scope.files
        val session = token(scope.profile)
        consolidateProfileFiles(database, profileFiles)
        val file = File.createTempFile("cloud-backup-", ".zip", context.cacheDir)
        try {
            val data = database.withTransaction {
                requireIdleDownloads(database)
                JSONObject().put("version", database.openHelper.writableDatabase.version).put("filesRoot", profileFiles.absolutePath).put("tables", JSONObject().apply {
                    for (table in tables(database)) {
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
            cleanupStaleRestoreDirectories(database, profileFiles)
            data.put("preferences", CloudPreferences(context).snapshot())
            val databaseJson = data.toString().toByteArray()
            check(databaseJson.size <= 64 * 1024 * 1024) { "Dữ liệu thư viện quá lớn" }
            ZipOutputStream(file.outputStream().buffered()).use { zip ->
                zip.putNextEntry(ZipEntry("database.json")); zip.write(databaseJson); zip.closeEntry()
                var total = 0L
                profileFiles.walkTopDown().onEnter { it.name !in setOf("temp", "datastore", "profiles") }.filter { it.isFile }.forEach { source ->
                    total += source.length(); check(total <= 2L * 1024 * 1024 * 1024) { "Thư viện vượt giới hạn sao lưu 2 GiB" }
                    val relative = source.relativeTo(profileFiles).invariantSeparatorsPath
                    zip.putNextEntry(ZipEntry("files/$relative")); source.inputStream().use { it.copyTo(zip) }; zip.closeEntry()
                }
            }
            check(file.length() <= 2000L * 1024 * 1024) { "Bản sao lưu nén vượt giới hạn 2000 MiB" }
            val chunks = uploadChunks(file, session)
            val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                .trim().ifBlank { "Android" }.take(100)
            val manifest = JSONObject().put("version", 1).put("chunks", chunks).put("size", file.length()).put("deviceName", deviceName)
            call("backup", session, "PUT", manifest.toString().toRequestBody("application/json".toMediaType())).close()
            "Đã sao lưu thư viện lên đám mây (${file.length() / 1024 / 1024} MiB)."
        } finally { file.delete() }
    } }

    suspend fun restore(snapshotId: String? = null): Result<String> = withContext(Dispatchers.IO) { SyncScheduler.lock.withLock { runCatching {
        val scope = currentScope()
        val profile = scope.profile
        val profileFiles = scope.files
        val database = scope.database
        val session = token(profile)
        // Best-effort remote state pull: do not block or abort restore if network is slow/failing
        runCatching {
            withTimeoutOrNull(5000L) {
                SyncEngine(context, profile).refreshRemoteStateForRestore()
            }
        }
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
            val digests = List(chunks.length()) { index -> chunks.getString(index) }
            zipFile.outputStream().use { output -> downloadChunks(digests, session, output) }
            check(zipFile.length() == manifest.getLong("size")) { "Bản sao lưu thiếu dữ liệu" }
            CloudArchive.extract(zipFile, staging)
            val dbFile = File(staging, "database.json"); check(dbFile.length() <= 64L * 1024 * 1024) { "Dữ liệu sao lưu quá lớn" }
            val data = JSONObject(dbFile.readText())
            check(data.getInt("version") == database.openHelper.writableDatabase.version) { "Cần dùng cùng phiên bản ứng dụng để khôi phục" }
            val oldRoot = data.getString("filesRoot").trimEnd('/') + "/"
            val newRoot = File(staging, "files").absolutePath + "/"
            val rows = data.getJSONObject("tables")
            database.withTransaction {
                requireIdleDownloads(database)
                val db = database.openHelper.writableDatabase
                val known = tables(database); check(rows.keys().asSequence().toSet() == known.toSet()) { "Cấu trúc dữ liệu không hợp lệ" }
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
                                    var restored: String? = try {
                                        CloudArchive.restoredPath(table, column, value, oldRoot, File(newRoot))
                                    } catch (e: Exception) {
                                        if (table == "downloaded_books" && column == "local_file_path") {
                                            if (value.isNotBlank() && File(value).exists()) value else ""
                                        } else if (table == "catalog_books" && column == "custom_cover_path") {
                                            if (value.isNotBlank() && File(value).exists()) value else null
                                        } else {
                                            throw e
                                        }
                                    }
                                    if (table == "catalog_books" && column == "id") {
                                        require(com.nocap.app.core.util.DocumentIds.isSafe(value)) { "Mã tài liệu không hợp lệ" }
                                    }
                                    if (table == "custom_fonts" && column == "file_name") {
                                        val source = File(newRoot, "custom_fonts/$value").canonicalFile
                                        check(source.path.startsWith(File(newRoot).canonicalPath + File.separator) && source.isFile) { "Thiếu tệp phông chữ" }
                                        val target = File(File(profileFiles, "custom_fonts").apply { mkdirs() }, "${UUID.randomUUID()}-${source.name}")
                                        source.copyTo(target); restoredFonts.add(target); restored = target.name
                                    }
                                    if (table == "downloaded_books" && column == "local_file_path" && !restored.isNullOrEmpty()) {
                                        val f = File(restored)
                                        if (!f.exists()) {
                                            val rootDir = File(newRoot)
                                            val direct = File(rootDir, f.name)
                                            val imported = File(rootDir, "imported/${f.name}")
                                            val books = File(rootDir, "books/${f.name}")
                                            restored = when {
                                                direct.exists() -> direct.absolutePath
                                                imported.exists() -> imported.absolutePath
                                                books.exists() -> books.absolutePath
                                                value.isNotEmpty() && File(value).exists() -> value
                                                else -> ""
                                            }
                                        }
                                    }
                                    if (table == "catalog_books" && column == "custom_cover_path" && !restored.isNullOrEmpty()) {
                                        val f = File(restored)
                                        if (!f.exists()) {
                                            val rootDir = File(newRoot)
                                            val direct = File(rootDir, f.name)
                                            val covers = File(rootDir, "covers/${f.name}")
                                            restored = when {
                                                direct.exists() -> direct.absolutePath
                                                covers.exists() -> covers.absolutePath
                                                value.isNotEmpty() && File(value).exists() -> value
                                                else -> null
                                            }
                                        }
                                    }
                                    if (restored != null) {
                                        content.put(column, restored)
                                    } else {
                                        content.putNull(column)
                                    }
                                }
                                else -> error("Giá trị sao lưu không hợp lệ")
                            }
                        }
                        check(db.insert(table, SQLiteDatabase.CONFLICT_ABORT, content) != -1L) { "Không thể khôi phục bản ghi trong $table" }
                    }
                }
                db.query("PRAGMA foreign_key_check").use { check(!it.moveToFirst()) { "Liên kết dữ liệu sao lưu bị lỗi" } }
            }
            committed = true; dbFile.delete()
            data.optJSONObject("preferences")?.let { CloudPreferences(context).restore(it) }
            cleanupStaleRestoreDirectories(database, profileFiles)
            runCatching {
                withTimeoutOrNull(15000L) {
                    SyncEngine(context, profile).run()
                }
            }
            database.invalidationTracker.refreshVersionsAsync()
            SyncScheduler.now(context, profile)
            "Đã khôi phục thư viện. Dữ liệu trên thiết bị đã được cập nhật."
        } finally { zipFile.delete(); if (!committed) { staging.deleteRecursively(); restoredFonts.forEach { it.delete() } } }
    } } }

    suspend fun deleteBackup(snapshotId: String? = null): Result<String> = withContext(Dispatchers.IO) { runCatching {
        val scope = currentScope()
        if (snapshotId != null && snapshotId != "latest") {
            call("backups/$snapshotId", token(scope.profile), "DELETE").close()
            "Đã xóa bản sao lưu."
        } else {
            call("backup", token(scope.profile), "DELETE").close()
            "Đã xóa bản sao lưu trên đám mây."
        }
    } }

    private suspend fun uploadChunks(file: File, session: String): JSONArray = coroutineScope {
        val semaphore = Semaphore(3)
        val uploads = mutableListOf<kotlinx.coroutines.Deferred<Pair<Int, String>>>()
        file.inputStream().use { input ->
            val buffer = ByteArray(20 * 1024 * 1024)
            var index = 0
            while (true) {
                var size = 0
                while (size < buffer.size) {
                    val read = input.read(buffer, size, buffer.size - size)
                    if (read < 0) break
                    size += read
                }
                if (size == 0) break
                semaphore.acquire()
                val chunkIndex = index++
                val bytes = buffer.copyOf(size)
                val digest = hash(bytes)
                uploads += async(Dispatchers.IO) {
                    try {
                        retryIO {
                            call("objects/$digest", session, "PUT", bytes.toRequestBody("application/octet-stream".toMediaType())).close()
                        }
                        chunkIndex to digest
                    } finally {
                        semaphore.release()
                    }
                }
            }
        }
        JSONArray().apply { uploads.awaitAll().sortedBy { it.first }.forEach { put(it.second) } }
    }

    private suspend fun downloadChunks(digests: List<String>, session: String, output: OutputStream) {
        for (batch in digests.chunked(3)) {
            val bytes = coroutineScope {
                batch.map { digest ->
                    async(Dispatchers.IO) {
                        check(Regex("[a-f0-9]{64}").matches(digest)) { "Mã tệp sao lưu không hợp lệ" }
                        val value = retryIO {
                            call("objects/$digest", session).use { response ->
                                val body = response.body ?: error("Tệp sao lưu trống")
                                check(body.contentLength() in 1..20L * 1024 * 1024) { "Kích thước tệp sao lưu không hợp lệ" }
                                body.bytes()
                            }
                        }
                        check(hash(value) == digest) { "Bản sao lưu bị hỏng" }
                        value
                    }
                }.awaitAll()
            }
            bytes.forEach(output::write)
        }
    }
}
