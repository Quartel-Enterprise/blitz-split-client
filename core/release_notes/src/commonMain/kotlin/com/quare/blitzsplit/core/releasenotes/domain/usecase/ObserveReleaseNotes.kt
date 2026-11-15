package com.quare.blitzsplit.core.releasenotes.domain.usecase

import com.quare.blitzsplit.core.releasenotes.domain.model.ReleaseNotes
import kotlinx.coroutines.flow.Flow

/*
 * Why: what this app shows of the versions the device saved for that language, newest first: only the notes that are
 * for the platform it runs on, only the versions that have one, and none newer than the app itself, which a store may
 * not offer yet. Null until the device downloaded them.
 */
fun interface ObserveReleaseNotes {
    operator fun invoke(language: String): Flow<List<ReleaseNotes>?>
}
