package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.sync.domain.model.FirstDownloadProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/*
 * Why: a first download is the sync that fills a device right after an account already on the server was restored on
 * it (a sign in, the recovery code), which Início and Grupos draw as "Sincronizando… 3 de 7 grupos". It starts before
 * the restored profile reaches Room, so every sync that runs with that profile began after it, and ends once its
 * groups came down, or the first of those syncs settles, failed or not. In memory (a Koin single), for the count: the
 * account's awaitsFullSync is what outlives a relaunch, until a sync settles (see ObserveFirstDownload).
 */
class FirstDownload {
    private val progress = MutableStateFlow<FirstDownloadProgress?>(null)

    val isPending: Boolean
        get() = progress.value != null

    // Why: null when there is no first download.
    fun observeProgress(): StateFlow<FirstDownloadProgress?> = progress.asStateFlow()

    fun start() {
        progress.value = FirstDownloadProgress(
            arrivedGroupCount = 0,
            groupCount = null,
            isWaitingForInternet = false,
        )
    }

    fun expect(groupCount: Int) {
        progress.update { it?.copy(groupCount = groupCount) }
    }

    fun groupArrived() {
        progress.update { it?.copy(arrivedGroupCount = it.arrivedGroupCount + 1) }
    }

    fun end() {
        progress.value = null
    }
}
