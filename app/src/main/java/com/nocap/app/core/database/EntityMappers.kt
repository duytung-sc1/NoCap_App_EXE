package com.nocap.app.core.database

import com.nocap.app.core.database.entity.CatalogBookEntity
import com.nocap.app.core.database.entity.CategoryEntity
import com.nocap.app.core.database.entity.DownloadedBookEntity
import com.nocap.app.core.database.entity.ReadingProgressEntity
import com.nocap.app.domain.model.CatalogBook
import com.nocap.app.domain.model.Category
import com.nocap.app.domain.model.DownloadedBook
import com.nocap.app.domain.model.ReadingProgress

fun CatalogBook.toEntity(): CatalogBookEntity = CatalogBookEntity(
    id = id,
    title = title,
    author = author,
    description = description,
    coverUrl = coverUrl,
    categoryId = categoryId,
    fileUrl = fileUrl,
    fileSizeBytes = fileSizeBytes,
    contentVersion = contentVersion,
    contentHash = contentHash,
    isFeatured = isFeatured,
    isNew = isNew,
    isPremium = isPremium,
    playProductId = playProductId,
    entitlementType = entitlementType,
    rating = rating,
    publishedDate = publishedDate,
    updatedAt = updatedAt
)

fun CatalogBookEntity.toDomain(): CatalogBook = CatalogBook(
    id = id,
    title = title,
    author = author,
    description = description,
    coverUrl = coverUrl,
    categoryId = categoryId,
    fileUrl = fileUrl,
    fileSizeBytes = fileSizeBytes,
    contentVersion = contentVersion,
    contentHash = contentHash,
    isFeatured = isFeatured,
    isNew = isNew,
    isPremium = isPremium,
    playProductId = playProductId,
    entitlementType = entitlementType,
    rating = rating,
    publishedDate = publishedDate,
    updatedAt = updatedAt
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    iconUrl = iconUrl,
    displayOrder = displayOrder
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    iconUrl = iconUrl,
    displayOrder = displayOrder
)

fun DownloadedBookEntity.toDomain(): DownloadedBook = DownloadedBook(
    bookId = bookId,
    localFilePath = localFilePath,
    downloadStatus = downloadStatus,
    downloadProgress = downloadProgress,
    downloadedBytes = downloadedBytes,
    totalBytes = totalBytes,
    downloadedContentVersion = downloadedContentVersion,
    contentHash = contentHash,
    downloadedAt = downloadedAt,
    lastError = lastError
)

fun DownloadedBook.toEntity(): DownloadedBookEntity = DownloadedBookEntity(
    bookId = bookId,
    localFilePath = localFilePath,
    downloadStatus = downloadStatus,
    downloadProgress = downloadProgress,
    downloadedBytes = downloadedBytes,
    totalBytes = totalBytes,
    downloadedContentVersion = downloadedContentVersion,
    contentHash = contentHash,
    downloadedAt = downloadedAt,
    lastError = lastError
)

fun ReadingProgressEntity.toDomain(): ReadingProgress = ReadingProgress(
    bookId = bookId,
    locatorJson = locatorJson,
    progression = progression,
    chapterTitle = chapterTitle,
    lastReadAt = lastReadAt,
    syncVersion = syncVersion
)

fun ReadingProgress.toEntity(): ReadingProgressEntity = ReadingProgressEntity(
    bookId = bookId,
    locatorJson = locatorJson,
    progression = progression,
    chapterTitle = chapterTitle,
    lastReadAt = lastReadAt,
    syncVersion = syncVersion
)

fun com.nocap.app.core.database.entity.BookmarkEntity.toDomain(): com.nocap.app.domain.model.Bookmark = com.nocap.app.domain.model.Bookmark(
    id = id,
    bookId = bookId,
    locatorJson = locatorJson,
    chapterTitle = chapterTitle,
    snippet = snippet,
    createdAt = createdAt,
    syncVersion = syncVersion,
    isDeleted = isDeleted
)

fun com.nocap.app.domain.model.Bookmark.toEntity(): com.nocap.app.core.database.entity.BookmarkEntity = com.nocap.app.core.database.entity.BookmarkEntity(
    id = id,
    bookId = bookId,
    locatorJson = locatorJson,
    chapterTitle = chapterTitle,
    snippet = snippet,
    createdAt = createdAt,
    syncVersion = syncVersion,
    isDeleted = isDeleted
)
