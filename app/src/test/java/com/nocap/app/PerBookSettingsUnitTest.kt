package com.nocap.app

import com.nocap.app.core.database.dao.PerBookPreferencesDao
import com.nocap.app.core.database.entity.PerBookPreferencesEntity
import com.nocap.app.core.datastore.ReaderFontFamily
import com.nocap.app.core.datastore.ReaderPreferences
import com.nocap.app.core.datastore.ReaderTextAlignment
import com.nocap.app.core.datastore.ReaderTheme
import com.nocap.app.data.preferences.LocalPerBookPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PerBookSettingsUnitTest {

    private lateinit var fakeDao: FakePerBookDao
    private lateinit var repository: LocalPerBookPreferencesRepository

    @Before
    fun setup() {
        fakeDao = FakePerBookDao()
        repository = LocalPerBookPreferencesRepository(fakeDao)
    }

    private fun resolvePreferences(
        global: ReaderPreferences,
        perBook: PerBookPreferencesEntity?
    ): ReaderPreferences {
        if (perBook != null && perBook.useBookOverride) {
            val theme = perBook.theme?.let { runCatching { ReaderTheme.valueOf(it) }.getOrNull() } ?: global.theme
            val fontFamily = perBook.fontFamily?.let { runCatching { ReaderFontFamily.valueOf(it) }.getOrNull() } ?: global.fontFamily
            val fontSize = perBook.fontSize ?: global.fontSizeMultiplier
            val lineHeight = perBook.lineHeight ?: global.lineHeightMultiplier
            val textAlignment = perBook.textAlignment?.let { runCatching { ReaderTextAlignment.valueOf(it) }.getOrNull() } ?: global.textAlignment
            val isScrollMode = perBook.scrollMode ?: global.isScrollMode

            return global.copy(
                theme = theme,
                fontFamily = fontFamily,
                fontSizeMultiplier = fontSize,
                lineHeightMultiplier = lineHeight,
                textAlignment = textAlignment,
                isScrollMode = isScrollMode
            )
        }
        return global
    }

    @Test
    fun resolvePreferences_inheritsGlobalDefaults_whenNoOverrideExists() {
        val global = ReaderPreferences(
            theme = ReaderTheme.LIGHT,
            fontSizeMultiplier = 1.0f,
            lineHeightMultiplier = 1.4f,
            fontFamily = ReaderFontFamily.SERIF
        )
        val resolved = resolvePreferences(global, null)
        assertEquals(global, resolved)
    }

    @Test
    fun resolvePreferences_inheritsGlobalDefaults_whenUseBookOverrideIsFalse() {
        val global = ReaderPreferences(
            theme = ReaderTheme.LIGHT,
            fontSizeMultiplier = 1.0f
        )
        val perBook = PerBookPreferencesEntity(
            bookId = "book_1",
            theme = ReaderTheme.DARK.name,
            fontSize = 1.8f,
            useBookOverride = false
        )
        val resolved = resolvePreferences(global, perBook)
        assertEquals(ReaderTheme.LIGHT, resolved.theme)
        assertEquals(1.0f, resolved.fontSizeMultiplier, 0.001f)
    }

    @Test
    fun resolvePreferences_appliesPerBookOverride_whenUseBookOverrideIsTrue() {
        val global = ReaderPreferences(
            theme = ReaderTheme.LIGHT,
            fontSizeMultiplier = 1.0f,
            fontFamily = ReaderFontFamily.SERIF
        )
        val perBook = PerBookPreferencesEntity(
            bookId = "book_1",
            theme = ReaderTheme.DARK.name,
            fontSize = 1.5f,
            fontFamily = ReaderFontFamily.ROBOTO.name,
            useBookOverride = true
        )
        val resolved = resolvePreferences(global, perBook)
        assertEquals(ReaderTheme.DARK, resolved.theme)
        assertEquals(1.5f, resolved.fontSizeMultiplier, 0.001f)
        assertEquals(ReaderFontFamily.ROBOTO, resolved.fontFamily)
    }

    @Test
    fun resolvePreferences_fallsBackToGlobal_forUnspecifiedFieldsInOverride() {
        val global = ReaderPreferences(
            theme = ReaderTheme.SEPIA,
            fontSizeMultiplier = 1.2f,
            lineHeightMultiplier = 1.6f
        )
        val perBook = PerBookPreferencesEntity(
            bookId = "book_1",
            fontSize = 2.0f,
            theme = null,
            lineHeight = null,
            useBookOverride = true
        )
        val resolved = resolvePreferences(global, perBook)
        assertEquals(2.0f, resolved.fontSizeMultiplier, 0.001f)
        assertEquals(ReaderTheme.SEPIA, resolved.theme)
        assertEquals(1.6f, resolved.lineHeightMultiplier, 0.001f)
    }

    @Test
    fun repository_saveAndResetToDefaults_worksCorrectly() = runBlocking {
        val bookId = "book_100"
        val entity = PerBookPreferencesEntity(
            bookId = bookId,
            fontSize = 1.4f,
            useBookOverride = true
        )

        val saveResult = repository.savePreferences(entity)
        assertTrue(saveResult.isSuccess)
        val saved = repository.getPreferences(bookId)
        assertNotNull(saved)
        assertEquals(1.4f, saved?.fontSize ?: 0f, 0.001f)
        assertTrue(saved?.useBookOverride == true)

        val resetResult = repository.resetToDefaults(bookId)
        assertTrue(resetResult.isSuccess)
        val afterReset = repository.getPreferences(bookId)
        assertNull(afterReset)
    }

    class FakePerBookDao : PerBookPreferencesDao {
        val map = mutableMapOf<String, PerBookPreferencesEntity>()

        override fun observePreferences(bookId: String): Flow<PerBookPreferencesEntity?> =
            flowOf(map[bookId])

        override suspend fun getPreferences(bookId: String): PerBookPreferencesEntity? =
            map[bookId]

        override suspend fun insertOrUpdate(preferences: PerBookPreferencesEntity) {
            map[preferences.bookId] = preferences
        }

        override suspend fun deletePreferences(bookId: String) {
            map.remove(bookId)
        }
    }
}
