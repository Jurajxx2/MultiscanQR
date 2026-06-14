package com.juraj.multiscanqr

import androidx.compose.ui.window.ComposeUIViewController
import com.juraj.multiscanqr.data.DatabaseDriverFactory
import com.juraj.multiscanqr.di.appModule
import org.koin.core.context.startKoin

private val koinApplication by lazy {
    startKoin {
        modules(appModule(DatabaseDriverFactory()))
    }
}

@Suppress("unused", "FunctionName") // called from Swift
fun MainViewController() = run {
    koinApplication
    ComposeUIViewController { App() }
}
