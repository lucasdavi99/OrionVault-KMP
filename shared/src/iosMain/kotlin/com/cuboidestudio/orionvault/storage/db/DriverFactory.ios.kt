package com.cuboidestudio.orionvault.storage.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.cuboidestudio.orionvault.storage.secure.PlatformContext

actual fun createSqlDriver(context: PlatformContext): SqlDriver =
    NativeSqliteDriver(OrionVaultDatabase.Schema, "orionvault.db")
