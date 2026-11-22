package com.quare.blitzsplit.core.watch.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.model.userIdOrNull
import com.quare.blitzsplit.core.sync.domain.usecase.SignOutWhenSynced
import com.quare.blitzsplit.core.watch.domain.model.WatchSignOutNotice
import com.quare.blitzsplit.core.watch.domain.usecase.EndWatchSessionFromPhone
import kotlinx.coroutines.flow.first

class EndWatchSessionFromPhoneUseCase(
    private val accountRepository: AccountRepository,
    private val signOutWhenSynced: SignOutWhenSynced,
    private val notice: WatchSignOutNotice,
) : EndWatchSessionFromPhone {
    override suspend fun invoke(userId: String): Boolean {
        if (accountRepository.observeStatus().first().userIdOrNull() != userId) return false
        // Why: a payment recorded offline on the watch goes up first, so the sign out never loses it.
        signOutWhenSynced()
        notice.show()
        return true
    }
}
