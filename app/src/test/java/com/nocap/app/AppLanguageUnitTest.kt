package com.nocap.app

import com.nocap.app.core.localization.AppLanguage
import com.nocap.app.core.localization.UiTranslations
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageUnitTest {
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
        assertEquals(AppLanguage.JAPANESE, AppLanguage.fromLanguageTag("ja-JP"))
        assertEquals(AppLanguage.SIMPLIFIED_CHINESE, AppLanguage.fromLanguageTag("zh-Hans-CN"))
        assertEquals(AppLanguage.KOREAN, AppLanguage.fromLanguageTag("ko-KR"))
    }

    @Test
    fun coreNavigationIsAvailableInEverySupportedLanguage() {
        assertEquals("Home", UiTranslations.translate("Trang chủ", AppLanguage.ENGLISH))
        assertEquals("ホーム", UiTranslations.translate("Trang chủ", AppLanguage.JAPANESE))
        assertEquals("首页", UiTranslations.translate("Trang chủ", AppLanguage.SIMPLIFIED_CHINESE))
        assertEquals("홈", UiTranslations.translate("Trang chủ", AppLanguage.KOREAN))
        assertEquals("Trang chủ", UiTranslations.translate("Trang chủ", AppLanguage.VIETNAMESE))
    }

    @Test
    fun unknownUserContentIsNeverMachineTranslated() {
        val userNote = "Một ghi chú riêng của người dùng"
        assertEquals(userNote, UiTranslations.translate(userNote, AppLanguage.ENGLISH))
        assertEquals(userNote, UiTranslations.translate(userNote, AppLanguage.JAPANESE))
    }
}
