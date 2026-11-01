package com.quare.blitzsplit.core.activity.domain.usecase

import com.quare.blitzsplit.core.activity.domain.model.ChargeSent

// Why: "Cobrar" sent, one event per group the charge covers, which the server can't see happen.
fun interface RecordCharges {
    suspend operator fun invoke(charges: List<ChargeSent>): Result<Unit>
}
