package com.quare.blitzsplit.core.activity.domain.usecase.impl

import com.quare.blitzsplit.core.activity.domain.repository.ActivityRepository
import com.quare.blitzsplit.core.activity.domain.usecase.LoadBillHistory

class LoadBillHistoryUseCase(
    private val repository: ActivityRepository,
) : LoadBillHistory {
    override suspend fun invoke(billId: String): Result<Unit> = repository.loadEventsAbout(billId)
}
