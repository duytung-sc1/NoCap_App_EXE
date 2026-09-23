package com.nocap.app.data.sync

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.room.withTransaction
import com.nocap.app.BuildConfig
import com.nocap.app.core.database.AppDatabase
import com.nocap.app.data.auth.CloudAuthRepository
import com.nocap.app.domain.model.AuthState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

class SyncEngine(private val context: Context, private val profile: String) {
    private val room = AppDatabase.getInstance(context, profile)
    private val db get() = room.openHelper.writableDatabase
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .connectionPool(okhttp3.ConnectionPool(5, 5, TimeUnit.MINUTES))
        .build()
    private lateinit var token: String

    private suspend fun authenticate() {
        check(profile.startsWith("ACCOUNT:")) { "Vui lòng đăng nhập để đồng bộ" }
        val auth = CloudAuthRepository.getInstance(context)
        val state = auth.authState.first { it != AuthState.Loading }
        val user = when(state) { is AuthState.Authenticated -> state.user; is AuthState.RequiresEmailVerification -> state.user; else -> null }
        check(user != null && profile == "ACCOUNT:${user.uid}" && Profiles.active.value == profile) { "Tài khoản đã thay đổi" }
        token = auth.getIdToken(false) ?: error("Phiên đăng nhập hết hạn")
        // Verify the captured token against the server before sending any private data.
        val me = callAbsolute("${BuildConfig.BACKEND_BASE_URL}/api/v1/me").use { JSONObject(it.body!!.string()) }
        check("ACCOUNT:${me.getString("id")}" == profile) { "Phiên đăng nhập không khớp profile" }
    }

    private fun callAbsolute(url: String, method: String = "GET", body: RequestBody? = null): okhttp3.Response {
        val response = client.newCall(Request.Builder().url(url).header("Authorization", "Bearer $token").method(method,body).build()).execute()
        if (!response.isSuccessful) { val code=response.code; val pro=code==403 && response.body?.string()?.contains("PRO_REQUIRED")==true;response.close();if(pro)throw com.nocap.app.data.billing.ProRequired();error("Đồng bộ thất bại (HTTP $code)") }
        return response
    }
    private fun call(path: String, method: String = "GET", body: RequestBody? = null) = callAbsolute("${BuildConfig.BACKEND_BASE_URL}/api/v1/sync/$path",method,body)
    suspend fun blobRequest(hash: String): Request {
        check(Regex("[a-f0-9]{64}").matches(hash))
        authenticate()
        com.nocap.app.data.billing.EntitlementRepository.get(context).require(com.nocap.app.data.billing.Feature.PRIVATE_CLOUD,profile)
        return Request.Builder().url("${BuildConfig.BACKEND_BASE_URL}/api/v1/sync/blobs/$hash").header("Authorization","Bearer $token").build()
    }
    private fun jsonBody(data: JSONObject) = data.toString().toRequestBody("application/json".toMediaType())
    private fun keyExpression(kind: String) = SyncSchema.keys.getValue(kind).joinToString(" || ':' || ") { "hex($it)" }
    private fun row(kind: String, key: String): JSONObject? = db.query("SELECT * FROM $kind WHERE ${keyExpression(kind)}=?",arrayOf<Any>(key)).use { if(it.moveToFirst()) readRow(it) else null }
    private fun readRow(cursor: Cursor): JSONObject = JSONObject().apply {
        cursor.columnNames.forEachIndexed { i, name -> put(name,when(cursor.getType(i)) {
            Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
            Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i)
            Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(i)
            else -> cursor.getString(i)
        }) }
    }
    private data class Identity(val id: String,val version: Long,val deleted: Boolean)
    private fun identity(kind: String, key: String): Identity {
        db.query("SELECT remote_id,version,deleted FROM sync_versions WHERE kind=? AND local_key=?",arrayOf<Any>(kind,key)).use {
            if(it.moveToFirst())return Identity(it.getString(0),it.getLong(1),it.getInt(2)==1)
        }
        val id=UUID.nameUUIDFromBytes("nocap-sync-v1:$kind:$key".toByteArray()).toString()
        val remote=db.query("SELECT version,deleted FROM sync_remote_heads WHERE kind=? AND remote_id=?",arrayOf<Any>(kind,id)).use {
            if(it.moveToFirst())it.getLong(0) to (it.getInt(1)==1) else null
        }
        db.execSQL(
            "INSERT INTO sync_versions(kind,local_key,remote_id,version,deleted) VALUES(?,?,?,?,?)",
            arrayOf<Any>(kind,key,id,remote?.first ?: 0L,if(remote?.second==true)1 else 0)
        )
        return Identity(id,remote?.first ?: 0L,remote?.second==true)
    }
    private fun hash(file: File): String {
        val digest=MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input -> val bytes=ByteArray(65536);while(true){val n=input.read(bytes);if(n<0)break;digest.update(bytes,0,n)} }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    private fun prepareDocument(payload: JSONObject) {
        val id=payload.getString("id")
        // Catalog references remain recoverable from the public catalog, not private R2.
        val publicBook=(payload.optString("file_url").startsWith("https://") && payload.optString("category_id")!="imported") ||
            com.nocap.app.data.catalog.CloudCatalog.books.value.any { it.id==id }
        if(!publicBook) {
            val path=db.query("SELECT local_file_path FROM downloaded_books WHERE book_id=? AND download_status='COMPLETED'",arrayOf<Any>(id)).use { if(it.moveToFirst()) it.getString(0) else null }
            if(path!=null) {
                val file=File(path)
                check(file.isFile && file.length() in 1..250L*1024*1024) { "Tệp chưa sẵn sàng để đồng bộ" }
                val digest=hash(file)
                db.execSQL("INSERT OR IGNORE INTO sync_blobs(hash,path) VALUES(?,?)",arrayOf<Any>(digest,path))
                payload.put("file_url","nocap-private:$digest").put("content_hash",digest).put("file_size_bytes",file.length())
            } else check(payload.optString("file_url").startsWith("nocap-private:")) { "Thiếu tệp tài liệu riêng" }
        }
        // Never send device-local paths to another device.
        if(!payload.optString("cover_url").startsWith("https://"))payload.put("cover_url","")
        payload.put("custom_cover_path",JSONObject.NULL)
    }
    private suspend fun pending(): JSONArray = room.withTransaction {
        val existing=db.query("SELECT operation FROM sync_pending ORDER BY rowid LIMIT 50").use { c -> JSONArray().apply { while(c.moveToNext())put(JSONObject(c.getString(0))) } }
        if(existing.length()>0)return@withTransaction existing
        val changes=db.query("SELECT kind,local_key,revision,deleted FROM sync_outbox ORDER BY CASE WHEN kind='reading_sessions' THEN 1 ELSE 0 END,rowid LIMIT 200").use { c -> buildList { while(c.moveToNext())add(readRow(c)) } }
        val result=JSONArray()
        var batchBytes=0
        for(change in changes) {
            if(result.length()>=50)break
            val kind=change.getString("kind");val key=change.getString("local_key")
            val deleted=change.getInt("deleted")==1
            val payload=if(deleted)JSONObject() else row(kind,key) ?: continue
            if(kind=="reading_sessions"&&!deleted&&payload.isNull("ended_at"))continue
            if(kind=="catalog_books"&&!deleted)prepareDocument(payload)
            val identity=identity(kind,key)
            val op=JSONObject().put("opId",UUID.randomUUID().toString()).put("kind",kind).put("id",identity.id).put("baseVersion",identity.version).put("deleted",deleted).put("payload",payload)
            // A local row that exists after this account has observed its server
            // tombstone is an explicit recreation (for example, cloud restore).
            // Stale devices that have not observed the tombstone still conflict.
            if(!deleted && identity.deleted)op.put("recreate",true)
            check(op.toString().length<=32768) { "Bản ghi quá lớn; dữ liệu vẫn được giữ trên máy" }
            val bytes=op.toString().toByteArray(Charsets.UTF_8).size
            if(batchBytes+bytes>450*1024)break
            batchBytes+=bytes
            db.execSQL("INSERT INTO sync_pending(op_id,kind,local_key,revision,operation) VALUES(?,?,?,?,?)",arrayOf<Any>(op.getString("opId"),kind,key,change.getLong("revision"),op.toString()))
            result.put(op)
        }
        result
    }
    private suspend fun uploadBlobs() {
        val files=db.query("SELECT hash,path FROM sync_blobs WHERE uploaded=0").use { c -> buildList { while(c.moveToNext())add(c.getString(0) to c.getString(1)) } }
        for((digest,path) in files) {
            call("blobs/$digest","PUT",File(path).asRequestBody("application/octet-stream".toMediaType())).close()
            db.execSQL("UPDATE sync_blobs SET uploaded=1 WHERE hash=?",arrayOf<Any>(digest))
        }
    }
    private suspend fun acknowledge(receipts: JSONArray) = room.withTransaction {
        for(i in 0 until receipts.length()) {
            val receipt=receipts.getJSONObject(i);val opId=receipt.getString("opId")
            val pending=db.query("SELECT * FROM sync_pending WHERE op_id=?",arrayOf<Any>(opId)).use { if(it.moveToFirst())readRow(it) else null } ?: continue
            val kind=pending.getString("kind");val key=pending.getString("local_key")
            if(receipt.getString("status")=="CONFLICT") {
                db.execSQL("INSERT OR IGNORE INTO sync_conflicts(id,kind,local_key,payload,reason) VALUES(?,?,?,?,?)",arrayOf<Any>(opId,kind,key,pending.getString("operation"),"SERVER_CONFLICT"))
            }
            receipt.optJSONObject("current")?.let { current ->
                db.execSQL("UPDATE sync_versions SET version=?,deleted=? WHERE kind=? AND local_key=?",arrayOf<Any>(current.getLong("version"),current.getInt("deleted"),kind,key))
            }
            db.execSQL("DELETE FROM sync_outbox WHERE kind=? AND local_key=? AND revision=?",arrayOf<Any>(kind,key,pending.getLong("revision")))
            db.execSQL("DELETE FROM sync_pending WHERE op_id=?",arrayOf<Any>(opId))
            if(receipt.getString("status")=="CONFLICT") {
                receipt.optJSONObject("current")?.let { current ->
                    db.execSQL("UPDATE sync_control SET applying=1 WHERE id=1")
                    try { applyChange(current, force = true) }
                    catch(e: android.database.sqlite.SQLiteConstraintException) {
                        conflict(kind,key,current,"REMOTE_DEPENDENCY_CONFLICT")
                    }
                    db.execSQL("UPDATE sync_control SET applying=0 WHERE id=1")
                }
            }
        }
    }
    private fun localKey(kind: String,payload: JSONObject): String = SyncSchema.keys.getValue(kind).joinToString(":") { column ->
        payload.getString(column).toByteArray().joinToString("") { "%02X".format(it) }
    }
    private fun conflict(kind: String,key: String,data: JSONObject,reason: String) {
        db.execSQL("INSERT INTO sync_conflicts(id,kind,local_key,payload,reason) VALUES(?,?,?,?,?)",arrayOf<Any>(UUID.randomUUID().toString(),kind,key,data.toString(),reason))
    }
    private fun applyChange(change: JSONObject, force: Boolean = false) {
        val kind=change.getString("kind");check(kind in SyncSchema.keys)
        val remoteId=change.getString("id");val payload=change.getJSONObject("payload");val deleted=change.getInt("deleted")==1
        val remoteHead=db.query("SELECT version,deleted FROM sync_remote_heads WHERE kind=? AND remote_id=?",arrayOf<Any>(kind,remoteId)).use { if(it.moveToFirst())it.getLong(0) to it.getInt(1) else null }
        if(remoteHead!=null && (remoteHead.first>change.getLong("version") || (!force && remoteHead.first==change.getLong("version"))))return
        val known=db.query("SELECT local_key,version FROM sync_versions WHERE kind=? AND remote_id=?",arrayOf<Any>(kind,remoteId)).use { if(it.moveToFirst())it.getString(0) to it.getLong(1) else null }
        if(known==null && deleted) {
            rememberRemoteHead(change)
            return
        }
        val key=known?.first ?: localKey(kind,payload)
        check(UUID.nameUUIDFromBytes("nocap-sync-v1:$kind:$key".toByteArray()).toString()==remoteId)
        if(known!=null&&known.second>change.getLong("version"))return
        val dirty=db.query("SELECT 1 FROM sync_outbox WHERE kind=? AND local_key=?",arrayOf<Any>(kind,key)).use { it.moveToFirst() }
        if(dirty) {
            row(kind,key)?.let { conflict(kind,key,it,"LOCAL_PENDING_BEFORE_PULL") }
            // Preserve the unpushed local edit in a conflict record; remote tombstones still win.
            db.execSQL("DELETE FROM sync_outbox WHERE kind=? AND local_key=?",arrayOf<Any>(kind,key))
        }
        if(deleted) {
            // A parent tombstone can cascade into an offline child's unsent note/review.
            // Preserve all affected pending rows before the foreign keys remove them.
            val pendingChildren=db.query("SELECT kind,local_key FROM sync_outbox").use { c -> buildList { while(c.moveToNext())add(c.getString(0) to c.getString(1)) } }
            for((childKind,childKey) in pendingChildren) {
                val child=row(childKind,childKey) ?: continue
                val parent=row(kind,key)
                val affected=when(kind) {
                    "catalog_books" -> child.optString("book_id")==parent?.optString("id")
                    "highlights" -> childKind=="review_items" && child.optString("annotation_id")==parent?.optString("id")
                    "tags" -> childKind=="book_tag_cross_ref" && child.optString("tag_id")==parent?.optString("id")
                    "collections" -> childKind=="book_collection_cross_ref" && child.optString("collection_id")==parent?.optString("id")
                    else -> false
                }
                if(affected) {
                    conflict(childKind,childKey,child,"PARENT_DELETED_WITH_LOCAL_CHANGES")
                    db.execSQL("DELETE FROM sync_outbox WHERE kind=? AND local_key=?",arrayOf<Any>(childKind,childKey))
                }
            }
            db.execSQL("DELETE FROM $kind WHERE ${keyExpression(kind)}=?",arrayOf<Any>(key))
        }
        else {
            // A finalized session can arrive after its book was deleted on this device.
            // Retain the orphan payload without blocking the applied feed cursor forever.
            if (payload.has("book_id") && !payload.isNull("book_id")) {
                val parentKey=localKey("catalog_books",JSONObject().put("id",payload.getString("book_id")))
                val parentId=UUID.nameUUIDFromBytes("nocap-sync-v1:catalog_books:$parentKey".toByteArray()).toString()
                val parentDeleted=db.query("SELECT deleted FROM sync_remote_heads WHERE kind='catalog_books' AND remote_id=?",arrayOf<Any>(parentId)).use { it.moveToFirst() && it.getInt(0)==1 }
                if(parentDeleted) {
                    conflict(kind,key,payload,"REMOTE_CHILD_AFTER_PARENT_DELETED")
                    rememberRemoteHead(change)
                    return
                }
            }
            if(kind=="catalog_books") {
                check(payload.isNull("custom_cover_path"))
                check(payload.optString("cover_url").isEmpty() || payload.optString("cover_url").startsWith("https://"))
                check(Regex("[A-Za-z0-9._-]{1,255}").matches(payload.getString("id")) && !payload.getString("id").contains(".."))
                // Local cover customization is device-specific and never replaced by a remote path.
                row(kind,key)?.opt("custom_cover_path")?.let { payload.put("custom_cover_path",it) }
            }
            val columns=db.query("PRAGMA table_info($kind)").use { c -> buildSet { while(c.moveToNext())add(c.getString(1)) } }
            check(payload.keys().asSequence().toSet()==columns) { "Cấu trúc bản ghi đồng bộ không tương thích" }
            val values=ContentValues()
            for(column in columns)when(val value=payload.get(column)) {
                JSONObject.NULL -> values.putNull(column)
                is Number -> if(value is Double || value is Float)values.put(column,value.toDouble()) else values.put(column,value.toLong())
                is String -> values.put(column,value)
                else -> error("Giá trị đồng bộ không hợp lệ")
            }
            // UPDATE preserves child rows; REPLACE would cascade-delete annotations.
            if(db.update(kind,SQLiteDatabase.CONFLICT_ABORT,values,"${keyExpression(kind)}=?",arrayOf<Any>(key))==0)db.insert(kind,SQLiteDatabase.CONFLICT_ABORT,values)
        }
        db.execSQL("INSERT OR REPLACE INTO sync_versions(kind,local_key,remote_id,version,deleted) VALUES(?,?,?,?,?)",arrayOf<Any>(kind,key,remoteId,change.getLong("version"),if(deleted)1 else 0))
        rememberRemoteHead(change)
    }
    private fun rememberRemoteHead(change: JSONObject) {
        db.execSQL("INSERT OR REPLACE INTO sync_remote_heads(kind,remote_id,version,deleted) VALUES(?,?,?,?)",arrayOf<Any>(change.getString("kind"),change.getString("id"),change.getLong("version"),change.getInt("deleted")))
    }
    private suspend fun pull() {
        do {
            val cursor=db.query("SELECT fetch_cursor FROM sync_control WHERE id=1").use { it.moveToFirst();it.getLong(0) }
            val page=call("changes?cursor=$cursor&limit=100").use { JSONObject(it.body!!.string()) }
            room.withTransaction {
                db.execSQL("UPDATE sync_control SET applying=1 WHERE id=1")
                val changes=page.getJSONArray("changes")
                // Feed order preserves write order. Store temporarily unresolvable dependencies durably.
                for(i in 0 until changes.length()) {
                    val change=changes.getJSONObject(i)
                    db.execSQL("INSERT OR IGNORE INTO sync_inbox(seq,payload) VALUES(?,?)",arrayOf<Any>(change.getLong("seq"),change.toString()))
                }
                var moved: Boolean
                do {
                    moved=false
                    val queued=db.query("SELECT seq,payload FROM sync_inbox WHERE applied=0 ORDER BY seq").use { c -> buildList { while(c.moveToNext())add(c.getLong(0) to c.getString(1)) } }
                    for((seq,text) in queued) {
                        db.execSQL("SAVEPOINT sync_apply")
                        try {
                            applyChange(JSONObject(text));db.execSQL("UPDATE sync_inbox SET applied=1 WHERE seq=?",arrayOf<Any>(seq));db.execSQL("RELEASE sync_apply");moved=true
                        } catch(e: android.database.sqlite.SQLiteConstraintException) {
                            db.execSQL("ROLLBACK TO sync_apply");db.execSQL("RELEASE sync_apply")
                        }
                    }
                } while(moved)
                // fetch_cursor only acknowledges durable receipt; cursor acknowledges successful apply.
                db.execSQL("UPDATE sync_control SET fetch_cursor=?,applying=0 WHERE id=1",arrayOf<Any>(page.getLong("cursor")))
                db.execSQL("UPDATE sync_control SET cursor=MAX(cursor,COALESCE((SELECT MAX(seq) FROM sync_inbox WHERE applied=1 AND seq<COALESCE((SELECT MIN(seq) FROM sync_inbox WHERE applied=0),9223372036854775807)),cursor)) WHERE id=1")
                db.execSQL("DELETE FROM sync_inbox WHERE applied=1 AND seq<=(SELECT cursor FROM sync_control WHERE id=1)")
            }
        } while(page.getBoolean("hasMore"))
        room.invalidationTracker.refreshVersionsAsync()
    }
    suspend fun run() = withContext(Dispatchers.IO) {
        authenticate()
        val grant=com.nocap.app.data.billing.EntitlementRepository.get(context).require(com.nocap.app.data.billing.Feature.MULTI_DEVICE_SYNC,profile)
        com.nocap.app.data.billing.EntitlementPolicy.withAccess(com.nocap.app.data.billing.Feature.MULTI_DEVICE_SYNC,grant,profile.removePrefix("ACCOUNT:")) {
        var batches=0
        while(true) {
            val operations=pending();if(operations.length()==0)break
            val result=call("push","POST",jsonBody(JSONObject().put("operations",operations))).use { JSONObject(it.body!!.string()) }
            // Upload is retryable even if the push response was lost. Keep pending receipts until blobs finish.
            uploadBlobs();acknowledge(result.getJSONArray("receipts"))
            if(++batches>=100)break
        }
        uploadBlobs();pull()
        }
    }

    /** Refreshes remote heads before a full restore without uploading local rows
     * that are about to be replaced by the selected backup. */
    suspend fun refreshRemoteStateForRestore() = withContext(Dispatchers.IO) {
        authenticate()
        // Multi-device sync is part of the current Free policy. The sync API still
        // authenticates and authorizes every account, so an entitlement refresh here
        // only adds two network round trips and can block an otherwise valid restore.
        pull()
    }
}
