package com.quare.blitzsplit.core.navigation

import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.GroupCurrency
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

    /*
     * Why: "Mapa de categorias" (flow 9), from Perfil › Preferências and from "Categoria" (9.0); the Mac keeps it in
     * its Ajustes.
     */
    data object CategoryMap : Route

    // Why: "Convidar", the group's link and QR code. It takes the place of "Novo grupo" once the group is created.
    data class Invite(
        val groupId: String,
        // Why: right after "Novo grupo", rather than from the group; only the latter offers "Trocar link".
        val isNewGroup: Boolean,
    ) : Route

    // Why: "Despesas do grupo", the group's own screen, where a group tapped on the home opens.
    data class GroupDetail(
        val groupId: String,
    ) : Route

    /*
     * Why: "Estatísticas do grupo" (6.6), from the chart button on the group's bar: what the group spent and who
     * spent it, from the bills on the device. Back goes to the tab it was opened from.
     */
    data class GroupStats(
        val groupId: String,
    ) : Route

    // Why: "Editar grupo", from the group's ⋮ menu: a sheet over the group on iOS and macOS, a dialog on the web.
    data class EditGroup(
        val groupId: String,
    ) : Route

    /*
     * Why: "Divisão padrão" (6.7), from Editar grupo's row: how the group's new bills split. Back and Salvar go back to
     * Editar grupo, which stays under it (pushed inside its sheet on iOS and macOS, in its dialog's place on the web).
     */
    data class DefaultSplit(
        val groupId: String,
    ) : Route

    // Why: "Rachar conta", a new bill in the group, or "Editar conta" for expenseId, with "Dividir" drawn over it.
    data class SplitBill(
        val groupId: String,
        val expenseId: String?,
        // Why: from the home's "Rachar conta" (7.1i), which asks "Rachar em qual grupo?" first, groupId marked.
        val picksGroupFirst: Boolean,
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
     * Why: the home's second tab, "Saldos" (flow 8): each person once, netted across every group. Over the groups, so
     * the Grupos tab and back return to them; "A receber" and "A pagar" open it already filtered.
     */
    data class GlobalBalances(
        val filter: BalancesFilter,
    ) : Route

    /*
     * Why: "Acertar" or "Recebi" of someone in the global Saldos: Registrar pagamento for the whole net with them in
     * that currency, one payment in each group so each group closes right. personKey is PersonBalance.key.
     */
    data class SettleUp(
        val personKey: String,
        val currency: GroupCurrency,
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
