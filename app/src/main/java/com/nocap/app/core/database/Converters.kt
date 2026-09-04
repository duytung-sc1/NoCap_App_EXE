package com.nocap.app.core.database

import androidx.room.TypeConverter
import com.nocap.app.domain.model.DownloadStatus
import com.nocap.app.domain.model.EntitlementType

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
    fun fromPublicationFormat(value: com.nocap.app.domain.model.PublicationFormat?): String =
        value?.name ?: com.nocap.app.domain.model.PublicationFormat.EPUB.name

    @TypeConverter
    fun toPublicationFormat(value: String?): com.nocap.app.domain.model.PublicationFormat =
        value?.let { runCatching { com.nocap.app.domain.model.PublicationFormat.valueOf(it) }.getOrDefault(com.nocap.app.domain.model.PublicationFormat.EPUB) }
            ?: com.nocap.app.domain.model.PublicationFormat.EPUB

    @TypeConverter
    fun fromPublicationSourceType(value: com.nocap.app.domain.model.PublicationSourceType?): String =
        value?.name ?: com.nocap.app.domain.model.PublicationSourceType.LOCAL_FILE.name

    @TypeConverter
    fun toPublicationSourceType(value: String?): com.nocap.app.domain.model.PublicationSourceType =
        value?.let { runCatching { com.nocap.app.domain.model.PublicationSourceType.valueOf(it) }.getOrDefault(com.nocap.app.domain.model.PublicationSourceType.LOCAL_FILE) }
            ?: com.nocap.app.domain.model.PublicationSourceType.LOCAL_FILE
}
