package com.nocap.app

import androidx.sqlite.db.SupportSQLiteDatabase
import com.nocap.app.core.database.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class DatabaseMigrationTest {

    @Test
    fun `test MIGRATION_1_2 executes correct alter table statements`() {
        val executedSqls = mutableListOf<String>()

        val dbProxy = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL") {
                executedSqls.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        assertEquals(1, AppDatabase.MIGRATION_1_2.startVersion)
        assertEquals(2, AppDatabase.MIGRATION_1_2.endVersion)

        AppDatabase.MIGRATION_1_2.migrate(dbProxy)

        assertEquals(4, executedSqls.size)
        assertTrue(executedSqls.any { it.contains("ALTER TABLE catalog_books ADD COLUMN format TEXT NOT NULL DEFAULT 'EPUB'") })
        assertTrue(executedSqls.any { it.contains("ALTER TABLE catalog_books ADD COLUMN media_type TEXT NOT NULL DEFAULT 'application/epub+zip'") })
        assertTrue(executedSqls.any { it.contains("ALTER TABLE catalog_books ADD COLUMN source_type TEXT NOT NULL DEFAULT 'LOCAL_FILE'") })
        assertTrue(executedSqls.any { it.contains("ALTER TABLE catalog_books ADD COLUMN source_url TEXT DEFAULT NULL") })
    }
}
