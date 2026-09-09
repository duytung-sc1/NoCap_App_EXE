package com.nocap.app.data.sync

/** Triggers participate in the caller's Room transaction, including direct DAO writes. */
object SyncSchema {
    val keys = linkedMapOf(
        "categories" to listOf("id"), "catalog_books" to listOf("id"),
        "reading_progress" to listOf("book_id"), "bookmarks" to listOf("id"),
        "highlights" to listOf("id"), "favorites" to listOf("book_id"),
        "tags" to listOf("id"), "collections" to listOf("id"),
        "book_tag_cross_ref" to listOf("book_id", "tag_id"),
        "book_collection_cross_ref" to listOf("book_id", "collection_id"),
        "review_items" to listOf("id"), "reading_sessions" to listOf("id"),
        "per_book_preferences" to listOf("book_id")
    )
    fun statements(): List<String> = buildList {
        add("CREATE TABLE IF NOT EXISTS sync_control (id INTEGER PRIMARY KEY, applying INTEGER NOT NULL DEFAULT 0, cursor INTEGER NOT NULL DEFAULT 0, fetch_cursor INTEGER NOT NULL DEFAULT 0, profile TEXT NOT NULL DEFAULT 'DEVICE_LOCAL')")
        add("INSERT OR IGNORE INTO sync_control(id) VALUES(1)")
        add("CREATE TABLE IF NOT EXISTS sync_outbox (kind TEXT NOT NULL, local_key TEXT NOT NULL, revision INTEGER NOT NULL, deleted INTEGER NOT NULL, PRIMARY KEY(kind,local_key))")
        add("CREATE TABLE IF NOT EXISTS sync_versions (kind TEXT NOT NULL, local_key TEXT NOT NULL, remote_id TEXT NOT NULL, version INTEGER NOT NULL DEFAULT 0, deleted INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(kind,local_key), UNIQUE(kind,remote_id))")
        add("CREATE TABLE IF NOT EXISTS sync_pending (op_id TEXT PRIMARY KEY, kind TEXT NOT NULL, local_key TEXT NOT NULL, revision INTEGER NOT NULL, operation TEXT NOT NULL)")
        add("CREATE TABLE IF NOT EXISTS sync_conflicts (id TEXT PRIMARY KEY, kind TEXT NOT NULL, local_key TEXT NOT NULL, payload TEXT NOT NULL, reason TEXT NOT NULL)")
        add("CREATE TABLE IF NOT EXISTS sync_blobs (hash TEXT PRIMARY KEY, path TEXT NOT NULL, uploaded INTEGER NOT NULL DEFAULT 0)")
        add("CREATE TABLE IF NOT EXISTS sync_inbox (seq INTEGER PRIMARY KEY, payload TEXT NOT NULL, applied INTEGER NOT NULL DEFAULT 0)")
        add("CREATE TABLE IF NOT EXISTS sync_remote_heads (kind TEXT NOT NULL, remote_id TEXT NOT NULL, version INTEGER NOT NULL, deleted INTEGER NOT NULL, PRIMARY KEY(kind,remote_id))")
        for ((table, columns) in keys) {
            for (event in listOf("INSERT", "UPDATE", "DELETE")) {
                val row = if(event == "DELETE") "OLD" else "NEW"
                // Hex encoding makes composite identities unambiguous for arbitrary legacy IDs.
                val key = columns.joinToString(" || ':' || ") { "hex($row.$it)" }
                val deleted = if(event == "DELETE") "1" else if(table == "bookmarks") "NEW.is_deleted" else "0"
                add("""CREATE TRIGGER IF NOT EXISTS sync_${table}_${event.lowercase()} AFTER $event ON $table
                    WHEN (SELECT applying FROM sync_control WHERE id=1)=0 BEGIN
                    INSERT INTO sync_outbox(kind,local_key,revision,deleted)
                    SELECT '$table',$key,0,0 WHERE NOT EXISTS(SELECT 1 FROM sync_outbox WHERE kind='$table' AND local_key=$key);
                    UPDATE sync_outbox SET revision=revision+1,deleted=$deleted WHERE kind='$table' AND local_key=$key;
                    END""".trimIndent())
            }
        }
    }
    fun install(exec: (String) -> Unit) = statements().forEach(exec)
}
