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

    // Why: "Novo grupo", a sheet over the groups on iOS, macOS and the web, a screen of its own on Android phones.
    data object CreateGroup : Route

    data object EditProfile : Route

    // Why: "Excluir conta e dados", reached from the profile of a Google or Apple account.
    data object DeleteAccount : Route

    // Why: "Notificações", a screen of its own on Android and iOS; the web and macOS keep it in the profile.
    data object NotificationSettings : Route

    // Why: "Convidar", the group's link and QR code. It takes the place of "Novo grupo" once the group is created.
    data class Invite(
        val groupId: String,
    ) : Route

    /*
     * Why: "Membros", who is in the group and who joined. The group's own screen (flow 6) will hold it as a tab; until
     * then a group opens here. joinedAs is the name someone just joined the group as, for "Você entrou como Bruno".
     */
    data class Members(
        val groupId: String,
        val joinedAs: String?,
    ) : Route

    // Why: an invite link opened, the group before joining it ("Convite"). A group already joined opens instead.
    data class JoinInvite(
        val code: String,
    ) : Route

    // Why: "Quem é você no grupo?", after "Entrar no grupo" on the invite.
    data class JoinPick(
        val code: String,
    ) : Route

    // Why: a new key of that method when keyId is null, otherwise that key, to edit or delete.
    data class PaymentKey(
        val method: PaymentMethod,
        val keyId: String?,
    ) : Route
}
