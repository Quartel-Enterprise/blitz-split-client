package com.quare.blitzsplit.core.navigation

import com.quare.blitzsplit.core.model.AppDownloadSource
import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.LegalPage
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.core.model.UpdatePromptSource

/*
 * Why: one back stack shared by every UI. Android hands it to Navigation 3, SwiftUI binds it to
 * a NavigationStack path and React renders its last entry, so navigation logic stays in the
 * ViewModels and every platform walks the same flow.
 */
sealed interface Route {
    data object Welcome : Route

    data object ProfileSetup : Route

    data object Restore : Route

    /*
     * Why: "Início" (flow 10), the home's first tab and where the app opens once there is a profile: the root of the
     * back stack under every other tab of the home.
     */
    data object Home : Route

    // Why: "Grupos", the home's second tab, right above Início.
    data object Groups : Route

    /*
     * Why: "Buscar" (5.2) opened from Início, Saldos, Contatos or Atividade, over that tab: the groups, people and bills
     * on the device. Grupos opens the same search in its own place instead, beside its list where there is room.
     */
    data object Search : Route

    data object Profile : Route

    // Why: "Novo grupo", a sheet over the groups on iOS, macOS and the web, a screen of its own on Android phones.
    data object CreateGroup : Route

    data object EditProfile : Route

    /*
     * Why: the profile's avatar tapped (4.1): the photo, or the initial, big over the profile, with shortcuts that
     * change the photo at once, as Bible Planner's.
     */
    data object ProfilePhoto : Route

    /*
     * Why: "Atualização disponível" over wherever the app is, on iOS and macOS only: the App Store's newer version and
     * "Abrir na App Store". Android hands the update to Google Play's own flow, and the web always serves the latest.
     */
    data class InAppUpdate(
        // Why: the App Store's version, shown as "Versão 2.1.0"; null when the store didn't say.
        val versionName: String?,
        val storeUrl: String,
        val source: UpdatePromptSource,
    ) : Route

    /*
     * Why: "Get the app"'s code (14.2), on the web (a dialog) and the Mac (a sheet) only: the QR code that opens the
     * stores, from Home's card and Profile's row (Settings on the Mac), with "I downloaded it".
     */
    data class AppDownload(
        val source: AppDownloadSource,
    ) : Route

    // Why: "Excluir conta e dados", reached from the profile of a Google or Apple account.
    data object DeleteAccount : Route

    // Why: "Notificações", a screen of its own on Android and iOS; the web and macOS keep it in the profile.
    data object NotificationSettings : Route

    /*
     * Why: "Mapa de categorias" (flow 9), from Perfil › Preferências and from "Categoria" (9.0); the Mac keeps it in
     * its Ajustes.
     */
    data object CategoryMap : Route

    /*
     * Why: "Privacy Policy" or "Terms of Service", from Profile's footer, drawn natively from the site's Markdown on
     * Android, iOS and the Mac (a sheet on its Settings); the web opens the site's page instead. anchor is the section a
     * link of the other text points at ("keep" in /privacy#keep), which the text opens scrolled to; null for the top.
     */
    data class LegalDocument(
        val page: LegalPage,
        val anchor: String?,
    ) : Route

    // Why: "Convidar", the group's link and QR code. It takes the place of "Novo grupo" once the group is created.
    data class Invite(
        val groupId: String,
        // Why: right after "Novo grupo", rather than from the group; only the latter offers "Trocar link".
        val isNewGroup: Boolean,
    ) : Route

    /*
     * Why: the group's own screen, where a group tapped on the home opens: its header over Despesas, Saldos and Membros,
     * which take each other's place inside it, never on the back stack. tab is the one it opens on. joinedAs is the
     * name someone just joined the group as, for "Você entrou como Bruno" (3.3): joining lands here.
     */
    data class GroupDetail(
        val groupId: String,
        val joinedAs: String?,
        val tab: GroupTab,
        /*
         * Why: opened from Rachar conta's group, over the form (beta feedback): an entry of its own, never equal to
         * the group's screen under the form, which Navigation 3's keys and each screen's "on top" would take for it.
         */
        val isOverBill: Boolean,
    ) : Route {
        companion object {
            // Why: the group's Despesas, where a group tapped anywhere opens.
            fun expensesOf(groupId: String): GroupDetail = GroupDetail(
                groupId,
                joinedAs = null,
                tab = GroupTab.EXPENSES,
                isOverBill = false,
            )
        }
    }

    /*
     * Why: "Estatísticas do grupo" (6.6), from the chart button on the group's bar: what the group spent and who
     * spent it, from the bills on the device. Back goes to the tab it was opened from.
     */
    data class GroupStats(
        val groupId: String,
    ) : Route

    /*
     * Why: "Expenses by category", from a category of the statistics' "By category" (its slice, legend line or row):
     * the group's bills of that main category, whose chips switch or add others. The first instant of the first and
     * last days of the period the statistics showed, both null for the whole history (which then follows new bills).
     * Back goes to the statistics.
     */
    data class CategoryExpenses(
        val groupId: String,
        val categoryId: String,
        // Why: the statistics' currency chip: the list and its total hold the bills in that currency only.
        val currency: GroupCurrency,
        val periodStartMillis: Long?,
        val periodEndMillis: Long?,
        // Why: the period was "This month" or "Last month", which the statistics name as a month ("October 2026").
        val isMonthPeriod: Boolean,
    ) : Route

    /*
     * Why: the group's avatar tapped on its screen: the photo, or the initials on the color, big over the group, with
     * shortcuts that change the photo at once, as the profile's (ProfilePhoto).
     */
    data class GroupPhoto(
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

    /*
     * Why: "Como funciona" of "Simplificar dívidas" (13.2), from Editar grupo's row and Saldos' "Dívidas simplificadas":
     * the same words for everyone, the example's amounts in the group's currency. Pushed over whichever opened it
     * (inside Editar grupo's sheet on iOS and macOS, in its dialog's place on the web).
     */
    data class SimplifyDebtsInfo(
        val currency: GroupCurrency,
    ) : Route

    /*
     * Why: "Rachar conta", a new bill in the group (a copy of duplicatesExpenseId's), or "Editar conta" for expenseId,
     * with "Dividir" drawn over it.
     */
    data class SplitBill(
        val groupId: String,
        val expenseId: String?,
        // Why: from the home's "Rachar conta" (7.1i), which asks "Rachar em qual grupo?" first, groupId marked.
        val picksGroupFirst: Boolean,
        /*
         * Why: from Início's "Ler cupom fiscal" (7.1e), whose photo, picked on Início, waits in TakePickedReceipt to be
         * read (beta feedback, part 3, item 16); without one there (an app restored) the form asks for it again.
         */
        val scansReceipt: Boolean,
        /*
         * Why: "Duplicar" on a bill: a new bill (expenseId stays null) that opens filled in with this one's description,
         * amount, category, payers and split, for the person to change whatever they want before saving it.
         */
        val duplicatesExpenseId: String?,
    ) : Route {
        companion object {
            // Why: "Editar conta" for expenseId, in the group it is in.
            fun createEditing(
                groupId: String,
                expenseId: String,
            ): SplitBill = SplitBill(
                groupId = groupId,
                expenseId = expenseId,
                picksGroupFirst = false,
                scansReceipt = false,
                duplicatesExpenseId = null,
            )

            // Why: "Rachar conta" for a new bill opened from the group, which is where it goes.
            fun createNew(groupId: String): SplitBill = SplitBill(
                groupId = groupId,
                expenseId = null,
                picksGroupFirst = false,
                scansReceipt = false,
                duplicatesExpenseId = null,
            )

            // Why: "Duplicar" on a bill of the group: a new bill there, filled in with that one.
            fun createDuplicating(
                groupId: String,
                expenseId: String,
            ): SplitBill = SplitBill(
                groupId = groupId,
                expenseId = null,
                picksGroupFirst = false,
                scansReceipt = false,
                duplicatesExpenseId = expenseId,
            )

            // Why: a home tab's "Rachar conta" (7.1i): a new bill that asks "Rachar em qual grupo?" first, groupId marked.
            fun createPickingGroup(
                groupId: String,
                scansReceipt: Boolean = false,
            ): SplitBill = SplitBill(
                groupId = groupId,
                expenseId = null,
                picksGroupFirst = true,
                scansReceipt = scansReceipt,
                duplicatesExpenseId = null,
            )
        }
    }

    // Why: "Conta", one bill of the group: what it was, who paid and how it splits.
    data class BillDetail(
        val groupId: String,
        val expenseId: String,
        // Why: a comment's link, or its item in Atividade, scrolls to the comment and highlights it; null for none.
        val focusCommentId: String?,
    ) : Route

    // Why: "Compartilhar conta" (7.3) over Conta: the bill's link and who may open it, online only.
    data class ShareBill(
        val groupId: String,
        val expenseId: String,
    ) : Route

    /*
     * Why: a bill's link opened (7.3h), blitzsplit.com/c/<code>: over wherever the app is, or the welcome without a
     * profile, as an invite.
     */
    data class SharedBill(
        val code: String,
        // Why: a comment the link was shared at ("?comentario="), which the page scrolls to; null for none.
        val commentId: String?,
    ) : Route

    /*
     * Why: "Registrar pagamento", a payment from one member to another made outside the app: Recebi, Já pagou or
     * Acertar in Saldos, or Acertar on the group's balance card. A sheet or a dialog over the group.
     */
    data class RegisterPayment(
        val groupId: String,
        val fromMemberId: String,
        val toMemberId: String,
        // Why: the pair owes in one currency at a time; a pair in two is two payments.
        val currency: GroupCurrency,
        /*
         * Why: Acertar opens "Pagar Ana" (4.5) first, the receiver's keys, while this device's user pays someone who
         * has one; Início's "Quem pagou a quem?" only records a payment, so it opens the form straight away.
         */
        val offersReceiverKeys: Boolean,
    ) : Route

    /*
     * Why: Início's "Registrar pagamento", which asks who paid whom first: every open pair of every group, the most
     * recently used group first, one of which opens Registrar pagamento in its place.
     */
    data object PickPayment : Route

    /*
     * Why: "Todas as despesas", which Início's "Ver tudo" over "Últimas despesas" opens over Início: every bill this
     * device's user paid or takes part in, across groups, by day, each with its ⋮ (Pierre's call, beta feedback).
     */
    data object AllExpenses : Route

    /*
     * Why: the home's third tab, "Saldos" (flow 8): each person once, netted across every group. Right above Início,
     * in the place of the other tabs, so back returns to Início; "A receber" and "A pagar" open it already filtered.
     */
    data class GlobalBalances(
        val filter: BalancesFilter,
    ) : Route

    /*
     * Why: "Cobrar" (8.2), a message for someone who owes you, opened over whichever screen asked for it (Saldos, a
     * contact's profile) as a dialog or sheet, so closing it goes back there. personKey is PersonBalance.key, in the
     * currency the screen showed.
     */
    data class Charge(
        val personKey: String,
        val currency: GroupCurrency,
    ) : Route

    /*
     * Why: the home's fourth tab, "Contatos" (flow 11): everyone in the user's groups, guests included, with the net of
     * each. Over the groups like Saldos, so the Grupos tab and back return to them.
     */
    data object Contacts : Route

    /*
     * Why: "Perfil do contato" (11.2), a person's net, groups in common and history with them, from Contatos and its
     * search. personKey is PeopleBalances.keyOf, the same person Saldos nets. A wide window draws it beside Contatos
     * instead (ContactsUiState.selectedKey).
     */
    data class ContactProfile(
        val personKey: String,
    ) : Route

    /*
     * Why: a profile link opened, blitzsplit.com/u/<code> ("Share profile"), over wherever the app is, or the welcome
     * without a profile, as an invite. It stays only while it asks the server or has no contact to show: a contact
     * replaces it with their profile, and the person's own link with Profile.
     */
    data class ProfileLink(
        val code: String,
    ) : Route

    // Why: "Convidar da agenda" (11.3): the device's contacts, each invited with a group's link. Not on the web.
    data object ContactsAgenda : Route

    /*
     * Why: "Convidar" (11.4), a group's link and QR code, the group picked in the sheet ("Novo grupo" included), for
     * name from the agenda or for nobody in particular ("Convidar com link ou QR"). For a guest (personKey of a name
     * nobody joined as), their personal link in the groups they are in instead.
     */
    data class ContactInvite(
        val personKey: String?,
        val name: String?,
    ) : Route

    /*
     * Why: "Atividade" (page 12), the feed and notification center of every group, which Início's bell opens over it
     * (no tab since iOS's search tab took its place in the bar, Pierre's call). personKey (PeopleBalances.keyOf) opens
     * it on one person, over a contact's profile, from their history, as a removable chip. showsBillsOnly opens it with
     * the Bills type already picked, as Início's "Ver tudo" over "Últimas despesas" does, a removable chip too.
     */
    data class Activity(
        val personKey: String?,
        val showsBillsOnly: Boolean,
    ) : Route {
        companion object {
            // Why: everyone's feed with nothing picked, as the bell and a push notification open it.
            val all: Activity = Activity(
                personKey = null,
                showsBillsOnly = false,
            )

            // Why: Início's "Ver tudo" over "Últimas despesas".
            val billsOnly: Activity = Activity(
                personKey = null,
                showsBillsOnly = true,
            )
        }
    }

    /*
     * Why: "Acertar" or "Recebi" of someone in the global Saldos: Registrar pagamento for the whole net with them in
     * that currency, one payment in each group so each group closes right. personKey is PersonBalance.key.
     */
    data class SettleUp(
        val personKey: String,
        val currency: GroupCurrency,
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
