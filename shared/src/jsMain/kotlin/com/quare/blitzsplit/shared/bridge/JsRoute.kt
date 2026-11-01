@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.navigation.Route

/*
 * Why: a back stack entry as React sees it, its name plus the arguments of the routes that carry them. The
 * route itself stays inside, so a Presenter opens its ViewModel with the very route that was pushed.
 */
class JsRoute internal constructor(
    internal val route: Route,
) {
    val name: String = toRouteName(route)

    // Why: the PaymentMethod's name for "payment-key", null for every other route.
    val paymentMethod: String? = (route as? Route.PaymentKey)?.method?.name

    // Why: the key to edit for "payment-key", null for a new key and for every other route.
    val paymentKeyId: String? = (route as? Route.PaymentKey)?.keyId

    /*
     * Why: the group of "invite", "members", "group-detail", "balances", "register-payment", "split-bill",
     * "bill-detail" and "edit-group", null for every other route.
     */
    val groupId: String? = when (route) {
        is Route.Invite -> route.groupId
        is Route.Members -> route.groupId
        is Route.GroupDetail -> route.groupId
        is Route.Balances -> route.groupId
        is Route.RegisterPayment -> route.groupId
        is Route.SplitBill -> route.groupId
        is Route.BillDetail -> route.groupId
        is Route.EditGroup -> route.groupId
        else -> null
    }

    // Why: the bill of "bill-detail", and of "split-bill" when it edits one; null for every other route.
    val expenseId: String? = when (route) {
        is Route.SplitBill -> route.expenseId
        is Route.BillDetail -> route.expenseId
        else -> null
    }

    // Why: who paid and who received in "register-payment", null for every other route.
    val fromMemberId: String? = (route as? Route.RegisterPayment)?.fromMemberId
    val toMemberId: String? = (route as? Route.RegisterPayment)?.toMemberId

    // Why: the person and currency of "settle-up", which together key its dialog; null for every other route.
    val personKey: String? = (route as? Route.SettleUp)?.personKey
    val currency: String? = (route as? Route.SettleUp)?.currency?.code

    // Why: the invite code of "join-invite" and "join-pick", null for every other route.
    val inviteCode: String? = when (route) {
        is Route.JoinInvite -> route.code
        is Route.JoinPick -> route.code
        else -> null
    }

    // Why: the member a personal link of "join-invite" joins as, null for the group's link and every other route.
    val inviteMemberCode: String? = (route as? Route.JoinInvite)?.memberCode
}
