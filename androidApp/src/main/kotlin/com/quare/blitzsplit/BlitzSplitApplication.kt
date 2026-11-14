package com.quare.blitzsplit

import android.app.Application
import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import com.quare.blitzsplit.core.inappupdate.CurrentActivityProvider
import com.quare.blitzsplit.core.provider.analytics.AnalyticsHub
import com.quare.blitzsplit.core.provider.analytics.FirebaseAnalyticsService
import com.quare.blitzsplit.core.provider.crashlytics.startCrashReporting
import com.quare.blitzsplit.push.PushSetup
import com.quare.blitzsplit.shared.di.startBlitzSplitKoin
import com.quare.blitzsplit.shared.settings.LegacyAppLocale
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext

class BlitzSplitApplication : Application() {
    /*
     * Why: before Android 13 the application's own strings (the notification channels' names) follow Profile ›
     * Language's pick from the next start of the app.
     */
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LegacyAppLocale.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.HAS_FIREBASE) startCrashReporting(isDebug = BuildConfig.DEBUG)
        startBlitzSplitKoin { androidContext(this@BlitzSplitApplication) }
        if (BuildConfig.HAS_FIREBASE && BuildConfig.COLLECTS_ANALYTICS && !BuildConfig.DEBUG) {
            get<AnalyticsHub>().attach(FirebaseAnalyticsService(this))
        }
        PushSetup.start(this, setPushDevice = get())
        // Why: Google Play's update flow starts from the Activity in front, whichever screen asked for it.
        registerActivityLifecycleCallbacks(get<CurrentActivityProvider>())
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppForegroundObserver(reportAppForeground = get()))
    }
}
