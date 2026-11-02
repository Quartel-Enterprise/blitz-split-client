package com.quare.blitzsplit.core.activity.domain.usecase

import com.quare.blitzsplit.core.activity.domain.model.ActivityEvent
import kotlinx.coroutines.flow.Flow

// Why: "Histórico" of a bill in Conta (7.3): its events but the comments, which show in their own section.
fun interface ObserveBillHistory {
    operator fun invoke(billId: String): Flow<List<ActivityEvent>>
}
