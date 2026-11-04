package com.quare.blitzsplit.core.navigation

import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.HomeTab

/*
 * Why: what a push notification opens, the link the server sends beside each message,
 * https://blitzsplit.com/n?k=<kind>&e=<event>&g=<group>&t=<target>. Atividade's open what the item's tap there opens,
 * over Atividade, the notification center; the settle-up reminders over Grupos, or Saldos for several groups.
 */
data class PushLink(
    val kind: String,
    val eventId: String?,
    val groupId: String?,
    val targetId: String?,
) {
    val homeTab: HomeTab
        get() = when (kind) {
            SETTLE_REMINDER -> HomeTab.GROUPS
            SETTLE_REMINDERS -> HomeTab.BALANCES
            else -> HomeTab.ACTIVITY
        }

    // Why: what opens over the tab, in order; nothing without a group, whose kind the tab already shows.
    val routesAbove: List<Route>
        get() {
            val groupId = groupId ?: return emptyList()
            return when (kind) {
                BILL_CREATED, BILL_EDITED, COMMENT_ADDED -> listOfNotNull(
                    buildGroupRoute(groupId, GroupTab.EXPENSES),
                    targetId?.let { Route.BillDetail(groupId, it, focusCommentId = null) },
                )

                CHARGE_SENT, SETTLE_REMINDER -> listOf(buildGroupRoute(groupId, GroupTab.BALANCES))

                ADDED_TO_GROUP, MEMBER_JOINED -> listOf(buildGroupRoute(groupId, GroupTab.MEMBERS))

                /*
                 * Why: the payments show in Despesas, and a kind a newer server adds opens the group. Unlike its item
                 * in Atividade, a deleted bill opens Despesas too: the notification already said who deleted it.
                 * "Simplificar dívidas" turned on or off (13.7) opens Saldos, as its item does.
                 */
                else -> listOf(
                    buildGroupRoute(
                        groupId = groupId,
                        tab = if (kind.startsWith(SIMPLIFY_KIND_PREFIX)) GroupTab.BALANCES else GroupTab.EXPENSES,
                    ),
                )
            }
        }

    private fun buildGroupRoute(
        groupId: String,
        tab: GroupTab,
    ): Route = Route.GroupDetail(
        groupId,
        joinedAs = null,
        tab = tab,
    )

    companion object {
        private const val BILL_CREATED = "bill_created"
        private const val BILL_EDITED = "bill_edited"
        private const val COMMENT_ADDED = "comment_added"
        private const val CHARGE_SENT = "charge_sent"
        private const val ADDED_TO_GROUP = "added_to_group"
        private const val MEMBER_JOINED = "member_joined"
        private const val SETTLE_REMINDER = "settle_reminder"
        private const val SETTLE_REMINDERS = "settle_reminders"

        // Why: debts_simplified_* and debts_unsimplified_*, who now pays you, whom you now pay or neither (13.7).
        private const val SIMPLIFY_KIND_PREFIX = "debts_"

        // Why: any host, as an invite's, since the web serves the path wherever it is hosted.
        private const val LINK_PATTERN = "^https?://[^/?#]+/n/?\\?([^#]*)(#.*)?$"

        // Why: null for any other URL, and for a link without its kind.
        fun of(url: String): PushLink? {
            val query = Regex(LINK_PATTERN).matchEntire(url.trim())?.groupValues?.get(1) ?: return null
            val parameters = query
                .split('&')
                .mapNotNull { pair -> pair.split('=', limit = 2).takeIf { it.size == 2 } }
                .associate { (name, value) -> name to value }
                .filterValues { it.isNotEmpty() }
            return PushLink(
                kind = parameters["k"] ?: return null,
                eventId = parameters["e"],
                groupId = parameters["g"],
                targetId = parameters["t"],
            )
        }
    }
}
