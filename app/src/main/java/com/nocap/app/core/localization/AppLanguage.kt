package com.nocap.app.core.localization

import android.content.Context
import android.content.res.Configuration
import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import java.util.Locale

enum class AppLanguage(val languageTag: String, val nativeName: String) {
    ENGLISH("en", "English"),
    VIETNAMESE("vi-VN", "Tiếng Việt"),
    JAPANESE("ja", "日本語"),
    SIMPLIFIED_CHINESE("zh-CN", "简体中文"),
    KOREAN("ko", "한국어");

    val locale: Locale get() = Locale.forLanguageTag(languageTag)

    companion object {
        fun fromLanguageTag(value: String?): AppLanguage = when {
            value?.startsWith("vi", ignoreCase = true) == true -> VIETNAMESE
            value?.startsWith("ja", ignoreCase = true) == true -> JAPANESE
            value?.startsWith("zh", ignoreCase = true) == true -> SIMPLIFIED_CHINESE
            value?.startsWith("ko", ignoreCase = true) == true -> KOREAN
            else -> ENGLISH
        }
    }
}

object AppLanguageManager {
    private const val PREFERENCES = "app_language_preferences"
    private const val KEY_LANGUAGE_TAG = "language_tag"

    fun selected(context: Context): AppLanguage {
        val tag = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE_TAG, null)
        if (tag == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val systemTag = context.getSystemService(LocaleManager::class.java)
                ?.applicationLocales
                ?.takeUnless { it.isEmpty }
                ?.get(0)
                ?.toLanguageTag()
            if (systemTag != null) return AppLanguage.fromLanguageTag(systemTag)
        }
        return AppLanguage.fromLanguageTag(tag)
    }

    fun select(context: Context, language: AppLanguage): Boolean {
        val committed = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE_TAG, language.languageTag)
            .commit()
        if (committed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)?.applicationLocales =
                LocaleList.forLanguageTags(language.languageTag)
        }
        return committed
    }

    fun wrap(base: Context): Context {
        val language = selected(base)
        Locale.setDefault(language.locale)
        val configuration = Configuration(base.resources.configuration).apply {
            setLocale(language.locale)
            setLayoutDirection(language.locale)
        }
        return base.createConfigurationContext(configuration)
    }

    fun applyDefault(context: Context) {
        Locale.setDefault(selected(context).locale)
    }

    fun translate(context: Context, text: String): String =
        UiTranslations.translate(text, selected(context))
}
