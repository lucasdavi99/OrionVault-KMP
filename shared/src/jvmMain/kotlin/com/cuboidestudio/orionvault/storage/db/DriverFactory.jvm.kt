package com.cuboidestudio.orionvault.storage.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.cuboidestudio.orionvault.storage.secure.PlatformContext
import java.io.File

actual fun createSqlDriver(context: PlatformContext): SqlDriver {
    val dir = vaultDataDir()
    dir.mkdirs()
    val dbFile = File(dir, "orionvault.db")
    val needsSchema = !dbFile.exists() || dbFile.length() == 0L
    val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
    if (needsSchema) {
        OrionVaultDatabase.Schema.create(driver)
    }
    return driver
}

private fun vaultDataDir(): File {
    val isWindows = System.getProperty("os.name").lowercase().contains("windows")
    return if (isWindows) {
        File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "OrionVault")
    } else {
        File(System.getProperty("user.home"), ".orionvault")
    }
}
