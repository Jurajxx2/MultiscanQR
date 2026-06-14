package com.juraj.multiscanqr.di

import com.juraj.multiscanqr.data.DatabaseDriverFactory
import com.juraj.multiscanqr.data.ScanHistoryRepository
import com.juraj.multiscanqr.data.SqlDelightScanHistoryRepository
import com.juraj.multiscanqr.history.HistoryViewModel
import com.juraj.multiscanqr.scanner.ScannerViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun appModule(driverFactory: DatabaseDriverFactory): Module = module {
    single { driverFactory }
    single<ScanHistoryRepository> { SqlDelightScanHistoryRepository(get()) }
    viewModel { ScannerViewModel(repository = get()) }
    viewModel { HistoryViewModel(repository = get()) }
}
