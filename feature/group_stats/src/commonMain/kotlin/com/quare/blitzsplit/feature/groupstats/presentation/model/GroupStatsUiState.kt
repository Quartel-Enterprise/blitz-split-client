package com.quare.blitzsplit.feature.groupstats.presentation.model

import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.core.model.StatsChartType
import com.quare.blitzsplit.core.model.StatsGrain

data class GroupStatsUiState(
    val groupName: String,
    val currency: GroupCurrency,
    // Why: "Toda a viagem" and "Ver toda a viagem" in a trip, "Todo o período" in any other group.
    val isTrip: Boolean,
    val period: StatsPeriod,
    // Why: the first instant of the period's first and last days.
    val periodStartMillis: Long,
    val periodEndMillis: Long,
    val periodRangeStyle: StatsRangeStyle,
    // Why: the period menu, in the order the UIs list it.
    val periodOptions: List<StatsPeriodOption>,
    /*
     * Why: the first instant of the first and last days with a bill, which "Período sem despesas" names ("Todas as
     * despesas deste grupo são de 24 a 29 de outubro."); null while the group has none.
     */
    val allStartMillis: Long?,
    val allEndMillis: Long?,
    val allRangeStyle: StatsRangeStyle,
    // Why: "Total gasto": the period's bills, never the payments recorded in Saldos.
    val totalMinor: Long,
    val expenseCount: Int,
    // Why: "7 despesas, 1 na fila", with the orange dot.
    val pendingCount: Int,
    // Why: everyone in the group, who hasn't joined yet included, as the average counts them.
    val memberCount: Int,
    val averagePerPersonMinor: Long,
    // Why: "Sua parte", the sum of this person's parts; null without a member of this device.
    val myShareMinor: Long?,
    // Why: "19% do total", rounded.
    val mySharePercent: Int,
    // Why: "Você pagou", what this person paid then, and in how many bills.
    val myPaidMinor: Long?,
    val myPaidCount: Int,
    val peopleView: StatsPeopleView,
    // Why: the largest amount first.
    val people: List<StatsPersonRow>,
    val chartType: StatsChartType,
    val grain: StatsGrain,
    val grains: List<StatsGrainOption>,
    val bars: List<StatsBar>,
    val grainNotes: List<StatsGrainNote>,
    val biggestExpense: StatsBiggestExpense?,
    // Why: "Por categoria" and the pie, the largest first; a bill without one counts in "Outros".
    val categories: List<StatsCategoryRow>,
    // Why: nothing is drawn until Room is read.
    val isLoaded: Boolean,
    // Why: the home tab the screen was opened from, which Android's rail beside it selects.
    val homeTab: HomeTab,
    // Why: Atividade's unread badge on Android's rail beside the screen.
    val activityUnreadCount: Int,
) {
    // Why: "Nada para mostrar ainda", with "Rachar conta", while the group has no bill at all.
    val isGroupEmpty: Boolean
        get() = isLoaded && allStartMillis == null

    // Why: "Nenhuma despesa em setembro", with "Ver toda a viagem".
    val isPeriodEmpty: Boolean
        get() = isLoaded && allStartMillis != null && expenseCount == 0

    val showsStats: Boolean
        get() = isLoaded && expenseCount > 0

    // Why: the period has nothing to pick from in a group without bills.
    val showsPeriodPicker: Boolean
        get() = isLoaded && allStartMillis != null

    // Why: the pie's center names the largest category.
    val topCategory: StatsCategoryRow?
        get() = categories.firstOrNull()

    // Why: "Dia mais caro: 28 de out, R$ 1.458,00".
    val peak: StatsBar?
        get() = bars.firstOrNull { it.isPeak }

    // Why: with only some labels, a label names a week by its month ("jan", "mar").
    val hasSparseLabels: Boolean
        get() = bars.any { !it.showsLabel }
}
