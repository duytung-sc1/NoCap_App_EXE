package com.nocap.app

import com.nocap.app.core.localization.AppLanguage
import com.nocap.app.core.localization.UiTranslations
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageUnitTest {
    @Test fun onlyEnglishAndVietnameseAreSelectable() {
        assertEquals(listOf("en", "vi-VN"), AppLanguage.entries.map { it.languageTag })
    }

    @Test fun switchingBackAndForthDoesNotRetainPreviousUiLanguage() {
        val labels = mapOf("Tô sáng" to "Highlights", "Ghi chú" to "Notes",
            "Ngang" to "Horizontal", "Trang sau" to "Next page", "Thư viện" to "Library")
        repeat(3) {
            for ((vi, en) in labels) {
                assertEquals(en, UiTranslations.translate(vi, AppLanguage.ENGLISH))
                assertEquals(vi, UiTranslations.translate(vi, AppLanguage.VIETNAMESE))
            }
            assertEquals("12 documents", UiTranslations.translate("12 tài liệu", AppLanguage.ENGLISH))
            assertEquals("12 tài liệu", UiTranslations.translate("12 tài liệu", AppLanguage.VIETNAMESE))
        }
    }
    @Test fun libraryFiltersAndSortLabelsAreLocalizedInAllLanguages() {
        val labels = com.nocap.app.core.datastore.LibrarySmartView.entries.map { it.displayName } +
            com.nocap.app.core.datastore.LibrarySort.entries.map { it.displayName } +
            listOf("fiction", "non-fiction", "mystery", "sci-fi", "romance", "history", "biography", "self-help")
                .map { com.nocap.app.domain.model.Category(it, it).vietnameseName }
        val formats = setOf("EPUB", "PDF", "TXT", "Markdown", "HTML", "DOCX", "CBZ")
        for (language in AppLanguage.entries) for (label in labels) {
            val actual = UiTranslations.translate(label, language)
            if (language == AppLanguage.VIETNAMESE || label in formats) assertEquals(label, actual)
            else org.junit.Assert.assertNotEquals("$language: $label", label, actual)
        }
    }
    @Test
    fun newInstallDefaultsToEnglishForUnknownOrMissingTag() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLanguageTag(null))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLanguageTag("en-US"))
    }

    @Test
    fun supportedLanguageTagsAreNormalized() {
        assertEquals(AppLanguage.VIETNAMESE, AppLanguage.fromLanguageTag("vi-VN"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLanguageTag("ja-JP"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLanguageTag("zh-Hans-CN"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLanguageTag("ko-KR"))
    }

    @Test
    fun coreNavigationIsAvailableInEverySupportedLanguage() {
        assertEquals("Home", UiTranslations.translate("Trang chủ", AppLanguage.ENGLISH))
        assertEquals("Trang chủ", UiTranslations.translate("Trang chủ", AppLanguage.VIETNAMESE))
    }

    @Test
    fun unknownUserContentIsNeverMachineTranslated() {
        val userNote = "Một ghi chú riêng của người dùng"
        assertEquals(userNote, UiTranslations.translate(userNote, AppLanguage.ENGLISH))
        assertEquals(userNote, UiTranslations.translate(userNote, AppLanguage.VIETNAMESE))
    }

    @Test
    fun advancedProScreensDoNotLeakVietnameseIntoEnglishUi() {
        val labels = mapOf(
            "Tự động tạo danh sách ôn tập" to "Create review queue automatically",
            "Ôn nhanh 5 phút" to "5-minute quick review",
            "Tiến trình ghi nhớ" to "Memory progress",
            "Xuất và sử dụng tri thức" to "Export and use your knowledge",
            "Lịch sử phiên bản ghi chú" to "Note version history",
            "Khôi phục theo thời điểm" to "Restore a previous backup",
            "Chưa lưu được kết quả ôn tập. Hãy thử lại." to "The review result could not be saved. Try again."
        )
        for ((vi, en) in labels) {
            assertEquals(en, UiTranslations.translate(vi, AppLanguage.ENGLISH))
            assertEquals(vi, UiTranslations.translate(vi, AppLanguage.VIETNAMESE))
        }
        assertEquals(
            "Backup history (10)",
            UiTranslations.translate("Lịch sử sao lưu (10)", AppLanguage.ENGLISH)
        )
        assertEquals(
            "You reviewed 7 cards in this session.",
            UiTranslations.translate("Bạn đã ôn luyện xong 7 thẻ trong phiên này.", AppLanguage.ENGLISH)
        )
    }
}
