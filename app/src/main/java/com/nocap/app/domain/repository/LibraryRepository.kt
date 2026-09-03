package com.nocap.app.domain.repository

import com.nocap.app.domain.model.LibraryBook
import com.nocap.app.domain.model.ReadingProgress
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun observeLibraryBooks(): Flow<List<LibraryBook>>
    fun observeRecentlyReadBooks(): Flow<List<LibraryBook>>
    fun observeBookProgress(bookId: String): Flow<ReadingProgress?>
    suspend fun saveReadingProgress(progress: ReadingProgress)
    suspend fun deleteReadingProgress(bookId: String)
}
