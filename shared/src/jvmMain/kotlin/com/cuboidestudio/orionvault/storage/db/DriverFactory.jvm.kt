package com.cuboidestudio.orionvault.storage.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.cuboidestudio.orionvault.storage.secure.PlatformContext
import java.io.File

/**
 * Diferente do `AndroidSqliteDriver`/`NativeSqliteDriver` (que chamam `Schema.migrate`
 * sozinhos quando o `PRAGMA user_version` do arquivo está atrás de `Schema.version`), o
 * `JdbcSqliteDriver` não tem nenhum tratamento de migração embutido. Sem o bloco abaixo, um
 * banco já existente no Desktop ficaria para sempre no schema antigo e toda query nova
 * falharia em runtime.
 */
actual fun createSqlDriver(context: PlatformContext): SqlDriver {
    val dir = vaultDataDir()
    dir.mkdirs()
    val dbFile = File(dir, "orionvault.db")
    val needsSchema = !dbFile.exists() || dbFile.length() == 0L
    val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
    if (needsSchema) {
        OrionVaultDatabase.Schema.create(driver)
        driver.setUserVersion(OrionVaultDatabase.Schema.version)
    } else {
        migrateIfNeeded(driver)
    }
    return driver
}

internal fun migrateIfNeeded(driver: SqlDriver) {
    val currentVersion = driver.getUserVersion()
    val targetVersion = OrionVaultDatabase.Schema.version
    if (currentVersion == 0L) {
        // Bancos criados antes de qualquer controle de versão nunca tiveram `user_version`
        // escrito: são, por definição, o schema base (versão 1).
        driver.setUserVersion(1L)
    }
    val from = if (currentVersion == 0L) 1L else currentVersion
    if (from < targetVersion) {
        OrionVaultDatabase.Schema.migrate(driver, from, targetVersion).value
        driver.setUserVersion(targetVersion)
    }
}

private fun SqlDriver.getUserVersion(): Long =
    executeQuery(null, "PRAGMA user_version", { cursor ->
        QueryResult.Value(if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L)
    }, 0).value

private fun SqlDriver.setUserVersion(version: Long) {
    execute(null, "PRAGMA user_version = $version", 0)
}

private fun vaultDataDir(): File {
    val isWindows = System.getProperty("os.name").lowercase().contains("windows")
    return if (isWindows) {
        File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "OrionVault")
    } else {
        File(System.getProperty("user.home"), ".orionvault")
    }
}
