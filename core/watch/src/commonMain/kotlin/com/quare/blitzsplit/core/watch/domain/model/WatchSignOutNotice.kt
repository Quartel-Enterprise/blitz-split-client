package com.quare.blitzsplit.core.watch.domain.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/*
 * Why: the phone's sign out reached this watch, which shows 15.13 instead of the first launch's welcome. Kept in
 * memory: a watch started again later shows the welcome, which says the same thing to do.
 */
class WatchSignOutNotice {
    private val mutableIsShown = MutableStateFlow(false)

    val isShown: StateFlow<Boolean> = mutableIsShown.asStateFlow()

    fun show() {
        mutableIsShown.value = true
    }

    fun dismiss() {
        mutableIsShown.value = false
    }
}
