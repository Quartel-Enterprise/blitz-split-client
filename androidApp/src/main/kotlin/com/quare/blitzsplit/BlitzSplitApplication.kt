package com.quare.blitzsplit

import android.app.Application
import com.quare.blitzsplit.push.PushSetup
import com.quare.blitzsplit.shared.di.startBlitzSplitKoin
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext

class BlitzSplitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startBlitzSplitKoin { androidContext(this@BlitzSplitApplication) }
        PushSetup.start(this, setPushDevice = get())
    }
}
