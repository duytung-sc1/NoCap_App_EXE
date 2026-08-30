package com.ebookreader.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ebookreader.app.core.database.entity.DownloadedBookEntity
import com.ebookreader.app.domain.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloaded_books")
    fun observeAllDownloads(): Flow<List<DownloadedBookEntity>>

    @Query("SELECT * FROM downloaded_books WHERE download_status = 'COMPLETED'")
    fun observeCompletedDownloads(): Flow<List<DownloadedBookEntity>>

    @Query("SELECT * FROM downloaded_books WHERE book_id = :bookId")
    fun observeDownloadByBookId(bookId: String): Flow<DownloadedBookEntity?>

    @Query("SELECT * FROM downloaded_books WHERE book_id = :bookId")
    suspend fun getDownloadByBookId(bookId: String): DownloadedBookEntity?

    @Query("SELECT * FROM downloaded_books WHERE content_hash = :hash LIMIT 1")
    suspend fun getDownloadByHash(hash: String): DownloadedBookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDownload(download: DownloadedBookEntity)

    @Query("UPDATE downloaded_books SET download_status = :status, download_progress = :progress, downloaded_bytes = :downloadedBytes, total_bytes = :totalBytes WHERE book_id = :bookId")
    suspend fun updateProgress(bookId: String, status: DownloadStatus, progress: Float, downloadedBytes: Long, totalBytes: Long)

    @Query("DELETE FROM downloaded_books WHERE book_id = :bookId")
    suspend fun deleteDownload(bookId: String)
}
