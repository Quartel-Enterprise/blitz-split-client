package com.quare.blitzsplit.core.provider.analytics.screen

import com.quare.blitzsplit.core.navigation.Route

/*
 * Why: each route's screen name in Analytics (docs/analytics/README.md), stable across versions and the same on every
 * UI, since one back stack drives them all. The when has no else, so a new route can't ship without its name. Params
 * say what kind of screen it is, which stays true each time it comes back on top, never how it was opened (a tap's
 * event says that) nor whose: no id, code, name nor key of the route leaves the device.
 */
internal fun Route.toScreenView(): ScreenView = when (this) {
    Route.Welcome -> ScreenView("welcome", emptyMap())
    Route.ProfileSetup -> ScreenView("profile_setup", emptyMap())
    Route.Restore -> ScreenView("restore", emptyMap())
    Route.Home -> ScreenView("home", emptyMap())
    Route.Groups -> ScreenView("groups", emptyMap())
    Route.Search -> ScreenView("search", emptyMap())
    Route.Profile -> ScreenView("profile", emptyMap())
    Route.CreateGroup -> ScreenView("create_group", emptyMap())
    Route.EditProfile -> ScreenView("edit_profile", emptyMap())
    Route.ProfilePhoto -> ScreenView("profile_photo", emptyMap())
    is Route.GroupPhoto -> ScreenView("group_photo", emptyMap())
    is Route.InAppUpdate -> ScreenView("in_app_update", emptyMap())
    is Route.AppDownload -> ScreenView("app_download", emptyMap())
    Route.DeleteAccount -> ScreenView("delete_account", emptyMap())
    Route.NotificationSettings -> ScreenView("notification_settings", emptyMap())
    Route.CategoryMap -> ScreenView("category_map", emptyMap())
    is Route.LegalDocument -> ScreenView("legal_document", mapOf("page" to page))
    is Route.Invite -> ScreenView("invite", mapOf("is_new_group" to isNewGroup))
    is Route.GroupDetail -> ScreenView("group_detail", emptyMap())
    is Route.GroupStats -> ScreenView("group_stats", emptyMap())
    is Route.CategoryExpenses -> ScreenView("category_expenses", mapOf("has_period" to (periodStartMillis != null)))
    is Route.EditGroup -> ScreenView("edit_group", emptyMap())
    is Route.DefaultSplit -> ScreenView("default_split", emptyMap())
    is Route.SimplifyDebtsInfo -> ScreenView("simplify_debts_info", emptyMap())
    is Route.SplitBill -> ScreenView("split_bill", mapOf("is_edit" to (expenseId != null)))
    is Route.BillDetail -> ScreenView("bill_detail", emptyMap())
    is Route.ShareBill -> ScreenView("share_bill", emptyMap())
    is Route.SharedBill -> ScreenView("shared_bill", emptyMap())
    is Route.ProfileLink -> ScreenView("profile_link", emptyMap())
    is Route.RegisterPayment -> ScreenView("register_payment", mapOf("offers_keys" to offersReceiverKeys))
    Route.PickPayment -> ScreenView("pick_payment", emptyMap())
    Route.AllExpenses -> ScreenView("all_expenses", emptyMap())
    is Route.GlobalBalances -> ScreenView("global_balances", emptyMap())
    is Route.Charge -> ScreenView("charge", emptyMap())
    Route.Contacts -> ScreenView("contacts", emptyMap())
    is Route.ContactProfile -> ScreenView("contact_profile", emptyMap())
    Route.ContactsAgenda -> ScreenView("contacts_agenda", emptyMap())
    is Route.ContactInvite -> ScreenView("contact_invite", mapOf("is_guest" to (personKey != null)))
    is Route.Activity -> ScreenView("activity", activityParamsOf(this))
    is Route.SettleUp -> ScreenView("settle_up", emptyMap())
    is Route.JoinInvite -> ScreenView("join_invite", mapOf("is_personal" to (memberCode != null)))
    is Route.JoinWithLink -> ScreenView("join_with_link", emptyMap())
    is Route.JoinPick -> ScreenView("join_pick", emptyMap())
    is Route.PaymentKey -> ScreenView("payment_key", mapOf("method" to method, "is_new" to (keyId == null)))
}

// Why: one person's history, or Início's "Ver tudo" over "Últimas despesas" (its bills).
private fun activityParamsOf(route: Route.Activity): Map<String, Any> = mapOf(
    "is_person" to (route.personKey != null),
    "is_bills_only" to route.showsBillsOnly,
)
