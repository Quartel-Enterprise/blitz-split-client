package com.quare.blitzsplit.core.navigation.analytics

import com.quare.blitzsplit.core.navigation.Route

/*
 * Why: each route's screen name in Analytics (docs/analytics/README.md), stable across versions and the same on every
 * UI, since one back stack drives them all. The when has no else, so a new route can't ship without its name. Params
 * say what kind of visit it was, never whose: no id, code, name nor key of the route leaves the device.
 */
internal fun Route.toScreenView(): ScreenView = when (this) {
    Route.Welcome -> ScreenView("welcome")

    Route.ProfileSetup -> ScreenView("profile_setup")

    Route.Restore -> ScreenView("restore")

    Route.Home -> ScreenView("home")

    Route.Groups -> ScreenView("groups")

    Route.Search -> ScreenView("search")

    Route.Profile -> ScreenView("profile")

    Route.CreateGroup -> ScreenView("create_group")

    Route.EditProfile -> ScreenView("edit_profile")

    Route.ProfilePhoto -> ScreenView("profile_photo")

    Route.DeleteAccount -> ScreenView("delete_account")

    Route.NotificationSettings -> ScreenView("notification_settings")

    Route.CategoryMap -> ScreenView("category_map")

    is Route.Invite -> ScreenView("invite", mapOf("is_new_group" to isNewGroup))

    is Route.GroupDetail -> ScreenView("group_detail", mapOf("tab" to tab, "is_join" to (joinedAs != null)))

    is Route.GroupStats -> ScreenView("group_stats")

    is Route.EditGroup -> ScreenView("edit_group")

    is Route.DefaultSplit -> ScreenView("default_split")

    is Route.SplitBill -> ScreenView(
        name = "split_bill",
        params = mapOf(
            "is_edit" to (expenseId != null),
            "picks_group_first" to picksGroupFirst,
            "scans_receipt" to scansReceipt,
        ),
    )

    is Route.BillDetail -> ScreenView("bill_detail", mapOf("is_comment" to (focusCommentId != null)))

    is Route.ShareBill -> ScreenView("share_bill")

    is Route.SharedBill -> ScreenView("shared_bill", mapOf("is_comment" to (commentId != null)))

    is Route.RegisterPayment -> ScreenView("register_payment", mapOf("offers_keys" to offersReceiverKeys))

    Route.PickPayment -> ScreenView("pick_payment")

    is Route.GlobalBalances -> ScreenView(
        name = "global_balances",
        params = mapOf("filter" to filter, "is_charge" to (charge != null)),
    )

    Route.Contacts -> ScreenView("contacts")

    is Route.ContactProfile -> ScreenView("contact_profile")

    Route.ContactsAgenda -> ScreenView("contacts_agenda")

    is Route.ContactInvite -> ScreenView("contact_invite", mapOf("is_guest" to (personKey != null)))

    is Route.Activity -> ScreenView("activity", mapOf("is_person" to (personKey != null)))

    is Route.SettleUp -> ScreenView("settle_up")

    is Route.JoinInvite -> ScreenView("join_invite", mapOf("is_personal" to (memberCode != null)))

    is Route.JoinWithLink -> ScreenView("join_with_link", mapOf("is_retry" to (skippedCode != null)))

    is Route.JoinPick -> ScreenView("join_pick")

    is Route.PaymentKey -> ScreenView("payment_key", mapOf("method" to method, "is_new" to (keyId == null)))
}
