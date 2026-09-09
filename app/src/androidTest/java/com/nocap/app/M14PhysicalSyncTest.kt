package com.nocap.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.withTransaction
import com.nocap.app.core.database.AppDatabase
import com.nocap.app.data.auth.CloudAuthRepository
import com.nocap.app.data.sync.*
import com.nocap.app.domain.model.AuthState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import org.json.JSONObject

/** Explicitly invoked QA-only physical integration test. Never clears existing app data. */
@RunWith(AndroidJUnit4::class)
class M14PhysicalSyncTest {
    @Test fun corePhase() = runBlocking(Dispatchers.IO) {
        check(BuildConfig.DEBUG && BuildConfig.BACKEND_BASE_URL.contains("nocap-ebook-api-qa."))
        val args=InstrumentationRegistry.getArguments()
        val phase=args.getString("phase") ?: error("phase required")
        val scenario=args.getString("scenario") ?: error("scenario required")
        check(Regex("[a-z0-9-]+").matches(scenario))
        val ctx=InstrumentationRegistry.getInstrumentation().targetContext
        val auth=CloudAuthRepository.getInstance(ctx)
        auth.authState.first { it != AuthState.Loading }
        SyncScheduler.lock.withLock {
            val user=if(phase.startsWith("offline-")) {
                val state=auth.authState.value
                val cached=when(state){is AuthState.Authenticated -> state.user;is AuthState.RequiresEmailVerification -> state.user;else -> error("No cached QA account")}
                assertEquals(args.getString("email"),cached.email);cached
            } else {
                var result=auth.loginWithEmail(args.getString("email")!!,args.getString("password")!!)
                repeat(4) { if(result.isFailure) { delay(2000);result=auth.loginWithEmail(args.getString("email")!!,args.getString("password")!!) } }
                result.getOrThrow()
            }
            val profile="ACCOUNT:${user.uid}"
            val room=AppDatabase.getInstance(ctx,profile)
            val db=room.openHelper.writableDatabase
            val book="$scenario-book";val note="$scenario-note"
            fun count(table: String,where: String="1=1"): Int = db.query("SELECT COUNT(*) FROM $table WHERE $where").use { it.moveToFirst();it.getInt(0) }
            fun scalar(sql: String): String = db.query(sql).use { check(it.moveToFirst());it.getString(0) }
            val content=(1..100).joinToString("\n\n") { "NoCap M14 QA paragraph $it. This is a private document with stable reading positions and real text." }
            when(phase) {
                "seed" -> {
                    val file=File(Profiles.files(ctx,profile),"imported/$book.txt").apply { parentFile!!.mkdirs();writeText(content) }
                    room.withTransaction {
                        db.execSQL("INSERT INTO categories(id,name,display_order) VALUES(?,?,0)",arrayOf(scenario,"M14 QA"))
                        db.execSQL("INSERT INTO catalog_books(id,title,author,description,cover_url,category_id,file_url,file_size_bytes,is_featured,is_new,is_premium,entitlement_type,rating,updated_at,format,media_type,source_type,is_in_inbox,reading_status) VALUES(?,?,?,'','',?,'',?,0,0,0,'FREE',0,?,'TXT','text/plain','LOCAL_FILE',1,'READING')",arrayOf<Any>(book,"M14 Private QA","NoCap QA",scenario,file.length(),System.currentTimeMillis()))
                        db.execSQL("INSERT INTO downloaded_books(book_id,local_file_path,download_status,download_progress,downloaded_bytes,total_bytes) VALUES(?,?,'COMPLETED',1,?,?)",arrayOf<Any>(book,file.absolutePath,file.length(),file.length()))
                        db.execSQL("INSERT INTO reading_progress(book_id,locator_json,progression,chapter_title,last_read_at) VALUES(?, ?,0.25,'QA',?)",arrayOf<Any>(book,com.nocap.app.domain.model.TextLocator(blockIndex=25,characterOffset=7,progression=.25f).toJson(),System.currentTimeMillis()))
                        db.execSQL("INSERT INTO bookmarks(id,book_id,locator_json,chapter_title,snippet,created_at) VALUES(?,?,?,'QA','QA bookmark',?)",arrayOf<Any>("$scenario-bookmark",book,com.nocap.app.domain.model.TextLocator(blockIndex=25,progression=.25f).toJson(),System.currentTimeMillis()))
                        db.execSQL("INSERT INTO highlights(id,book_id,locator_json,text,color,note,created_at,updated_at) VALUES(?,?,?,'QA highlighted text','YELLOW','Note from device A',?,?)",arrayOf<Any>(note,book,com.nocap.app.domain.model.TextLocator(blockIndex=25,progression=.25f).toJson(),System.currentTimeMillis(),System.currentTimeMillis()))
                        db.execSQL("INSERT INTO tags(id,name,normalized_name,created_at) VALUES(?,?,?,?)",arrayOf<Any>("$scenario-tag",scenario,scenario,System.currentTimeMillis()))
                        db.execSQL("INSERT INTO book_tag_cross_ref VALUES(?,?,?)",arrayOf<Any>(book,"$scenario-tag",System.currentTimeMillis()))
                        db.execSQL("INSERT INTO collections(id,name,created_at,updated_at) VALUES(?,?,?,?)",arrayOf<Any>("$scenario-col",scenario,System.currentTimeMillis(),System.currentTimeMillis()))
                        db.execSQL("INSERT INTO book_collection_cross_ref VALUES(?,?,?)",arrayOf<Any>(book,"$scenario-col",System.currentTimeMillis()))
                        db.execSQL("INSERT INTO review_items(id,annotation_id,book_id,next_review_at,review_count,created_at,updated_at) VALUES(?,?,?, ?,1,?,?)",arrayOf<Any>("$scenario-review",note,book,System.currentTimeMillis(),System.currentTimeMillis(),System.currentTimeMillis()))
                        db.execSQL("INSERT INTO reading_sessions(id,book_id,started_at,ended_at,duration_ms,start_progress,end_progress,format) VALUES(?,?,10000,20000,10000,0.1,0.25,'TXT')",arrayOf("$scenario-session",book))
                    }
                    SyncEngine(ctx,profile).run()
                }
                "receive", "verify-back" -> {
                    val hadFile=count("downloaded_books","book_id='$book'")>0
                    SyncEngine(ctx,profile).run()
                    assertEquals(1,count("catalog_books","id='$book'"))
                    for((table,id) in listOf("bookmarks" to "$scenario-bookmark","highlights" to note,"review_items" to "$scenario-review","reading_sessions" to "$scenario-session","tags" to "$scenario-tag","collections" to "$scenario-col"))assertEquals(table,1,count(table,"id='$id'"))
                    assertEquals(1,count("book_tag_cross_ref","book_id='$book'"));assertEquals(1,count("book_collection_cross_ref","book_id='$book'"))
                    if(phase=="receive") {
                        if(!hadFile)assertEquals(0,count("downloaded_books","book_id='$book'"))
                        assertEquals(25,JSONObject(scalar("SELECT locator_json FROM reading_progress WHERE book_id='$book'")).getInt("blockIndex"))
                    } else {
                        assertEquals("Note from device B",scalar("SELECT note FROM highlights WHERE id='$note'"))
                        assertEquals(60,JSONObject(scalar("SELECT locator_json FROM reading_progress WHERE book_id='$book'")).getInt("blockIndex"))
                    }
                    val hash=scalar("SELECT content_hash FROM catalog_books WHERE id='$book'")
                    val request=SyncEngine(ctx,profile).blobRequest(hash)
                    okhttp3.OkHttpClient().newCall(request).execute().use { response ->assertTrue(response.isSuccessful);assertEquals(content,response.body!!.string()) }
                }
                "edit-back" -> {
                    SyncEngine(ctx,profile).run()
                    room.withTransaction {
                        db.execSQL("UPDATE highlights SET note='Note from device B',updated_at=? WHERE id=?",arrayOf<Any>(System.currentTimeMillis(),note))
                        db.execSQL("UPDATE reading_progress SET locator_json=?,progression=0.6,last_read_at=? WHERE book_id=?",arrayOf<Any>(com.nocap.app.domain.model.TextLocator(blockIndex=60,characterOffset=3,progression=.6f).toJson(),System.currentTimeMillis(),book))
                    }
                    SyncEngine(ctx,profile).run()
                }
                "offline-edit", "offline-conflict" -> {
                    db.execSQL("UPDATE highlights SET note=?,updated_at=? WHERE id=?",arrayOf<Any>(if(phase=="offline-edit")"Offline preserved note" else "Concurrent offline note",System.currentTimeMillis(),note))
                    assertTrue(count("sync_outbox","kind='highlights'")>0)
                    return@withLock
                }
                "retry-offline" -> {
                    SyncEngine(ctx,profile).run()
                    assertEquals("Offline preserved note",scalar("SELECT note FROM highlights WHERE id='$note'"))
                }
                "stage-pending" -> {
                    db.execSQL("UPDATE highlights SET note='Pending process-death note',updated_at=? WHERE id=?",arrayOf<Any>(System.currentTimeMillis(),note))
                    val engine=SyncEngine(ctx,profile)
                    val method=SyncEngine::class.java.getDeclaredMethod("pending",kotlin.coroutines.Continuation::class.java).apply { isAccessible=true }
                    kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn<Any?> { continuation -> method.invoke(engine,continuation) }
                    assertTrue(count("sync_pending")>0)
                    InstrumentationRegistry.getInstrumentation().sendStatus(2,android.os.Bundle().apply { putString("stream","M14_PENDING_DURABLE") })
                    delay(120_000) // Host force-stops this QA process once the durable marker appears.
                    error("Host did not interrupt pending sync")
                }
                "retry-pending" -> {
                    SyncEngine(ctx,profile).run()
                    assertEquals("Pending process-death note",scalar("SELECT note FROM highlights WHERE id='$note'"))
                }
                "conflict-winner" -> {
                    db.execSQL("UPDATE highlights SET note='Online winning note',updated_at=? WHERE id=?",arrayOf<Any>(System.currentTimeMillis(),note))
                    SyncEngine(ctx,profile).run()
                }
                "verify-conflict" -> {
                    SyncEngine(ctx,profile).run()
                    assertTrue(count("sync_conflicts","payload LIKE '%Concurrent offline note%'")>0)
                    assertEquals("Online winning note",scalar("SELECT note FROM highlights WHERE id='$note'"))
                }
                "complete" -> {
                    db.execSQL("UPDATE reading_progress SET progression=1,locator_json=?,last_read_at=? WHERE book_id=?",arrayOf<Any>(com.nocap.app.domain.model.TextLocator(blockIndex=99,progression=1f).toJson(),System.currentTimeMillis(),book))
                    db.execSQL("UPDATE catalog_books SET reading_status='COMPLETED' WHERE id=?",arrayOf(book))
                    SyncEngine(ctx,profile).run()
                }
                "regress-completed" -> {
                    SyncEngine(ctx,profile).run()
                    db.execSQL("UPDATE reading_progress SET progression=0.1,last_read_at=? WHERE book_id=?",arrayOf<Any>(System.currentTimeMillis(),book))
                    SyncEngine(ctx,profile).run()
                    assertEquals(1.0,scalar("SELECT progression FROM reading_progress WHERE book_id='$book'").toDouble(),0.00001)
                }
                "dedupe" -> {
                    repeat(3){SyncEngine(ctx,profile).run()}
                    assertEquals(1,count("reading_sessions","id='$scenario-session'"))
                    assertEquals("1",scalar("SELECT review_count FROM review_items WHERE id='$scenario-review'"))
                }
                "delete" -> { db.execSQL("DELETE FROM catalog_books WHERE id=?",arrayOf(book));SyncEngine(ctx,profile).run() }
                "verify-deleted" -> {
                    SyncEngine(ctx,profile).run()
                    assertEquals(0,count("catalog_books","id='$book'"));assertEquals(0,count("highlights","id='$note'"))
                }
                "isolation" -> {
                    assertEquals(0,count("catalog_books","id='$book'"))
                    assertEquals(0,count("highlights","id='$note'"))
                    auth.signOut()
                    assertEquals(Profiles.LOCAL,Profiles.active.value)
                    assertNotEquals(room,AppDatabase.getInstance(ctx))
                    auth.loginWithEmail(args.getString("email")!!,args.getString("password")!!).getOrThrow()
                    assertSame(room,AppDatabase.getInstance(ctx))
                }
                else -> error("Unknown phase")
            }
            SyncEngine(ctx,profile).run() // replay/retry must remain idempotent
            assertEquals("No pending frozen request",0,count("sync_pending"))
            assertEquals("No unresolved feed rows",0,count("sync_inbox","applied=0"))
        }
    }
}
