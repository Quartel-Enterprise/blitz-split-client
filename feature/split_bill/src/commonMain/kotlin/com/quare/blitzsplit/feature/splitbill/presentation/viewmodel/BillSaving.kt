package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.BillItems
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTeachOffer
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.usecase.CreateExpense
import com.quare.blitzsplit.core.expenses.domain.usecase.UpdateExpense
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState

// Why: "Salvar" on a new bill, or on a duplicate, which is one: the form as typed, in the group it was opened for.
internal suspend fun CreateExpense.saveNew(
    state: SplitBillUiState,
    amountMinor: Long,
    payers: List<ExpensePayer>,
    shares: List<ExpenseShare>,
): Result<String> = invoke(
    groupId = state.groupId,
    description = state.description,
    amountMinor = amountMinor,
    currency = state.currency,
    payers = payers,
    splitMode = state.split.mode.savedMode,
    shares = shares,
    items = state.split.items,
    categoryId = state.categoryIdToSave,
    spentAt = state.spentAt,
)

/*
 * Why: "Salvar" on "Editar conta": the form as typed, over the bill it was opened for. A bill whose only change is the
 * person's own "Paid with" stays as it was, so the group sees no edit of it. The items are compared with the bill's
 * own, since SplitBillInputs leaves them out: an item renamed keeps everyone's share, and would otherwise be lost.
 */
internal suspend fun UpdateExpense.saveEdit(
    expenseId: String,
    groupId: String,
    state: SplitBillUiState,
    amountMinor: Long,
    payers: List<ExpensePayer>,
    shares: List<ExpenseShare>,
    savedItems: BillItems?,
): Result<Unit> = if (state.isOnlyPaidWithChanged && state.split.items == savedItems) {
    Result.success(Unit)
} else {
    invoke(
        expenseId = expenseId,
        groupId = groupId,
        description = state.description,
        amountMinor = amountMinor,
        currency = state.currency,
        payers = payers,
        splitMode = state.split.mode.savedMode,
        shares = shares,
        items = state.split.items,
        categoryId = state.categoryIdToSave,
        spentAt = state.spentAt,
    )
}

// Why: a duplicate that keeps its original's category doesn't ask "Ensinar ao mapa?" again, which the original did.
internal fun SplitBillUiState.findTeachOfferUnlessKept(
    expenseId: String,
    keptCategoryId: String?,
): CategoryTeachOffer? = teachOfferOrNull(expenseId).takeUnless { categoryId == keptCategoryId }
