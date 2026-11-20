package com.quare.blitzsplit.core.watch.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.model.userIdOrNull
import com.quare.blitzsplit.core.watch.domain.usecase.ObserveAccountSignOuts
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.runningFold

class ObserveAccountSignOutsUseCase(
    private val accountRepository: AccountRepository,
) : ObserveAccountSignOuts {
    // Why: a user id followed by none is a sign out (or the account deleted); one user switched for another isn't.
    override fun invoke(): Flow<String> = accountRepository
        .observeStatus()
        .map { status -> status.userIdOrNull() }
        .distinctUntilChanged()
        .runningFold(
            Transition(
                previous = null,
                current = null,
            ),
        ) { last, userId -> Transition(last.current, userId) }
        .mapNotNull { transition -> transition.previous?.takeIf { transition.current == null } }

    private data class Transition(
        val previous: String?,
        val current: String?,
    )
}
