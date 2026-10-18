package com.quare.blitzsplit

import android.app.Application
import com.quare.blitzsplit.shared.di.startBlitzSplitKoin
import org.koin.android.ext.koin.androidContext

class BlitzSplitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startBlitzSplitKoin { androidContext(this@BlitzSplitApplication) }
    }
}
