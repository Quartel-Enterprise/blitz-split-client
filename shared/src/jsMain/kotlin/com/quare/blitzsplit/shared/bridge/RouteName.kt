package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.navigation.Route

internal fun toRouteName(route: Route): String = when (route) {
    Route.Welcome -> "welcome"
    Route.ProfileSetup -> "profile-setup"
    Route.Restore -> "restore"
    Route.Groups -> "groups"
    Route.CreateGroup -> "create-group"
    is Route.Invite -> "invite"
    is Route.Members -> "members"
    is Route.GroupDetail -> "group-detail"
    is Route.SplitBill -> "split-bill"
    is Route.BillDetail -> "bill-detail"
    is Route.EditGroup -> "edit-group"
    is Route.JoinInvite -> "join-invite"
    is Route.JoinPick -> "join-pick"
    is Route.JoinWithLink -> "join-with-link"
    Route.Profile -> "profile"
    Route.EditProfile -> "edit-profile"
    Route.DeleteAccount -> "delete-account"
    Route.NotificationSettings -> "notification-settings"
    is Route.PaymentKey -> "payment-key"
}
