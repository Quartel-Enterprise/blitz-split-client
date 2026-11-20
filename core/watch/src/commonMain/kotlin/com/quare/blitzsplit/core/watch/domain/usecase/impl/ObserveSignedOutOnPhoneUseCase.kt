package com.quare.blitzsplit.core.watch.domain.usecase.impl

import com.quare.blitzsplit.core.watch.domain.model.WatchSignOutNotice
import com.quare.blitzsplit.core.watch.domain.usecase.ObserveSignedOutOnPhone
import kotlinx.coroutines.flow.Flow

class ObserveSignedOutOnPhoneUseCase(
    private val notice: WatchSignOutNotice,
) : ObserveSignedOutOnPhone {
    override fun invoke(): Flow<Boolean> = notice.isShown
}
