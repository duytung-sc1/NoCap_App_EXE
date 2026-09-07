package com.nocap.app.domain.model

enum class DocumentReadingStatus(val displayName: String) {
    UNREAD("Chưa đọc"),
    READING("Đang đọc"),
    COMPLETED("Hoàn thành")
}
