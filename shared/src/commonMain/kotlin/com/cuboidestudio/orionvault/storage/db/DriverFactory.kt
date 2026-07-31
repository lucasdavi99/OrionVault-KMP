package com.cuboidestudio.orionvault.storage.db

import app.cash.sqldelight.db.SqlDriver
import com.cuboidestudio.orionvault.storage.secure.PlatformContext

expect fun createSqlDriver(context: PlatformContext): SqlDriver

object VaultDatabaseProvider {
    fun create(context: PlatformContext): OrionVaultDatabase = OrionVaultDatabase(createSqlDriver(context))
}
