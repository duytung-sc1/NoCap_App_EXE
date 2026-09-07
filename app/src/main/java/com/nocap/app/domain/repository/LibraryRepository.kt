package com.nocap.app.domain.repository

import com.nocap.app.domain.model.DocumentReadingStatus
import com.nocap.app.domain.model.LibraryBook
import com.nocap.app.domain.model.ReadingProgress
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun observeLibraryBooks(): Flow<List<LibraryBook>>
    fun observeRecentlyReadBooks(): Flow<List<LibraryBook>>
    fun observeBookProgress(bookId: String): Flow<ReadingProgress?>
    suspend fun saveReadingProgress(progress: ReadingProgress)
    suspend fun deleteReadingProgress(bookId: String)

    suspend fun setInboxState(bookId: String, inInbox: Boolean)
    suspend fun setPinnedState(bookId: String, isPinned: Boolean)
    suspend fun setArchivedState(bookId: String, isArchived: Boolean)
    suspend fun setReadingStatus(bookId: String, status: DocumentReadingStatus)
    suspend fun setMetadataOverrides(bookId: String, titleOverride: String?, authorOverride: String?)
    suspend fun setCustomCover(bookId: String, coverPath: String?)
    suspend fun updateLastOpenedAt(bookId: String, timestamp: Long)

    suspend fun bulkArchive(bookIds: List<String>, isArchived: Boolean)
    suspend fun bulkPin(bookIds: List<String>, isPinned: Boolean)
    suspend fun bulkSetReadingStatus(bookIds: List<String>, status: DocumentReadingStatus)
}
