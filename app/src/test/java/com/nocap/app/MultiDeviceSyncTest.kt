package com.nocap.app

import com.nocap.app.core.database.AppDatabase
import com.nocap.app.data.sync.Profiles
import com.nocap.app.data.sync.SyncSchema
import com.nocap.app.data.cloud.BackupRestoreSyncState
import org.junit.Assert.*
import org.junit.Test
import java.sql.DriverManager
import java.sql.Connection
import androidx.sqlite.db.SupportSQLiteDatabase
import java.lang.reflect.Proxy

class MultiDeviceSyncTest {
    private fun database(): Connection = DriverManager.getConnection("jdbc:sqlite::memory:").also { db ->
        for((table,keys) in SyncSchema.keys)db.createStatement().use {
            it.execute("CREATE TABLE $table (${keys.joinToString { key -> "$key TEXT NOT NULL" }}, value TEXT, ${if(table=="bookmarks") "is_deleted INTEGER NOT NULL DEFAULT 0," else ""} PRIMARY KEY(${keys.joinToString()}))")
        }
        SyncSchema.install { sql -> db.createStatement().use { it.execute(sql) } }
    }
    private fun Connection.exec(sql: String) { createStatement().use { it.execute(sql) } }
    private fun Connection.count(table: String): Int = createStatement().use { statement -> statement.executeQuery("SELECT COUNT(*) FROM $table").use { it.next();it.getInt(1) } }

    @Test fun `migration is 5 to 6 and preserves old data as device local`() {
        assertEquals(5,AppDatabase.MIGRATION_5_6.startVersion)
        assertEquals(6,AppDatabase.MIGRATION_5_6.endVersion)
        database().use { db ->
            db.exec("INSERT INTO highlights VALUES('legacy','note')")
            SyncSchema.install { db.exec(it) }
            assertEquals(1,db.count("highlights"))
            db.createStatement().use { s -> s.executeQuery("SELECT profile FROM sync_control").use { it.next();assertEquals(Profiles.LOCAL,it.getString(1)) } }
        }
        assertEquals("ebook_reader.db",Profiles.databaseName(Profiles.LOCAL))
    }
    @Test fun `account A and B have separate databases and retain data after switching`() {
        assertNotEquals(Profiles.databaseName("ACCOUNT:A"),Profiles.databaseName("ACCOUNT:B"))
        assertNotEquals(Profiles.databaseName("ACCOUNT:A"),Profiles.databaseName(Profiles.LOCAL))
        database().use { a -> database().use { b ->
            a.exec("INSERT INTO highlights VALUES('same-id','A private note')")
            b.exec("INSERT INTO highlights VALUES('same-id','B private note')")
            a.exec("DELETE FROM highlights")
            assertEquals(1,b.count("highlights"));assertEquals(1,b.count("sync_outbox"))
        } }
    }
    @Test fun `outbox is transactional and coalesces retries without losing revision`() {
        database().use { db ->
            db.autoCommit=false
            db.exec("INSERT INTO highlights VALUES('note','text')")
            assertEquals(1,db.count("sync_outbox"));db.rollback()
            assertEquals(0,db.count("sync_outbox"));assertEquals(0,db.count("highlights"))
            db.exec("INSERT INTO highlights VALUES('note','text')");db.commit()
            db.exec("UPDATE highlights SET value='edited' WHERE id='note'");db.commit()
            assertEquals(1,db.count("sync_outbox"))
            db.createStatement().use { s -> s.executeQuery("SELECT revision FROM sync_outbox").use { it.next();assertEquals(2,it.getInt(1)) } }
            // Acknowledging revision 1 cannot remove a newer local mutation.
            db.exec("DELETE FROM sync_outbox WHERE revision=1");assertEquals(1,db.count("sync_outbox"))
        }
    }
    @Test fun `DAO replace conflict policy cannot reset an existing outbox revision`() {
        database().use { db ->
            db.exec("INSERT OR REPLACE INTO highlights VALUES('note','original')")
            db.exec("INSERT OR REPLACE INTO highlights VALUES('note','newer')")
            db.createStatement().use { s -> s.executeQuery("SELECT revision FROM sync_outbox").use { it.next();assertEquals(2,it.getInt(1)) } }
        }
    }
    @Test fun `received cursor can advance while applied cursor waits for dependency`() {
        database().use { db ->
            db.exec("INSERT INTO sync_inbox VALUES(10,'child',0)")
            db.exec("INSERT INTO sync_inbox VALUES(20,'independent',1)")
            db.exec("UPDATE sync_control SET fetch_cursor=20")
            val advance="UPDATE sync_control SET cursor=MAX(cursor,COALESCE((SELECT MAX(seq) FROM sync_inbox WHERE applied=1 AND seq<COALESCE((SELECT MIN(seq) FROM sync_inbox WHERE applied=0),9223372036854775807)),cursor)) WHERE id=1"
            db.exec(advance)
            db.createStatement().use { s -> s.executeQuery("SELECT cursor,fetch_cursor FROM sync_control").use { it.next();assertEquals(0,it.getInt(1));assertEquals(20,it.getInt(2)) } }
            db.exec("UPDATE sync_inbox SET applied=1 WHERE seq=10");db.exec(advance)
            db.createStatement().use { s -> s.executeQuery("SELECT cursor FROM sync_control").use { it.next();assertEquals(20,it.getInt(1)) } }
        }
    }
    @Test fun `remote apply suppresses echo and cursor rolls back with failed transaction`() {
        database().use { db ->
            db.autoCommit=false
            db.exec("UPDATE sync_control SET applying=1")
            db.exec("INSERT INTO highlights VALUES('remote','text')")
            db.exec("UPDATE sync_control SET cursor=123,applying=0")
            assertEquals(0,db.count("sync_outbox"));db.rollback()
            assertEquals(0,db.count("highlights"))
            db.createStatement().use { s -> s.executeQuery("SELECT cursor,applying FROM sync_control").use { it.next();assertEquals(0,it.getInt(1));assertEquals(0,it.getInt(2)) } }
        }
    }
    @Test fun `backup restore discards stale protocol state without emitting delete tombstones`() {
        database().use { db ->
            db.exec("INSERT INTO highlights VALUES('old','before restore')")
            val restoredKey="726573746F726564"
            val restoredRemoteId=java.util.UUID.nameUUIDFromBytes("nocap-sync-v1:highlights:$restoredKey".toByteArray()).toString()
            db.exec("INSERT INTO sync_remote_heads(kind,remote_id,version,deleted) VALUES('highlights','$restoredRemoteId',4,1)")
            db.exec("INSERT INTO sync_pending(op_id,kind,local_key,revision,operation) VALUES('pending','highlights','6F6C64',1,'{}')")
            db.exec("INSERT INTO sync_inbox(seq,payload) VALUES(20,'{}')")
            db.exec("INSERT INTO sync_blobs(hash,path) VALUES('${"a".repeat(64)}','old-file')")
            db.exec("UPDATE sync_control SET cursor=12,fetch_cursor=20")

            db.autoCommit=false
            BackupRestoreSyncState.prepareStatements.forEach { db.exec(it) }
            db.exec("DELETE FROM highlights")
            db.exec(BackupRestoreSyncState.observeRestoredRows)
            db.exec("INSERT INTO highlights VALUES('restored','from backup')")
            db.commit()

            assertEquals(0,db.count("sync_pending"))
            assertEquals(0,db.count("sync_inbox"))
            assertEquals(0,db.count("sync_blobs"))
            assertEquals(1,db.count("sync_outbox"))
            db.createStatement().use { statement ->
                statement.executeQuery("SELECT local_key,deleted FROM sync_outbox").use {
                    it.next();assertEquals(restoredKey,it.getString(1));assertEquals(0,it.getInt(2))
                }
                statement.executeQuery("SELECT version,deleted FROM sync_remote_heads WHERE kind='highlights' AND remote_id='$restoredRemoteId'").use {
                    assertTrue(it.next());assertEquals(4,it.getInt(1));assertEquals(1,it.getInt(2))
                }
                statement.executeQuery("SELECT cursor,fetch_cursor,applying FROM sync_control").use {
                    it.next();assertEquals(12,it.getInt(1));assertEquals(12,it.getInt(2));assertEquals(0,it.getInt(3))
                }
            }
        }
    }
    @Test fun `deletion emits tombstone and membership identities are independent`() {
        database().use { db ->
            db.exec("INSERT INTO book_tag_cross_ref VALUES('book','tag-one','')")
            db.exec("INSERT INTO book_tag_cross_ref VALUES('book','tag-two','')")
            db.exec("INSERT INTO book_collection_cross_ref VALUES('book','collection','')")
            db.exec("DELETE FROM book_tag_cross_ref WHERE tag_id='tag-one'")
            assertEquals(3,db.count("sync_outbox"))
            db.createStatement().use { s -> s.executeQuery("SELECT COUNT(*) FROM sync_outbox WHERE deleted=1").use { it.next();assertEquals(1,it.getInt(1)) } }
        }
    }
    @Test fun `bookmark soft deletion becomes a server tombstone`() {
        database().use { db ->
            db.exec("INSERT INTO bookmarks(id,value) VALUES('bookmark','text')")
            db.exec("UPDATE bookmarks SET is_deleted=1 WHERE id='bookmark'")
            db.createStatement().use { s -> s.executeQuery("SELECT deleted FROM sync_outbox WHERE kind='bookmarks'").use { it.next();assertEquals(1,it.getInt(1)) } }
        }
    }
    @Test fun `real M13 schema migration keeps annotations reviews and foreign keys`() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { db ->
            db.exec("PRAGMA foreign_keys=ON")
            val fixture=javaClass.getResourceAsStream("/room-v5.sql")!!.bufferedReader().readText()
            fixture.lineSequence().filterNot { it.trimStart().startsWith("--") }.joinToString("\n").split(';').filter { it.isNotBlank() }.forEach { db.exec(it) }
            db.exec("INSERT INTO categories VALUES('category','Category',NULL,0)")
            db.exec("INSERT INTO catalog_books(id,title,author,description,cover_url,category_id,file_url,file_size_bytes,is_featured,is_new,is_premium,entitlement_type,rating,updated_at) VALUES('book','Book','Author','','','category','https://example.test/book.epub',1,0,0,0,'FREE',0,1)")
            db.exec("INSERT INTO highlights VALUES('note','book','{}','Original text','YELLOW','Private note',1,1)")
            db.exec("INSERT INTO review_items(id,annotation_id,book_id,next_review_at,created_at,updated_at) VALUES('review','note','book',1,1,1)")
            val adapter=Proxy.newProxyInstance(SupportSQLiteDatabase::class.java.classLoader,arrayOf(SupportSQLiteDatabase::class.java)) { _,method,args ->
                if(method.name=="execSQL")db.exec(args[0] as String)
                null
            } as SupportSQLiteDatabase
            AppDatabase.MIGRATION_5_6.migrate(adapter)
            assertEquals(1,db.count("highlights"));assertEquals(1,db.count("review_items"))
            assertEquals(0,db.count("sync_outbox")) // Old data remains local, never assigned to a login.
            db.createStatement().use { s -> s.executeQuery("PRAGMA foreign_key_check").use { assertFalse(it.next()) } }
            db.exec("UPDATE highlights SET note='Updated note' WHERE id='note'")
            assertEquals(1,db.count("sync_outbox"))
            db.exec("DELETE FROM catalog_books WHERE id='book'")
            assertEquals(0,db.count("highlights"));assertEquals(0,db.count("review_items"))
            db.createStatement().use { s -> s.executeQuery("SELECT COUNT(*) FROM sync_outbox WHERE deleted=1").use { it.next();assertEquals(3,it.getInt(1)) } }
        }
    }
    @Test fun `frozen operation survives close and reopen for byte identical retry`() {
        val file=java.io.File.createTempFile("nocap-sync-test-", ".db")
        try {
            DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { db ->
                // Only durable protocol tables are needed for the process-death check.
                SyncSchema.statements().filterNot { it.startsWith("CREATE TRIGGER") }.forEach { db.exec(it) }
                db.exec("INSERT INTO sync_pending VALUES('operation-id','highlights','key',1,'{\"opId\":\"operation-id\",\"payload\":\"original\"}')")
            }
            DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { db ->
                db.createStatement().use { s -> s.executeQuery("SELECT operation FROM sync_pending").use { it.next();assertEquals("{\"opId\":\"operation-id\",\"payload\":\"original\"}",it.getString(1)) } }
            }
        } finally { file.delete() }
    }
}
