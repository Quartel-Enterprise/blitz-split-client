package com.quare.blitzsplit.core.contacts.domain.usecase.impl

import com.quare.blitzsplit.core.contacts.domain.repository.HiddenContactRepository
import com.quare.blitzsplit.core.contacts.domain.usecase.SetContactHidden
import com.quare.blitzsplit.core.utils.suspendRunCatching

class SetContactHiddenUseCase(
    private val repository: HiddenContactRepository,
) : SetContactHidden {
    override suspend fun invoke(
        personKey: String,
        isHidden: Boolean,
    ): Result<Unit> = suspendRunCatching { repository.setHidden(personKey, isHidden) }
}
