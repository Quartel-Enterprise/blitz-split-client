package com.quare.blitzsplit.shared.storescreenshots

import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import kotlinx.coroutines.flow.first

/*
 * Why: the screen each screenshot opens on, by the code the captures launch the app with (the iOS UI test,
 * capture-mac.sh and the web's capture-web.mjs, from scripts/store-screenshots/screens.tsv). The routes go on the back
 * stack over Início, as a person would have tapped their way there; Items is the dinner's bill, on which the Apple
 * bridge then opens "Bill items" (BlitzSplitApple.createSplitBillViewModel). The web has no such step, so it stops on
 * the bill.
 */
internal enum class StoreScreenshotsScreen(
    val code: String,
) {
    GROUP("group"),
    ITEMS("items"),
    RECEIPT("receipt"),
    BALANCES("balances"),
    INVITE("invite"),
    STATISTICS("statistics"),
    HOME("home"),
    PAYERS("payers"),
    ACTIVITY("activity"),
    LIGHT_THEME("light"),
    ;

    fun routesOf(trip: SeededTrip): List<Route> {
        val group = Route.GroupDetail(
            groupId = trip.groupId,
            joinedAs = null,
            tab = GroupTab.EXPENSES,
            isOverBill = false,
        )
        return when (this) {
            GROUP, LIGHT_THEME -> listOf(group)

            ITEMS -> listOf(group, Route.SplitBill.createEditing(trip.groupId, trip.dinnerId))

            RECEIPT -> listOf(
                Route.SplitBill(
                    groupId = trip.groupId,
                    expenseId = null,
                    picksGroupFirst = false,
                    scansReceipt = true,
                    duplicatesExpenseId = null,
                ),
            )

            BALANCES -> listOf(group.copy(tab = GroupTab.BALANCES))

            INVITE -> listOf(
                group,
                Route.Invite(
                    groupId = trip.groupId,
                    isNewGroup = false,
                ),
            )

            STATISTICS -> listOf(group, Route.GroupStats(groupId = trip.groupId))

            HOME -> emptyList()

            PAYERS -> listOf(
                group,
                Route.BillDetail(
                    groupId = trip.groupId,
                    expenseId = trip.foreignBillId,
                    focusCommentId = null,
                ),
            )

            ACTIVITY -> listOf(Route.Activity.all)
        }
    }

    companion object {
        /*
         * Why: the app puts Início at the root once it read the profile; the screen goes over it after that, on every
         * platform the same way.
         */
        suspend fun Navigator.openOverHome(routes: List<Route>) {
            backStack.first { it.firstOrNull() == Route.Home }
            routes.forEach(::navigate)
        }

        fun fromCodeOrNull(code: String?): StoreScreenshotsScreen? = entries.firstOrNull { it.code == code }
    }
}
