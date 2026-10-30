package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.repository.DeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveDeletedBillNotice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObserveDeletedBillNoticeUseCase(
    private val repository: DeletedBillRepository,
) : ObserveDeletedBillNotice {
    override fun invoke(groupId: String): Flow<Boolean> = repository
        .observeNoticeGroupId()
        .map { it == groupId }
        .distinctUntilChanged()
}
