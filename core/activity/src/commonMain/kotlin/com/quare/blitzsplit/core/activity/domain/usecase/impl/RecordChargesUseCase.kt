package com.quare.blitzsplit.core.activity.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccountStatus
import com.quare.blitzsplit.core.activity.domain.model.ChargeSent
import com.quare.blitzsplit.core.activity.domain.repository.ActivityRepository
import com.quare.blitzsplit.core.activity.domain.usecase.RecordCharges
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import kotlinx.coroutines.flow.first

// Why: saved on the device first, offline too, as the user's own event: read from the start, queued until it goes up.
class RecordChargesUseCase(
    private val repository: ActivityRepository,
    private val observeAccountStatus: ObserveAccountStatus,
    private val currentTimeMillis: CurrentTimeMillis,
) : RecordCharges {
    override suspend fun invoke(charges: List<ChargeSent>): Result<Unit> {
        if (charges.isEmpty()) return Result.success(Unit)
        return repository.saveCharges(
            charges = charges,
            actorUserId = observeAccountStatus().first().userIdOrNull(),
            sentAt = currentTimeMillis(),
        )
    }
}
