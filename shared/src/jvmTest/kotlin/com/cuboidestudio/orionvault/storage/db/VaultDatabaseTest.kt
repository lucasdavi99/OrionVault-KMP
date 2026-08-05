package com.cuboidestudio.orionvault.storage.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.cuboidestudio.orionvault.domain.model.SyncState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VaultDatabaseTest {

    private fun inMemoryDatabase(): OrionVaultDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        OrionVaultDatabase.Schema.create(driver)
        return OrionVaultDatabase(driver)
    }

    @Test
    fun insertAndSelectFolderRoundTrips() {
        val db = inMemoryDatabase()
        db.vaultQueries.insertFolder("f1", null, "Trabalho", 1L, 1L, 1L, SyncState.DIRTY_NEW, 0L)

        val roots = db.vaultQueries.selectFoldersByParent(null).executeAsList()

        assertEquals(1, roots.size)
        assertEquals("Trabalho", roots[0].name)
        assertNull(roots[0].parentId)
        assertEquals(1L, roots[0].version)
    }

    @Test
    fun insertAndSelectItemRoundTrips() {
        val db = inMemoryDatabase()
        db.vaultQueries.insertFolder("f1", null, "Trabalho", 1L, 1L, 1L, SyncState.DIRTY_NEW, 0L)
        db.vaultQueries.insertItem(
            "i1", "f1", "Gmail", "user@example.com", "cipher-email", "cipher-pass",
            "https://gmail.com", "cipher-notes", 1L, 1L, 1L, SyncState.DIRTY_NEW, 0L
        )

        val items = db.vaultQueries.selectItemsByFolder("f1").executeAsList()

        assertEquals(1, items.size)
        assertEquals("Gmail", items[0].title)
        assertEquals("cipher-email", items[0].emailCipher)
        assertEquals("cipher-pass", items[0].passwordCipher)
        assertEquals(1L, items[0].version)
    }

    @Test
    fun insertAndSelectStandaloneItemRoundTrips() {
        val db = inMemoryDatabase()
        db.vaultQueries.insertItem(
            "i1", null, "Gmail", null, null, "cipher-pass", null, null, 1L, 1L, 1L, SyncState.DIRTY_NEW, 0L
        )

        val items = db.vaultQueries.selectItemsByFolder(null).executeAsList()

        assertEquals(1, items.size)
        assertEquals("Gmail", items[0].title)
        assertNull(items[0].folderId)
    }

    @Test
    fun updateItemIncrementsVersion() {
        val db = inMemoryDatabase()
        db.vaultQueries.insertFolder("f1", null, "Trabalho", 1L, 1L, 1L, SyncState.DIRTY_NEW, 0L)
        db.vaultQueries.insertItem(
            "i1", "f1", "Gmail", null, null, "cipher-pass", null, null, 1L, 1L, 1L, SyncState.CLEAN, 1L
        )

        db.vaultQueries.updateItem(
            "f1", "Gmail", null, null, "cipher-pass-2", null, null, 2L, 2L, SyncState.DIRTY_UPDATED, "i1"
        )

        val item = db.vaultQueries.selectItemById("i1").executeAsOne()
        assertEquals("cipher-pass-2", item.passwordCipher)
        assertEquals(2L, item.version)
        assertEquals(SyncState.DIRTY_UPDATED, item.syncState)
        assertEquals(1L, item.syncedVersion)
    }

    @Test
    fun tombstonedItemIsHiddenFromListsAndLosesItsCiphertext() {
        val db = inMemoryDatabase()
        db.vaultQueries.insertItem(
            "i1", null, "Gmail", "c-user", "c-email", "c-pass", "c-url", "c-notes",
            1L, 1L, 1L, SyncState.CLEAN, 1L
        )

        db.vaultQueries.tombstoneItem(5L, "i1")

        assertTrue(db.vaultQueries.selectItemsByFolder(null).executeAsList().isEmpty())
        val row = db.vaultQueries.selectItemById("i1").executeAsOne()
        assertEquals(SyncState.TOMBSTONE, row.syncState)
        assertNull(row.usernameCipher)
        assertNull(row.emailCipher)
        assertNull(row.urlCipher)
        assertNull(row.notesCipher)
        assertEquals("", row.passwordCipher)
        // Continua visível para o push, que precisa propagar o delete ao servidor.
        assertEquals(1, db.vaultQueries.selectDirtyItems().executeAsList().size)

        db.vaultQueries.purgeTombstoneItem("i1")
        assertNull(db.vaultQueries.selectItemById("i1").executeAsOneOrNull())
    }

    @Test
    fun tombstonedFolderIsHiddenFromListsUntilPurged() {
        val db = inMemoryDatabase()
        db.vaultQueries.insertFolder("f1", null, "Trabalho", 1L, 1L, 1L, SyncState.CLEAN, 1L)

        db.vaultQueries.tombstoneFolder(5L, "f1")

        assertTrue(db.vaultQueries.selectAllFolders().executeAsList().isEmpty())
        assertTrue(db.vaultQueries.selectFoldersByParent(null).executeAsList().isEmpty())
        assertEquals(1, db.vaultQueries.selectDirtyFolders().executeAsList().size)

        db.vaultQueries.purgeTombstoneFolder("f1")
        assertNull(db.vaultQueries.selectFolderById("f1").executeAsOneOrNull())
    }

    @Test
    fun upsertRemoteItemOverwritesLocalRowAndMarksItClean() {
        val db = inMemoryDatabase()
        db.vaultQueries.insertItem(
            "i1", null, "Antigo", null, null, "c-old", null, null, 1L, 1L, 1L, SyncState.DIRTY_NEW, 0L
        )

        db.vaultQueries.upsertRemoteItem(
            "i1", null, "Novo", null, null, "c-new", null, null, 1L, 9L, 4L, 4L
        )

        val row = db.vaultQueries.selectItemById("i1").executeAsOne()
        assertEquals("Novo", row.title)
        assertEquals("c-new", row.passwordCipher)
        assertEquals(SyncState.CLEAN, row.syncState)
        assertEquals(4L, row.syncedVersion)
        assertEquals(9L, row.updatedAt)
    }

    @Test
    fun syncMetaKeepsCursorAndDeviceIdIndependently() {
        val db = inMemoryDatabase()

        assertNull(db.vaultQueries.selectSyncMeta().executeAsOneOrNull())

        db.vaultQueries.ensureSyncMetaRow()
        db.vaultQueries.updateSyncCursor(42L)
        db.vaultQueries.updateDeviceId("device-a")

        val meta = db.vaultQueries.selectSyncMeta().executeAsOne()
        assertEquals(42L, meta.lastPulledAt)
        assertEquals("device-a", meta.deviceId)
    }
}
