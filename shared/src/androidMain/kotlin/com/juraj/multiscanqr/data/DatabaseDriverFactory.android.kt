package com.juraj.multiscanqr.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.juraj.multiscanqr.db.ScanDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(ScanDatabase.Schema, context, "scan_history.db")
}
