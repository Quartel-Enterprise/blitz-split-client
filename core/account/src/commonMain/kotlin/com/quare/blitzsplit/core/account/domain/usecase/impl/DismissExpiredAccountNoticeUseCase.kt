package com.quare.blitzsplit.core.account.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.account.domain.usecase.DismissExpiredAccountNotice

class DismissExpiredAccountNoticeUseCase(
    private val repository: AccountRepository,
) : DismissExpiredAccountNotice {
    override suspend fun invoke() {
        repository.dismissExpiredNotice()
    }
}
