package com.nocap.app.core.util

import java.text.Normalizer
import java.util.regex.Pattern

object VietnameseUtils {

    private val DIACRITICS_PATTERN: Pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")

    /**
     * Removes diacritical marks from Vietnamese characters and converts 'đ'/'Đ' to 'd'/'D'.
     * Example: "Trí Tuệ Nhân Tạo" -> "Tri Tue Nhan Tao"
     */
    fun removeAccents(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        val withoutDiacritics = DIACRITICS_PATTERN.matcher(normalized).replaceAll("")
        return withoutDiacritics
            .replace('đ', 'd')
            .replace('Đ', 'D')
    }

    /**
     * Case-insensitive, accent-insensitive search check.
     * Returns true if [source] contains [query] regardless of case or Vietnamese diacritics.
     * Example: containsNormalized("Trí Tuệ Nhân Tạo", "tri tue") -> true
     */
    fun containsNormalized(source: String?, query: String?): Boolean {
        if (source.isNullOrBlank() || query.isNullOrBlank()) return false
        val normSource = removeAccents(source).lowercase()
        val normQuery = removeAccents(query).lowercase().trim()
        return normSource.contains(normQuery)
    }
}
