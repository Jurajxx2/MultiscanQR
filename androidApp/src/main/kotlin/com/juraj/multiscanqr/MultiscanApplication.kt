package com.juraj.multiscanqr

import android.app.Application
import com.juraj.multiscanqr.data.DatabaseDriverFactory
import com.juraj.multiscanqr.di.appModule
import org.koin.core.context.startKoin

class MultiscanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            modules(appModule(DatabaseDriverFactory(this@MultiscanApplication)))
        }
    }
}
