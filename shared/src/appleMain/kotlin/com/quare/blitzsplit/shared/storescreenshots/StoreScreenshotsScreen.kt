package com.quare.blitzsplit.shared.storescreenshots

import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.navigation.Route

/*
 * Why: the screen each App Store screenshot opens on, by the code the iOS UI tests launch the app with
 * (iosApp/UITests). The routes go on the back stack over Início, as a person would have tapped their way there; Items
 * still needs its tap on the bill's split, which the test makes.
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
        fun fromCodeOrNull(code: String?): StoreScreenshotsScreen? = entries.firstOrNull { it.code == code }
    }
}
