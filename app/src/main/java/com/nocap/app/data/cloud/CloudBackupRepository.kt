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

class CloudBackupRepository(private val context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val client = OkHttpClient.Builder().readTimeout(120,TimeUnit.SECONDS).writeTimeout(120,TimeUnit.SECONDS).build()
    private suspend fun token() = CloudAuthRepository.getInstance(context).getIdToken(false) ?: error("Vui lòng đăng nhập")
    private fun call(path: String, token: String, method: String = "GET", body: RequestBody? = null): okhttp3.Response {
        val response = client.newCall(Request.Builder().url("${BuildConfig.BACKEND_BASE_URL}/api/v1/cloud/$path")
            .header("Authorization", "Bearer $token").method(method,body).build()).execute()
        if (!response.isSuccessful) {
            val message = response.use { runCatching { JSONObject(it.body?.string().orEmpty()).getJSONObject("error").getString("message") }.getOrDefault("Không kết nối được kho lưu trữ (${it.code})") }
            error(message)
        }
        return response
    }
    private fun tables(): List<String> = database.openHelper.writableDatabase.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT IN ('android_metadata','room_master_table')").use { cursor ->
        buildList { while(cursor.moveToNext()) add(cursor.getString(0)) }
    }
    private fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private fun requireIdleDownloads() {
        database.openHelper.writableDatabase.query("SELECT COUNT(*) FROM downloaded_books WHERE download_status IN ('PENDING','DOWNLOADING')").use {
            it.moveToFirst();check(it.getInt(0)==0) { "Vui lòng hoàn tất hoặc hủy các lượt tải sách trước khi sao lưu/khôi phục." }
        }
    }
    suspend fun backup(): Result<String> = withContext(Dispatchers.IO) { runCatching {
        val session = token()
        val file = File.createTempFile("cloud-backup-", ".zip",context.cacheDir)
        try {
            val data = database.withTransaction {
                requireIdleDownloads()
                JSONObject().put("version",database.openHelper.writableDatabase.version).put("filesRoot",context.filesDir.absolutePath).put("tables",JSONObject().apply {
                    for(table in tables()) {
                        val rows=JSONArray()
                        database.openHelper.writableDatabase.query("SELECT * FROM \"$table\"").use { cursor ->
                            while(cursor.moveToNext()) {
                                val row=JSONObject()
                                cursor.columnNames.forEachIndexed { i,name -> row.put(name,when(cursor.getType(i)) {
                                    Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
                                    Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i)
                                    Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(i)
                                    Cursor.FIELD_TYPE_BLOB -> JSONObject().put("blob",Base64.encodeToString(cursor.getBlob(i),Base64.NO_WRAP))
                                    else -> cursor.getString(i)
                                }) }
                                rows.put(row)
                            }
                        }
                        put(table,rows)
                    }
                })
            }
            data.put("preferences",CloudPreferences(context).snapshot())
            check(data.toString().toByteArray().size <= 64*1024*1024) { "Dữ liệu thư viện quá lớn" }
            ZipOutputStream(file.outputStream().buffered()).use { zip ->
                zip.putNextEntry(ZipEntry("database.json"));zip.write(data.toString().toByteArray());zip.closeEntry()
                var total=0L
                context.filesDir.walkTopDown().onEnter { it.name !in setOf("temp","datastore") }.filter { it.isFile }.forEach { source ->
                    total+=source.length();check(total<=2L*1024*1024*1024) { "Thư viện vượt giới hạn sao lưu 2 GiB" }
                    val relative=source.relativeTo(context.filesDir).invariantSeparatorsPath
                    zip.putNextEntry(ZipEntry("files/$relative"));source.inputStream().use { it.copyTo(zip) };zip.closeEntry()
                }
            }
            val chunks=JSONArray()
            check(file.length()<=2000L*1024*1024) { "Bản sao lưu nén vượt giới hạn 2000 MiB" }
            file.inputStream().use { input ->
                val buffer=ByteArray(20*1024*1024)
                while(true) {
                    var size=0
                    while(size<buffer.size) { val read=input.read(buffer,size,buffer.size-size);if(read<0)break;size+=read }
                    if(size==0)break
                    val bytes=buffer.copyOf(size);val digest=hash(bytes)
                    call("objects/$digest",session,"PUT",bytes.toRequestBody("application/octet-stream".toMediaType())).close()
                    chunks.put(digest)
                }
            }
            val manifest=JSONObject().put("version",1).put("chunks",chunks).put("size",file.length())
            call("backup",session,"PUT",manifest.toString().toRequestBody("application/json".toMediaType())).close()
            "Đã sao lưu thư viện lên đám mây (${file.length()/1024/1024} MiB)."
        } finally { file.delete() }
    } }
    suspend fun restore(): Result<String> = withContext(Dispatchers.IO) { runCatching {
        val session=token()
        val manifest=call("backup",session).use { JSONObject(it.body!!.string()) }
        check(manifest.getInt("version")==1) { "Phiên bản sao lưu không được hỗ trợ" }
        val chunks=manifest.getJSONArray("chunks")
        check(chunks.length() in 1..100)
        val zipFile=File.createTempFile("cloud-restore-",".zip",context.cacheDir)
        val staging=File(context.filesDir,"restored-${UUID.randomUUID()}").apply { mkdirs() }
        var committed=false
        val restoredFonts=mutableListOf<File>()
        try {
            zipFile.outputStream().use { output ->
                for(i in 0 until chunks.length()) {
                    val digest=chunks.getString(i);check(Regex("[a-f0-9]{64}").matches(digest))
                    val bytes=call("objects/$digest",session).use { response ->
                        val body=response.body ?: error("Tệp trống");check(body.contentLength() in 1..20L*1024*1024);body.bytes()
                    }
                    check(hash(bytes)==digest) { "Bản sao lưu bị hỏng" };output.write(bytes)
                }
            }
            check(zipFile.length()==manifest.getLong("size")) { "Bản sao lưu thiếu dữ liệu" }
            CloudArchive.extract(zipFile,staging)
            val dbFile=File(staging,"database.json");check(dbFile.length()<=64L*1024*1024) { "Dữ liệu sao lưu quá lớn" }
            val data=JSONObject(dbFile.readText())
            check(data.getInt("version")==database.openHelper.writableDatabase.version) { "Cần dùng cùng phiên bản ứng dụng để khôi phục" }
            val oldRoot=data.getString("filesRoot").trimEnd('/')+"/"
            val newRoot=File(staging,"files").absolutePath+"/"
            val rows=data.getJSONObject("tables")
            database.withTransaction {
                requireIdleDownloads()
                val db=database.openHelper.writableDatabase
                val known=tables();check(rows.keys().asSequence().toSet()==known.toSet()) { "Cấu trúc dữ liệu không hợp lệ" }
                db.execSQL("PRAGMA defer_foreign_keys=ON")
                known.reversed().forEach { db.execSQL("DELETE FROM \"$it\"") }
                for(table in known) {
                    val columns=db.query("PRAGMA table_info(\"$table\")").use { cursor -> buildSet { while(cursor.moveToNext()) add(cursor.getString(1)) } }
                    val values=rows.getJSONArray(table)
                    for(i in 0 until values.length()) {
                        val row=values.getJSONObject(i);check(row.keys().asSequence().toSet()==columns)
                        val content=ContentValues()
                        for(column in columns) {
                            when(val value=row.get(column)) {
                                JSONObject.NULL -> content.putNull(column)
                                is JSONObject -> content.put(column,Base64.decode(value.getString("blob"),Base64.NO_WRAP))
                                is Int -> content.put(column,value)
                                is Long -> content.put(column,value)
                                is Number -> content.put(column,value.toDouble())
                                is String -> {
                                    var restored=value
                                    if(value.startsWith(oldRoot)) {
                                        val target=File(newRoot,value.removePrefix(oldRoot)).canonicalFile
                                        check(target.path.startsWith(File(newRoot).canonicalPath+File.separator)) { "Đường dẫn dữ liệu không hợp lệ" }
                                        restored=target.absolutePath
                                    }
                                    if(table=="custom_fonts" && column=="file_name") {
                                        val source=File(newRoot,"custom_fonts/$value").canonicalFile
                                        check(source.path.startsWith(File(newRoot).canonicalPath+File.separator) && source.isFile) { "Thiếu tệp phông chữ" }
                                        val target=File(File(context.filesDir,"custom_fonts").apply { mkdirs() },"${UUID.randomUUID()}-${source.name}")
                                        source.copyTo(target);restoredFonts.add(target);restored=target.name
                                    }
                                    content.put(column,restored)
                                }
                                else -> error("Giá trị sao lưu không hợp lệ")
                            }
                        }
                        db.insert(table,SQLiteDatabase.CONFLICT_ABORT,content)
                    }
                }
                db.query("PRAGMA foreign_key_check").use { check(!it.moveToFirst()) { "Liên kết dữ liệu sao lưu bị lỗi" } }
            }
            committed=true;dbFile.delete()
            data.optJSONObject("preferences")?.let { CloudPreferences(context).restore(it) }
            database.invalidationTracker.refreshVersionsAsync()
            "Đã khôi phục thư viện. Đóng và mở lại ứng dụng để tải lại toàn bộ dữ liệu."
        } finally { zipFile.delete();if(!committed) { staging.deleteRecursively();restoredFonts.forEach { it.delete() } } }
    } }
    suspend fun deleteBackup(): Result<String> = withContext(Dispatchers.IO) { runCatching { call("backup",token(),"DELETE").close();"Đã xóa bản sao lưu trên đám mây." } }
}
