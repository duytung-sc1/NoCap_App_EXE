package com.nocap.app.domain.repository

import com.nocap.app.domain.model.DownloadedBook
import kotlinx.coroutines.flow.Flow

interface BookDownloadRepository {
    fun observeDownload(bookId: String): Flow<DownloadedBook?>
    fun observeAllDownloads(): Flow<List<DownloadedBook>>
    fun observeCompletedDownloads(): Flow<List<DownloadedBook>>
    suspend fun startDownload(bookId: String)
    suspend fun cancelDownload(bookId: String)
    suspend fun retryDownload(bookId: String)
    suspend fun deleteDownloadedBook(bookId: String)
    suspend fun isBookDownloaded(bookId: String): Boolean
}
