package com.quare.blitzsplit.core.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

// Why: whether any of the parts has a change waiting to go up, which each repository reports for its own rows.
internal fun observeAnyPending(vararg pending: Flow<Boolean>): Flow<Boolean> = combine(pending.toList()) { flags ->
    flags.any { it }
}.distinctUntilChanged()
