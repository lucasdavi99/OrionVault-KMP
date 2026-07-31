package com.cuboidestudio.orionvault.storage.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VaultDatabaseTest {

    private fun inMemoryDatabase(): OrionVaultDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        OrionVaultDatabase.Schema.create(driver)
        return OrionVaultDatabase(driver)
    }

    @Test
    fun insertAndSelectFolderRoundTrips() {
        val db = inMemoryDatabase()
        db.vaultQueries.insertFolder("f1", null, "Trabalho", 1L, 1L)

        val roots = db.vaultQueries.selectFoldersByParent(null).executeAsList()

        assertEquals(1, roots.size)
        assertEquals("Trabalho", roots[0].name)
        assertNull(roots[0].parentId)
    }

    @Test
    fun insertAndSelectItemRoundTrips() {
        val db = inMemoryDatabase()
        db.vaultQueries.insertFolder("f1", null, "Trabalho", 1L, 1L)
        db.vaultQueries.insertItem("i1", "f1", "Gmail", "user@example.com", "cipher-pass", "https://gmail.com", "cipher-notes", 1L, 1L, 1L)

        val items = db.vaultQueries.selectItemsByFolder("f1").executeAsList()

        assertEquals(1, items.size)
        assertEquals("Gmail", items[0].title)
        assertEquals("cipher-pass", items[0].passwordCipher)
        assertEquals(1L, items[0].version)
    }

    @Test
    fun updateItemIncrementsVersion() {
        val db = inMemoryDatabase()
        db.vaultQueries.insertFolder("f1", null, "Trabalho", 1L, 1L)
        db.vaultQueries.insertItem("i1", "f1", "Gmail", null, "cipher-pass", null, null, 1L, 1L, 1L)

        db.vaultQueries.updateItem("Gmail", null, "cipher-pass-2", null, null, 2L, 2L, "i1")

        val item = db.vaultQueries.selectItemById("i1").executeAsOne()
        assertEquals("cipher-pass-2", item.passwordCipher)
        assertEquals(2L, item.version)
    }
}
