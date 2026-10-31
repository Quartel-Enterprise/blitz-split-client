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

    // Why: "Despesas do grupo", the group's own screen, where a group tapped on the home opens.
    data class GroupDetail(
        val groupId: String,
    ) : Route

    // Why: "Editar grupo", from the group's ⋮ menu: a sheet over the group on iOS and macOS, a dialog on the web.
    data class EditGroup(
        val groupId: String,
    ) : Route

    // Why: "Rachar conta", a new bill in the group, or "Editar conta" for expenseId, with "Dividir" drawn over it.
    data class SplitBill(
        val groupId: String,
        val expenseId: String?,
    ) : Route

    // Why: "Conta", one bill of the group: what it was, who paid and how it splits.
    data class BillDetail(
        val groupId: String,
        val expenseId: String,
    ) : Route

    // Why: "Saldos", who owes whom in the group: the group's tab next to Despesas and Membros, which takes its place.
    data class Balances(
        val groupId: String,
    ) : Route

    /*
     * Why: "Registrar pagamento", a payment from one member to another made outside the app: Recebi, Já pagou or
     * Acertar in Saldos, or Acertar on the group's balance card. A sheet or a dialog over the group.
     */
    data class RegisterPayment(
        val groupId: String,
        val fromMemberId: String,
        val toMemberId: String,
    ) : Route

    /*
     * Why: "Membros", who is in the group and who joined: the group's tab next to Despesas, which takes its place.
     * joinedAs is the name someone just joined the group as, for "Você entrou como Bruno".
     */
    data class Members(
        val groupId: String,
        val joinedAs: String?,
    ) : Route

    /*
     * Why: an invite link opened, the group before joining it ("Convite"). A group already joined opens instead. A
     * personal link carries the member's code, which joins straight in as that member, and the code of whoever
     * shared it.
     */
    data class JoinInvite(
        val code: String,
        val memberCode: String?,
        val inviterCode: String?,
    ) : Route

    /*
     * Why: "Entrar com convite", a QR read by the camera or a link pasted, from the home or an invite no longer valid.
     * From "Escanear outro QR", the camera skips the QR of the invite that just failed, which may still be in front of
     * it, so only another QR or a pasted link opens.
     */
    data class JoinWithLink(
        val skippedCode: String?,
        val skippedMemberCode: String?,
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
