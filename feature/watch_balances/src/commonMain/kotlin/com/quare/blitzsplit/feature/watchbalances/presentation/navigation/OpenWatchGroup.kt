package com.quare.blitzsplit.feature.watchbalances.presentation.navigation

import com.quare.blitzsplit.core.expenses.domain.usecase.ObservePeopleBalances
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.watchbalances.presentation.mapper.WatchBalancesMapper
import kotlinx.coroutines.flow.first

/*
 * Why: a push's "View group" or tap on the watch (15.7). The push names the group but not its currency, so it opens the
 * group in the currency where your balance in it is largest, over the balances; a group with nothing open with you
 * opens the balances only. A watch not connected yet stays on its connect screens.
 */
class OpenWatchGroup(
    private val navigator: Navigator,
    private val observePeopleBalances: ObservePeopleBalances,
) {
    suspend operator fun invoke(groupId: String?) {
        if (navigator.backStack.value.firstOrNull() == Route.WatchConnect) return
        val row = groupId?.let { id ->
            WatchBalancesMapper
                .groupsOf(observePeopleBalances().first(), money = emptyMap())
                .firstOrNull { it.groupId == id }
        }
        navigator.replaceAll(Route.WatchBalances)
        if (row != null) navigator.navigate(Route.WatchGroup(row.groupId, row.currency))
    }
}
