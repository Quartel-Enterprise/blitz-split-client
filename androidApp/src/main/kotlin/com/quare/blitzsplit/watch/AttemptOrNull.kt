package com.quare.blitzsplit.watch

import kotlinx.coroutines.CancellationException

/*
 * Why: a Data Layer call that may fail (no watch in reach, a node gone) without failing what called it, while a
 * cancellation still cancels. :core:utils' suspendRunCatching can't be inlined here: the app compiles for an older JVM.
 */
@Suppress("ktlint:quare:suspend-run-catching")
internal suspend fun <T> attemptOrNull(block: suspend () -> T): T? = try {
    block()
} catch (exception: CancellationException) {
    throw exception
} catch (_: Exception) {
    null
}
