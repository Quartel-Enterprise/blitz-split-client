package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.format.categoryName
import com.quare.blitzsplit.android.designsystem.format.categoryPath
import com.quare.blitzsplit.core.expenses.domain.model.Category
import com.quare.blitzsplit.feature.splitbill.presentation.model.CategoryHint
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState

// Why: "Alimentação › Mercado", the bill's category with its main one.
@Composable
internal fun billCategoryPathOf(state: SplitBillUiState): String = categoryPath(state.category, state.categoryParent)

// Why: "Alimentação › Mercado" of any category of the bill's group.
@Composable
internal fun categoryPathIn(
    state: SplitBillUiState,
    category: Category,
): String = categoryPath(category, category.parentId?.let { state.categories.findOrNull(it) })

// Why: the line under the description, what chose the bill's category (7.1, 7.1l to 7.1n).
@Composable
internal fun categoryHintOf(state: SplitBillUiState): String {
    val path = billCategoryPathOf(state)
    val match = state.categoryMatches.firstOrNull()
    val mapCategory = state.mapCategory
    return when (state.categoryHint) {
        CategoryHint.BY_TERM -> stringResource(R.string.split_bill_category_by_term, path, match?.term.orEmpty())

        CategoryHint.BY_LONGEST_TERM -> stringResource(
            R.string.split_bill_category_by_longest,
            state.categoryMatches.size,
            match?.term.orEmpty(),
            path,
        )

        CategoryHint.NO_TERM -> stringResource(R.string.split_bill_category_no_term)

        CategoryHint.CHANGED -> stringResource(
            R.string.split_bill_category_changed,
            path,
            mapCategory?.let { categoryPathIn(state, it) }.orEmpty(),
            match?.term.orEmpty(),
        )

        CategoryHint.PICKED -> stringResource(R.string.split_bill_category_picked, path)
    }
}

// Why: "Criada por Ana", the member who created a group category, or nothing when they left the group.
internal fun creatorNameOrNull(
    state: SplitBillUiState,
    category: Category,
): String? = state.people.firstOrNull { it.id == category.createdByMemberId }?.name

@Composable
internal fun categoryNameOf(category: Category): String = categoryName(category)
