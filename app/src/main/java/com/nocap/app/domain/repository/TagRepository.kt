package com.nocap.app.domain.repository

import com.nocap.app.core.database.dao.TagWithBookCount
import com.nocap.app.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    fun observeTags(): Flow<List<Tag>>
    fun observeTagsWithCount(): Flow<List<TagWithBookCount>>
    fun observeTagsForBook(bookId: String): Flow<List<Tag>>
    suspend fun getTagsForBook(bookId: String): List<Tag>
    suspend fun createTag(name: String): Result<String>
    suspend fun renameTag(tagId: String, newName: String): Result<Unit>
    suspend fun deleteTag(tagId: String): Result<Unit>
    suspend fun addTagToBook(bookId: String, tagId: String): Result<Unit>
    suspend fun removeTagFromBook(bookId: String, tagId: String): Result<Unit>
    suspend fun setTagsForBook(bookId: String, tagIds: List<String>): Result<Unit>
    suspend fun bulkAddTag(bookIds: List<String>, tagId: String): Result<Unit>
}
