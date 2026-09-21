package com.nocap.app.domain.model

data class Announcement(
    val id: String,
    val title: String,
    val message: String,
    val type: AnnouncementType,
    val startsAt: String? = null,
    val endsAt: String? = null,
    val active: Boolean = true
)

enum class AnnouncementType {
    INFO, WARNING, MAINTENANCE;

    companion object {
        fun from(value: String): AnnouncementType = when (value.lowercase()) {
            "warning"     -> WARNING
            "maintenance" -> MAINTENANCE
            else          -> INFO
        }
    }
}
