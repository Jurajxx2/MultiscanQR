package com.juraj.multiscanqr.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.juraj.multiscanqr.db.ScanDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(ScanDatabase.Schema, "scan_history.db")
}
