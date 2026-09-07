package com.nocap.app.core.database

import androidx.room.TypeConverter
import com.nocap.app.domain.model.DocumentReadingStatus
import com.nocap.app.domain.model.DownloadStatus
import com.nocap.app.domain.model.EntitlementType
import com.nocap.app.domain.model.PublicationFormat
import com.nocap.app.domain.model.PublicationSourceType

class Converters {
    @TypeConverter
    fun fromEntitlementType(value: EntitlementType?): String = value?.name ?: EntitlementType.FREE.name

    @TypeConverter
    fun toEntitlementType(value: String?): EntitlementType =
        value?.let { runCatching { EntitlementType.valueOf(it) }.getOrDefault(EntitlementType.FREE) } ?: EntitlementType.FREE

    @TypeConverter
    fun fromDownloadStatus(value: DownloadStatus?): String = value?.name ?: DownloadStatus.PENDING.name

    @TypeConverter
    fun toDownloadStatus(value: String?): DownloadStatus =
        value?.let { runCatching { DownloadStatus.valueOf(it) }.getOrDefault(DownloadStatus.PENDING) } ?: DownloadStatus.PENDING

    @TypeConverter
    fun fromPublicationFormat(value: PublicationFormat?): String =
        value?.name ?: PublicationFormat.EPUB.name

    @TypeConverter
    fun toPublicationFormat(value: String?): PublicationFormat =
        value?.let { runCatching { PublicationFormat.valueOf(it) }.getOrDefault(PublicationFormat.EPUB) }
            ?: PublicationFormat.EPUB

    @TypeConverter
    fun fromPublicationSourceType(value: PublicationSourceType?): String =
        value?.name ?: PublicationSourceType.LOCAL_FILE.name

    @TypeConverter
    fun toPublicationSourceType(value: String?): PublicationSourceType =
        value?.let { runCatching { PublicationSourceType.valueOf(it) }.getOrDefault(PublicationSourceType.LOCAL_FILE) }
            ?: PublicationSourceType.LOCAL_FILE

    @TypeConverter
    fun fromDocumentReadingStatus(value: DocumentReadingStatus?): String =
        value?.name ?: DocumentReadingStatus.UNREAD.name

    @TypeConverter
    fun toDocumentReadingStatus(value: String?): DocumentReadingStatus =
        value?.let { runCatching { DocumentReadingStatus.valueOf(it) }.getOrDefault(DocumentReadingStatus.UNREAD) }
            ?: DocumentReadingStatus.UNREAD
}
