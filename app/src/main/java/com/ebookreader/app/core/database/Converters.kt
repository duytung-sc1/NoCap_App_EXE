package com.ebookreader.app.core.database

import androidx.room.TypeConverter
import com.ebookreader.app.domain.model.DownloadStatus
import com.ebookreader.app.domain.model.EntitlementType

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
}
