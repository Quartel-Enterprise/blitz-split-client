package com.quare.blitzsplit.core.releasenotes.domain.usecase

import kotlinx.coroutines.flow.Flow

/*
 * Why: "Show beta versions" as it stands on this device: what was switched there, or, until it was, on in a beta build
 * (whose own notes are a beta's) and off in a stable one.
 */
fun interface ObserveShowsBetaReleaseNotes {
    operator fun invoke(): Flow<Boolean>
}
