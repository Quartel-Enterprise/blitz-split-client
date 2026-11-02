package com.quare.blitzsplit.feature.editgroup.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.Category
import com.quare.blitzsplit.core.groups.domain.model.CategorySplit

// Why: one row of "Exceções por categoria": "Mercado" over "Igual entre todos" or "Partes, 2 para 1".
data class ExceptionRow(
    val exception: CategorySplit,
    // Why: the category it reads as, a deleted group category as its parent.
    val category: Category,
    // Why: "Igual entre todos" while everyone is in, "Igual entre 2" otherwise.
    val includedCount: Int,
    val totalCount: Int,
) {
    // Why: "3 para 1", "75% e 25%": who is in, in the group's order, a newcomer as a bill of it starts them.
    fun valuesOf(memberIds: List<String>): List<Long> {
        val split = exception.asDefault
        return memberIds.map(split::weightOf).filter { it > 0 }
    }
}
