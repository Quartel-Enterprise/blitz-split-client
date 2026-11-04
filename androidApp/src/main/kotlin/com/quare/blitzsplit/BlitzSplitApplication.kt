package com.quare.blitzsplit

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.quare.blitzsplit.core.provider.crashlytics.startCrashReporting
import com.quare.blitzsplit.push.PushSetup
import com.quare.blitzsplit.shared.di.startBlitzSplitKoin
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext

class BlitzSplitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.HAS_FIREBASE) startCrashReporting(isDebug = BuildConfig.DEBUG)
        startBlitzSplitKoin { androidContext(this@BlitzSplitApplication) }
        PushSetup.start(this, setPushDevice = get())
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppForegroundObserver(reportAppForeground = get()))
    }
}
