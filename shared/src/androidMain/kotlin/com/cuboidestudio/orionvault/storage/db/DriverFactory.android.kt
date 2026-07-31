package com.cuboidestudio.orionvault.storage.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.cuboidestudio.orionvault.storage.secure.PlatformContext

actual fun createSqlDriver(context: PlatformContext): SqlDriver =
    AndroidSqliteDriver(OrionVaultDatabase.Schema, context.context, "orionvault.db")
