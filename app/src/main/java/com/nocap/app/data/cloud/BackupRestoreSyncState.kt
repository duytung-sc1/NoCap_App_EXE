package com.nocap.app.data.cloud

import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Reconciles the local sync protocol around a full-library restore.
 *
 * A restore replaces the current local snapshot. Pending operations created for
 * the snapshot being replaced must not be sent after the restored rows are in
 * place. Deletes are suppressed while clearing the old snapshot; inserts are
 * deliberately observed so the restored snapshot can be reconciled with the
 * account's server state.
 */
internal object BackupRestoreSyncState {
    val prepareStatements = listOf(
        "UPDATE sync_control SET applying=1 WHERE id=1",
        "DELETE FROM sync_pending",
        "DELETE FROM sync_outbox",
        "DELETE FROM sync_inbox",
        "DELETE FROM sync_blobs",
        "UPDATE sync_control SET fetch_cursor=cursor WHERE id=1"
    )

    const val observeRestoredRows = "UPDATE sync_control SET applying=0 WHERE id=1"

    fun prepare(database: SupportSQLiteDatabase) {
        prepareStatements.forEach(database::execSQL)
    }

    fun observeRestoredRows(database: SupportSQLiteDatabase) {
        database.execSQL(observeRestoredRows)
    }
}
