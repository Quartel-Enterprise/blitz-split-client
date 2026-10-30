package com.quare.blitzsplit.core.expenses.data.repository

import com.quare.blitzsplit.core.expenses.domain.repository.DeletedBillRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// Why: nothing to keep across launches: a notice and a delete in flight only matter while the app runs.
class InMemoryDeletedBillRepository : DeletedBillRepository {
    private val deletedHere = MutableStateFlow(emptySet<String>())
    private val noticeGroupId = MutableStateFlow<String?>(null)

    override fun markDeletedHere(expenseId: String) {
        deletedHere.update { it + expenseId }
    }

    override fun unmarkDeletedHere(expenseId: String) {
        deletedHere.update { it - expenseId }
    }

    override fun isDeletedHere(expenseId: String): Boolean = expenseId in deletedHere.value

    override fun observeNoticeGroupId(): Flow<String?> = noticeGroupId

    override fun postNotice(groupId: String) {
        noticeGroupId.value = groupId
    }

    override fun consumeNotice(groupId: String) {
        noticeGroupId.update { if (it == groupId) null else it }
    }
}
