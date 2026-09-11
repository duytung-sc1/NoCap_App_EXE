package com.nocap.app

import com.nocap.app.core.localization.AppLanguage
import com.nocap.app.core.localization.UiTranslations
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageUnitTest {
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
