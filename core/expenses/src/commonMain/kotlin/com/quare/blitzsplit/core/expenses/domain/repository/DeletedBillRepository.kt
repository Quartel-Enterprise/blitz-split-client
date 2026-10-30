package com.quare.blitzsplit.core.expenses.domain.repository

import kotlinx.coroutines.flow.Flow

/*
 * Why: what this device knows of deleted bills while the app runs: which ones it deleted itself, and which group's
 * Despesas do grupo still owes "Esta conta foi apagada" for a bill someone else deleted while it was open here.
 */
interface DeletedBillRepository {
    fun markDeletedHere(expenseId: String)

    // Why: the delete failed, so the bill is still there.
    fun unmarkDeletedHere(expenseId: String)

    fun isDeletedHere(expenseId: String): Boolean

    // Why: the group owed the notice, null when none is.
    fun observeNoticeGroupId(): Flow<String?>

    fun postNotice(groupId: String)

    // Why: only that group's own notice, so another group's screen shown meanwhile leaves it waiting.
    fun consumeNotice(groupId: String)
}
