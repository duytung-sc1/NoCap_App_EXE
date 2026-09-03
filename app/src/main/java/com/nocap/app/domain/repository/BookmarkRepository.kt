package com.nocap.app.domain.repository

import com.nocap.app.domain.model.Bookmark
import kotlinx.coroutines.flow.Flow

interface BookmarkRepository {
    fun observeBookmarks(bookId: String): Flow<List<Bookmark>>
    suspend fun addBookmark(bookmark: Bookmark)
    suspend fun removeBookmark(bookmarkId: String)
}
