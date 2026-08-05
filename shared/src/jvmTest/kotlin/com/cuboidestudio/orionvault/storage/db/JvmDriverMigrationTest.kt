package com.cuboidestudio.orionvault.storage.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Cobre o bug pré-existente da fase 4.1 do plano: o `JdbcSqliteDriver` (Desktop) não migra
 * schema sozinho, ao contrário dos drivers Android/Native. Cria um banco com o schema
 * ANTIGO (versão 1), roda a migração e confere que o `user_version` avançou e que os dados
 * antigos sobreviveram.
 */
class JvmDriverMigrationTest {

    @Test
    fun migratesLegacyDatabaseAndPreservesData() {
        val dbFile = File.createTempFile("orionvault-migration", ".db").also { it.delete() }
        try {
            val legacy = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
            createLegacySchema(legacy)
            legacy.execute(
                null,
                "INSERT INTO Folder (id, parentId, name, createdAt, updatedAt) VALUES ('f1', NULL, 'Trabalho', 1, 1)",
                0
            )
            legacy.execute(
                null,
                "INSERT INTO Item (id, folderId, title, passwordCipher, createdAt, updatedAt, version) " +
                    "VALUES ('i1', 'f1', 'Gmail', 'c-pass', 1, 1, 1)",
                0
            )
            legacy.close()

            val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
            migrateIfNeeded(driver)

            assertEquals(OrionVaultDatabase.Schema.version, driver.userVersion())

            val db = OrionVaultDatabase(driver)
            val folder = db.vaultQueries.selectFolderById("f1").executeAsOne()
            assertEquals("Trabalho", folder.name)
            // Linhas pré-existentes entram como "sujas, nunca enviadas".
            assertEquals(1L, folder.syncState)
            assertEquals(0L, folder.syncedVersion)

            val item = db.vaultQueries.selectItemById("i1").executeAsOne()
            assertEquals("c-pass", item.passwordCipher)
            assertEquals(1L, item.syncState)

            // A tabela nova existe e responde.
            db.vaultQueries.ensureSyncMetaRow()
            db.vaultQueries.updateSyncCursor(7L)
            assertEquals(7L, db.vaultQueries.selectSyncMeta().executeAsOne().lastPulledAt)

            // Reabrir depois de migrado não deve tentar migrar de novo.
            migrateIfNeeded(driver)
            assertEquals(OrionVaultDatabase.Schema.version, driver.userVersion())
            driver.close()
        } finally {
            dbFile.delete()
        }
    }

    @Test
    fun schemaVersionIsAheadOfBaseline() {
        assertTrue(OrionVaultDatabase.Schema.version >= 2L, "A migração 1.sqm deve levar o schema à versão 2+")
    }

    private fun createLegacySchema(driver: SqlDriver) {
        driver.execute(
            null,
            """
            CREATE TABLE Folder (
              id TEXT NOT NULL PRIMARY KEY,
              parentId TEXT,
              name TEXT NOT NULL,
              createdAt INTEGER NOT NULL,
              updatedAt INTEGER NOT NULL
            )
            """.trimIndent(),
            0
        )
        driver.execute(null, "CREATE INDEX Folder_parentId ON Folder(parentId)", 0)
        driver.execute(
            null,
            """
            CREATE TABLE Item (
              id TEXT NOT NULL PRIMARY KEY,
              folderId TEXT,
              title TEXT NOT NULL,
              usernameCipher TEXT,
              emailCipher TEXT,
              passwordCipher TEXT NOT NULL,
              urlCipher TEXT,
              notesCipher TEXT,
              createdAt INTEGER NOT NULL,
              updatedAt INTEGER NOT NULL,
              version INTEGER NOT NULL
            )
            """.trimIndent(),
            0
        )
        driver.execute(null, "CREATE INDEX Item_folderId ON Item(folderId)", 0)
        driver.execute(null, "PRAGMA user_version = 1", 0)
    }

    private fun SqlDriver.userVersion(): Long =
        executeQuery(null, "PRAGMA user_version", { cursor ->
            QueryResult.Value(if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L)
        }, 0).value
}
