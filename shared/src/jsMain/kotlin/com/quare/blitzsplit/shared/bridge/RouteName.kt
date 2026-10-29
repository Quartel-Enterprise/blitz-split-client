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
    Route.Profile -> "profile"
    Route.EditProfile -> "edit-profile"
    Route.DeleteAccount -> "delete-account"
    Route.NotificationSettings -> "notification-settings"
    is Route.PaymentKey -> "payment-key"
}
