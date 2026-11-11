package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.navigation.Route

internal fun toRouteName(route: Route): String = when (route) {
    Route.Welcome -> "welcome"

    Route.ProfileSetup -> "profile-setup"

    Route.Restore -> "restore"

    Route.Home -> "home"

    Route.Groups -> "groups"

    Route.Search -> "search"

    Route.CreateGroup -> "create-group"

    is Route.Invite -> "invite"

    is Route.GroupDetail -> "group-detail"

    is Route.RegisterPayment -> "register-payment"

    Route.PickPayment -> "pick-payment"

    is Route.GlobalBalances -> "global-balances"

    is Route.Charge -> "charge"

    is Route.Activity -> "activity"

    is Route.SettleUp -> "settle-up"

    Route.Contacts -> "contacts"

    is Route.ContactProfile -> "contact-profile"

    Route.ContactsAgenda -> "contacts-agenda"

    is Route.ContactInvite -> "contact-invite"

    is Route.SplitBill -> "split-bill"

    is Route.BillDetail -> "bill-detail"

    is Route.ShareBill -> "share-bill"

    is Route.SharedBill -> "shared-bill"

    is Route.ProfileLink -> "profile-link"

    is Route.EditGroup -> "edit-group"

    is Route.GroupPhoto -> "group-photo"

    is Route.DefaultSplit -> "default-split"

    is Route.SimplifyDebtsInfo -> "simplify-debts-info"

    is Route.GroupStats -> "group-stats"

    is Route.CategoryExpenses -> "category-expenses"

    is Route.JoinInvite -> "join-invite"

    is Route.JoinPick -> "join-pick"

    is Route.JoinWithLink -> "join-with-link"

    Route.Profile -> "profile"

    Route.EditProfile -> "edit-profile"

    Route.ProfilePhoto -> "profile-photo"

    // Why: never pushed on the web, which always serves the latest version; named only so the mapping stays whole.
    is Route.InAppUpdate -> "in-app-update"

    is Route.AppDownload -> "app-download"

    Route.DeleteAccount -> "delete-account"

    Route.NotificationSettings -> "notification-settings"

    Route.CategoryMap -> "category-map"

    // Why: never on the web's stack, where Profile's footer opens the site's page.
    is Route.LegalDocument -> "legal-document"

    is Route.PaymentKey -> "payment-key"
}
