package com.quare.blitzsplit.core.presentation.bridge

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/*
 * Why: Swift and JS can't collect a Flow, so the native UIs observe a ViewModel's state and actions
 * through a callback, delivered on the main thread.
 */
class FlowWatcher<T : Any>(
    private val flow: Flow<T>,
) {
    fun observe(onEach: (T) -> Unit): Watch {
        val job = MainScope().launch {
            flow.collect(onEach::invoke)
        }
        return Watch(job::cancel)
    }
}
