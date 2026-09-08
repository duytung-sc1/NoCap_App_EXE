package com.nocap.app.domain.model

data class Category(
    val id: String,
    val name: String,
    val iconUrl: String? = null,
    val displayOrder: Int = 0
) {
    val vietnameseName: String
        get() = when (id) {
            "fiction" -> "Hư cấu"
            "non-fiction" -> "Phi hư cấu"
            "mystery" -> "Trinh thám"
            "sci-fi" -> "Khoa học viễn tưởng"
            "romance" -> "Lãng mạn"
            "history" -> "Lịch sử"
            "biography" -> "Tiểu sử"
            "self-help" -> "Phát triển bản thân"
            else -> name
        }
}
