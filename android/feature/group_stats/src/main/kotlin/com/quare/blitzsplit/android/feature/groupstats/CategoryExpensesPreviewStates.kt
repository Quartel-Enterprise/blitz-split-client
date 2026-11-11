package com.quare.blitzsplit.android.feature.groupstats

import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.feature.groupstats.presentation.model.CategoryExpenseDay
import com.quare.blitzsplit.feature.groupstats.presentation.model.CategoryExpenseRow
import com.quare.blitzsplit.feature.groupstats.presentation.model.CategoryExpensesUiState
import com.quare.blitzsplit.feature.groupstats.presentation.model.CategoryFilterChip
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPayer
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsRangeStyle

// Why: Viagem Floripa's Food, the 28th and the 26th of October 2026, as the statistics' preview counts it.
private const val PREVIEW_DAY = 1_793_588_400_000L
private const val PREVIEW_HOUR = 3_600_000L

internal fun createPreviewCategoryExpensesState(): CategoryExpensesUiState {
    val tree = CategoryTree.appOnly
    val food = tree.resolve(AppCategory.FOOD.key)
    val restaurant = tree.branchOf(AppCategory.FOOD.key)?.subcategories?.firstOrNull() ?: food
    val dinner = CategoryExpenseRow(
        id = "dinner",
        description = "Jantar no Ostradamus",
        spentAt = PREVIEW_DAY + 21 * PREVIEW_HOUR,
        hasSpentTime = true,
        payers = listOf(
            StatsPayer(
                name = "Bruno",
                isMe = false,
            ),
        ),
        totalMinor = 64_200L,
        myBalanceMinor = -11_800L,
        category = restaurant,
        isPending = false,
    )
    val market = CategoryExpenseRow(
        id = "market",
        description = "Mercado",
        spentAt = PREVIEW_DAY - 2 * 24 * PREVIEW_HOUR + 10 * PREVIEW_HOUR,
        hasSpentTime = true,
        payers = listOf(
            StatsPayer(
                name = null,
                isMe = true,
            ),
        ),
        totalMinor = 19_800L,
        myBalanceMinor = 15_840L,
        category = food,
        isPending = true,
    )
    return CategoryExpensesUiState(
        groupName = "Viagem Floripa",
        currency = GroupCurrency.BRL,
        isTrip = true,
        periodStartMillis = null,
        periodEndMillis = null,
        periodRangeStyle = StatsRangeStyle.DAYS,
        chips = listOf(AppCategory.TRAVEL, AppCategory.FOOD, AppCategory.LEISURE).map {
            CategoryFilterChip(tree.resolve(it.key), it == AppCategory.FOOD)
        },
        days = listOf(
            CategoryExpenseDay(PREVIEW_DAY, RelativeDay.TODAY, listOf(dinner)),
            CategoryExpenseDay(PREVIEW_DAY - 2 * 24 * PREVIEW_HOUR, RelativeDay.OTHER, listOf(market)),
        ),
        totalMinor = 84_000L,
        expenseCount = 2,
        isFirstDownload = false,
        isLoaded = true,
        homeTab = HomeTab.GROUPS,
    )
}
