package com.quare.blitzsplit.core.utils

import kotlinx.coroutines.flow.Flow

/*
 * Why: what a screen that says how long ago something happened counts again on, so a home left open turns "hoje"
 * into "ontem" at midnight with no change to its data. Injected so tests decide when a minute passes.
 */
fun interface MinuteTicks {
    operator fun invoke(): Flow<Unit>
}
