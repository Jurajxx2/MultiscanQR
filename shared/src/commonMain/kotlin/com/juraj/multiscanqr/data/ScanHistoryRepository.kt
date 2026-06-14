package com.juraj.multiscanqr.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.juraj.multiscanqr.db.ScanDatabase
import com.juraj.multiscanqr.db.Scan_entry
import com.juraj.multiscanqr.domain.ScanContentType
import com.juraj.multiscanqr.domain.ScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface ScanHistoryRepository {
    fun observeHistory(): Flow<List<ScanResult>>
    suspend fun save(content: String, type: ScanContentType, scannedAtEpochMillis: Long)
    suspend fun delete(id: Long)
    suspend fun clearAll()
}

class SqlDelightScanHistoryRepository(
    driverFactory: DatabaseDriverFactory,
) : ScanHistoryRepository {

    private val queries = ScanDatabase(driverFactory.createDriver()).scanHistoryQueries

    override fun observeHistory(): Flow<List<ScanResult>> =
        queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun save(content: String, type: ScanContentType, scannedAtEpochMillis: Long) {
        withContext(Dispatchers.Default) {
            queries.insert(content, type.name, scannedAtEpochMillis)
        }
    }

    override suspend fun delete(id: Long) {
        withContext(Dispatchers.Default) {
            queries.deleteById(id)
        }
    }

    override suspend fun clearAll() {
        withContext(Dispatchers.Default) {
            queries.clearAll()
        }
    }

    private fun Scan_entry.toDomain() = ScanResult(
        id = id,
        content = content,
        type = ScanContentType.entries.firstOrNull { it.name == content_type }
            ?: ScanContentType.TEXT,
        scannedAtEpochMillis = scanned_at_epoch_millis,
    )
}
