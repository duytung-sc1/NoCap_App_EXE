package com.nocap.app.core.datastore

enum class LibrarySort(val displayName: String) {
    RECENTLY_ADDED("Mới thêm"),
    RECENTLY_OPENED("Mới đọc"),
    TITLE_ASC("Tên (A-Z)"),
    TITLE_DESC("Tên (Z-A)"),
    AUTHOR("Tác giả"),
    PROGRESS("Tiến độ đọc")
}

enum class LibrarySmartView(val displayName: String) {
    ALL("Tất cả"),
    PINNED("Đã ghim"),
    FAVORITES("Yêu thích"),
    RECENTLY_OPENED("Mới đọc"),
    RECENTLY_ADDED("Mới thêm"),
    UNREAD("Chưa đọc"),
    READING("Đang đọc"),
    COMPLETED("Hoàn thành"),
    ARCHIVED("Lưu trữ"),
    EPUB("EPUB"),
    PDF("PDF"),
    FROM_WEB("Từ Web")
}

data class LibraryPreferences(
    val sort: LibrarySort = LibrarySort.RECENTLY_OPENED,
    val smartView: LibrarySmartView = LibrarySmartView.ALL,
    val isGridMode: Boolean = false
)
