package com.quare.blitzsplit.core.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/*
 * Why: a first download is the sync that fills a device right after an account already on the server was restored on
 * it (a sign in, the recovery code), which Início draws as "Sincronizando…". It starts before the restored profile
 * reaches Room, so every sync that runs with that profile began after it, and ends once the first of them settles,
 * failed or not. In memory (a Koin single): a relaunch, or Início made again (the web's tabs), is never one.
 */
class FirstDownload {
    private val isPending = MutableStateFlow(false)

    fun observeIsPending(): StateFlow<Boolean> = isPending.asStateFlow()

    fun start() {
        isPending.value = true
    }

    fun end() {
        isPending.value = false
    }
}
