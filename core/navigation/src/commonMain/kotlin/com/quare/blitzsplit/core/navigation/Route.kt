package com.quare.blitzsplit.core.navigation

import com.quare.blitzsplit.core.model.PaymentMethod

/*
 * Why: one back stack shared by every UI. Android hands it to Navigation 3, SwiftUI binds it to
 * a NavigationStack path and React renders its last entry, so navigation logic stays in the
 * ViewModels and every platform walks the same flow.
 */
sealed interface Route {
    data object Welcome : Route

    data object ProfileSetup : Route

    data object Restore : Route

    data object Groups : Route

    data object Profile : Route

    data object EditProfile : Route

    // Why: "Excluir conta e dados", reached from the profile of a Google or Apple account.
    data object DeleteAccount : Route

    // Why: "Notificações", a screen of its own on Android and iOS; the web and macOS keep it in the profile.
    data object NotificationSettings : Route

    // Why: a new key of that method when keyId is null, otherwise that key, to edit or delete.
    data class PaymentKey(
        val method: PaymentMethod,
        val keyId: String?,
    ) : Route
}
