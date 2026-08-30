package com.ebookreader.app.core.database

import com.ebookreader.app.core.database.entity.CatalogBookEntity
import com.ebookreader.app.core.database.entity.CategoryEntity
import com.ebookreader.app.core.database.entity.DownloadedBookEntity
import com.ebookreader.app.core.database.entity.ReadingProgressEntity
import com.ebookreader.app.domain.model.CatalogBook
import com.ebookreader.app.domain.model.Category
import com.ebookreader.app.domain.model.DownloadedBook
import com.ebookreader.app.domain.model.ReadingProgress

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
