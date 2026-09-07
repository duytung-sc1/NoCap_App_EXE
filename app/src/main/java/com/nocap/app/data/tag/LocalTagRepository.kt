package com.nocap.app.data.tag

import com.nocap.app.core.database.dao.TagDao
import com.nocap.app.core.database.dao.TagWithBookCount
import com.nocap.app.core.database.entity.BookTagCrossRef
import com.nocap.app.core.database.entity.TagEntity
import com.nocap.app.core.database.toDomain
import com.nocap.app.core.database.toEntity
import com.nocap.app.domain.model.Tag
import com.nocap.app.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import java.util.UUID

class LocalTagRepository(
    private val tagDao: TagDao
) : TagRepository {

    override fun observeTags(): Flow<List<Tag>> =
        tagDao.observeTags().map { list -> list.map { it.toDomain() } }

    override fun observeTagsWithCount(): Flow<List<TagWithBookCount>> =
        tagDao.observeTagsWithCount()

    override fun observeTagsForBook(bookId: String): Flow<List<Tag>> =
        tagDao.observeTagsForBook(bookId).map { list -> list.map { it.toDomain() } }

    override suspend fun getTagsForBook(bookId: String): List<Tag> =
        tagDao.getTagsForBook(bookId).map { it.toDomain() }

    override suspend fun createTag(name: String): Result<String> = runCatching {
        val (cleanedName, normalized) = validateAndNormalize(name)
        val existing = tagDao.getTagByNormalizedName(normalized)
        if (existing != null) {
            throw IllegalArgumentException("Thẻ đã tồn tại")
        }
        val id = UUID.randomUUID().toString()
        val entity = TagEntity(
            id = id,
            name = cleanedName,
            normalizedName = normalized,
            createdAt = System.currentTimeMillis()
        )
        tagDao.insertTag(entity)
        id
    }

    override suspend fun renameTag(tagId: String, newName: String): Result<Unit> = runCatching {
        val tag = tagDao.getTagById(tagId) ?: throw IllegalArgumentException("Không tìm thấy thẻ")
        val (cleanedName, normalized) = validateAndNormalize(newName)
        val existingWithSameNormalized = tagDao.getTagByNormalizedName(normalized)
        if (existingWithSameNormalized != null && existingWithSameNormalized.id != tagId) {
            throw IllegalArgumentException("Tên thẻ đã tồn tại")
        }
        val updated = tag.copy(
            name = cleanedName,
            normalizedName = normalized
        )
        tagDao.updateTag(updated)
    }

    override suspend fun deleteTag(tagId: String): Result<Unit> = runCatching {
        tagDao.deleteTag(tagId)
    }

    override suspend fun addTagToBook(bookId: String, tagId: String): Result<Unit> = runCatching {
        tagDao.addTagToBook(BookTagCrossRef(bookId = bookId, tagId = tagId))
    }

    override suspend fun removeTagFromBook(bookId: String, tagId: String): Result<Unit> = runCatching {
        tagDao.removeTagFromBook(bookId, tagId)
    }

    override suspend fun setTagsForBook(bookId: String, tagIds: List<String>): Result<Unit> = runCatching {
        tagDao.setTagsForBook(bookId, tagIds)
    }

    override suspend fun bulkAddTag(bookIds: List<String>, tagId: String): Result<Unit> = runCatching {
        val crossRefs = bookIds.map { BookTagCrossRef(bookId = it, tagId = tagId) }
        tagDao.addTagsToBooks(crossRefs)
    }

    companion object {
        fun validateAndNormalize(rawName: String): Pair<String, String> {
            val cleaned = rawName.trim().replace("\\s+".toRegex(), " ")
            if (cleaned.isBlank()) {
                throw IllegalArgumentException("Tên thẻ không được để trống")
            }
            if (cleaned.length > 40) {
                throw IllegalArgumentException("Tên thẻ không được dài quá 40 ký tự")
            }
            val normalized = cleaned.lowercase(Locale.ROOT)
            return Pair(cleaned, normalized)
        }
    }
}
