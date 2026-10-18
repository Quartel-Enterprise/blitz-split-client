package com.quare.blitzsplit.core.utils

import kotlinx.coroutines.CancellationException

// Why: stdlib runCatching swallows CancellationException, which breaks structured concurrency.
@Suppress("ktlint:quare:suspend-run-catching")
inline fun <T> suspendRunCatching(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}
