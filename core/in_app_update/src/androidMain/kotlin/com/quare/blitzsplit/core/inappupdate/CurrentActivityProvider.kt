package com.quare.blitzsplit.core.inappupdate

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/*
 * Why: Google Play's update flow starts from an Activity, while the shared code asks for it from wherever the app
 * comes to the front. The application registers this once, and it keeps the resumed Activity without holding it; the
 * other callbacks have nothing to do.
 */
class CurrentActivityProvider : Application.ActivityLifecycleCallbacks {
    private var current: WeakReference<Activity>? = null

    val activity: Activity?
        get() = current?.get()

    override fun onActivityResumed(activity: Activity) {
        current = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        // Why: only the resumed Activity matters.
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (current?.get() === activity) current = null
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {
        // Why: only the resumed Activity matters.
    }

    override fun onActivityStarted(activity: Activity) {
        // Why: only the resumed Activity matters.
    }

    override fun onActivityStopped(activity: Activity) {
        // Why: only the resumed Activity matters.
    }

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) {
        // Why: only the resumed Activity matters.
    }
}
