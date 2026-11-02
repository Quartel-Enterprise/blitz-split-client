package com.quare.blitzsplit.feature.billdetail.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.SplitMode

// Why: one field an edit changed, before and after; each UI writes it in its language.
sealed interface BillHistoryChange {
    data class Amount(
        val beforeMinor: Long?,
        val afterMinor: Long?,
    ) : BillHistoryChange

    data class Description(
        val before: String?,
        val after: String?,
    ) : BillHistoryChange

    // Why: "Quando".
    data class SpentAt(
        val beforeMillis: Long?,
        val afterMillis: Long?,
    ) : BillHistoryChange

    // Why: "Bar → Restaurante", resolved in the state's categories; null for none ("Outros").
    data class Category(
        val beforeId: String?,
        val afterId: String?,
    ) : BillHistoryChange

    // Why: "Bruno → Ana e Bruno"; null names are someone no longer in the group.
    data class Payers(
        val before: List<String?>,
        val after: List<String?>,
    ) : BillHistoryChange

    // Why: "mudou a divisão": the mode, and each part that moved.
    data class Split(
        val beforeMode: SplitMode?,
        val afterMode: SplitMode?,
        val parts: List<BillHistoryPart>,
    ) : BillHistoryChange
}
