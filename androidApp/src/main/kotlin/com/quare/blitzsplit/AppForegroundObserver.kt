package com.quare.blitzsplit

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.quare.blitzsplit.core.sync.domain.usecase.ReportAppForeground

/*
 * Why: real time listens to the other devices' changes only while the app is in front. The process lifecycle, not an
 * activity's: it waits a moment before stopping, so a rotation or the browser sign in coming back never drops the
 * channel.
 */
class AppForegroundObserver(
    private val reportAppForeground: ReportAppForeground,
) : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) {
        reportAppForeground(true)
    }

    override fun onStop(owner: LifecycleOwner) {
        reportAppForeground(false)
    }
}
