@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.model.GroupGridSize
import com.quare.blitzsplit.core.model.GroupsDisplay
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.groups.presentation.model.BalanceTotal
import com.quare.blitzsplit.feature.groups.presentation.model.GroupRow
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsSummary
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiAction
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.SearchScope
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.GroupsViewModel
import kotlinx.coroutines.flow.map
import org.koin.mp.KoinPlatform

class JsMemberFace(
    val initial: String,
    val color: String,
)

class JsGroupRow(
    val id: String,
    val name: String,
    val initials: String,
    val color: String,
    // Why: up to three faces; the rest is "+N".
    val faces: Array<JsMemberFace>,
    val extraMemberCount: Int,
    val memberCount: Int,
    val isPending: Boolean,
    // Why: "na fila · Mercado"; null when only the group or a payment waits.
    val pendingBillDescription: String?,
    // Why: the ISO 4217 code ("BRL") and its digits after the decimal separator.
    val currency: String,
    val fractionDigits: Int,
    // Why: GroupBalanceKind's name: "TO_RECEIVE", "YOU_OWE", "YOU_ARE_SETTLED", "NO_EXPENSES", "EVERYONE_SETTLED".
    val balanceKind: String,
    // Why: amounts in the currency's minor unit, as numbers JavaScript reads exactly.
    val netMinor: Double,
    val receivesMinor: Double,
    val owesMinor: Double,
    // Why: GroupRowDetail's name, what the row says besides its last activity.
    val detail: String,
    val lastActivityMillis: Double,
    // Why: ActivityAge's name ("TODAY", "DAYS"…) and the days or weeks it counts.
    val activityAge: String,
    val activityCount: Int,
    val isSettled: Boolean,
    val usesTwoNameLines: Boolean,
    // Why: the hints under "Sair do grupo" ("Acerte os R$ 98,00 antes", 0 once settled) and "Apagar grupo".
    val settleBeforeLeavingMinor: Double,
    val hasOpenDebts: Boolean,
)

// Why: "Saldo geral", "A receber" and "A pagar" in one currency.
class JsBalanceTotal(
    val currency: String,
    val fractionDigits: Int,
    val netMinor: Double,
    val toReceiveMinor: Double,
    val toPayMinor: Double,
)

class JsGroupsSummary(
    // Why: the currency with the most groups first.
    val totals: Array<JsBalanceTotal>,
    // Why: GroupBalanceKind's name of the first currency's net: "TO_RECEIVE", "YOU_OWE" or "YOU_ARE_SETTLED".
    val kind: String,
    val canOpenToReceive: Boolean,
    val canOpenToPay: Boolean,
    // Why: "em 2 grupos".
    val toReceiveGroupCount: Int,
    val toPayGroupCount: Int,
)

class JsGroupsState(
    val displayName: String,
    val avatarInitial: String,
    val avatarColor: String,
    val photoUrl: String?,
    // Why: "google" or "apple", null while there is no account.
    val accountProvider: String?,
    // Why: null when everything is synced, which draws no badge.
    val syncBadge: String?,
    // Why: "Offline · 1 alteração na fila".
    val pendingChangeCount: Int,
    val canRetrySync: Boolean,
    val showsRecoveryCodePrompt: Boolean,
    // Why: the latest activity first; nothing draws until they were read.
    val groups: Array<JsGroupRow>,
    val hasLoadedGroups: Boolean,
    val showsEmptyState: Boolean,
    val showsSummary: Boolean,
    val summary: JsGroupsSummary,
    // Why: the tabs Em aberto and Quitados, already filtered by "Filtrar grupos".
    val openGroups: Array<JsGroupRow>,
    val settledGroups: Array<JsGroupRow>,
    val showsSettledGroups: Boolean,
    // Why: "Nenhum grupo em aberto" or "Nenhum grupo quitado" on a tab with no group and no filter.
    val showsNoOpenGroups: Boolean,
    val showsNoSettledGroups: Boolean,
    val filterQuery: String,
    // Why: "Carla apagou o grupo Aniversário da Ana.", a snackbar; deletedByName is null once that account is gone.
    val deletedGroupId: String?,
    val deletedGroupName: String?,
    val deletedGroupByName: String?,
    // Why: "Sair do grupo" and "Apagar grupo" from a row, the group's dialogs; offline both need the internet.
    val exit: JsGroupExit,
    // Why: the group those dialogs are about, whose currency their amounts are in.
    val exitGroup: JsGroupRow?,
    // Why: GroupsDisplay's code ("list" or "grid") and GroupGridSize's ("compact", "standard" or "large").
    val display: String,
    val gridSize: String,
    // Why: the open groups as cards; Quitados stays the table or the list.
    val showsGrid: Boolean,
    val canPickGridSize: Boolean,
    // Why: "Pagamento de Ana registrado." with Desfazer, once Registrar pagamento opened by a row's Acertar closes.
    val recordedPayment: JsPaymentNotice?,
    // Why: "Buscar…" and ⌘K only once there is a group; the dialog shows while isSearching.
    val canSearch: Boolean,
    val isSearching: Boolean,
    val search: JsGroupsSearch,
    // Why: the Atividade tab's badge, every unread event whatever its filters.
    val activityUnreadCount: Int,
)

class GroupsPresenter {
    private val holder = ViewModelHolder(GroupsViewModel::class) { KoinPlatform.getKoin().get() }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsGroupsState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsGroupsState(
                displayName = state.displayName,
                avatarInitial = state.avatarInitial,
                avatarColor = state.avatarColor.code,
                photoUrl = state.photoUrl,
                accountProvider = state.accountProvider?.code,
                syncBadge = state.syncBadge?.name,
                pendingChangeCount = state.pendingChangeCount,
                canRetrySync = state.canRetrySync,
                showsRecoveryCodePrompt = state.showsRecoveryCodePrompt,
                groups = state.groups.map(::toJsGroupRow).toTypedArray(),
                hasLoadedGroups = state.hasLoadedGroups,
                showsEmptyState = state.showsEmptyState,
                showsSummary = state.showsSummary,
                summary = toJsGroupsSummary(state.summary),
                openGroups = state.openGroups.map(::toJsGroupRow).toTypedArray(),
                settledGroups = state.settledGroups.map(::toJsGroupRow).toTypedArray(),
                showsSettledGroups = state.showsSettledGroups,
                showsNoOpenGroups = state.showsNoOpenGroups,
                showsNoSettledGroups = state.showsNoSettledGroups,
                filterQuery = state.filterQuery,
                deletedGroupId = state.deletedGroupNotice?.groupId,
                deletedGroupName = state.deletedGroupNotice?.groupName,
                deletedGroupByName = state.deletedGroupNotice?.deletedByName,
                exit = toJsGroupExit(state.exit),
                exitGroup = state.exitGroup?.let(::toJsGroupRow),
                display = state.layout.display.code,
                gridSize = state.layout.gridSize.code,
                showsGrid = state.showsGrid,
                canPickGridSize = state.canPickGridSize,
                recordedPayment = state.recordedPayment?.let(::toJsPaymentNotice),
                canSearch = state.canSearch,
                isSearching = state.isSearching,
                search = toJsGroupsSearch(state.search),
                activityUnreadCount = state.activityUnreadCount,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    // Why: "coming-soon", "leave-error", "delete-error" and "change-error", a toast in the page's words.
    fun observeActions(onAction: (String) -> Unit): JsWatch {
        val actions = viewModel.uiAction.map { action ->
            when (action) {
                GroupsUiAction.ShowComingSoon -> "coming-soon"
                GroupsUiAction.ShowLeaveError -> "leave-error"
                GroupsUiAction.ShowDeleteError -> "delete-error"
                GroupsUiAction.ShowChangeError -> "change-error"
            }
        }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
    }

    fun createGroup() {
        viewModel.onEvent(GroupsUiEvent.OnCreateGroupClicked)
    }

    fun openGroup(groupId: String) {
        viewModel.onEvent(GroupsUiEvent.OnGroupClicked(groupId))
    }

    fun joinWithLink() {
        viewModel.onEvent(GroupsUiEvent.OnJoinWithLinkClicked)
    }

    fun openProfile() {
        viewModel.onEvent(GroupsUiEvent.OnProfileClicked)
    }

    // Why: Início, the home's first tab.
    fun openHome() {
        viewModel.onEvent(GroupsUiEvent.OnHomeTabClicked)
    }

    fun openBalances() {
        viewModel.onEvent(GroupsUiEvent.OnBalancesTabClicked)
    }

    fun openContacts() {
        viewModel.onEvent(GroupsUiEvent.OnContactsTabClicked)
    }

    fun openActivity() {
        viewModel.onEvent(GroupsUiEvent.OnActivityTabClicked)
    }

    fun retrySync() {
        viewModel.onEvent(GroupsUiEvent.OnRetrySyncClicked)
    }

    fun splitBill() {
        viewModel.onEvent(GroupsUiEvent.OnSplitBillClicked)
    }

    fun openToReceive() {
        viewModel.onEvent(GroupsUiEvent.OnToReceiveClicked)
    }

    fun openToPay() {
        viewModel.onEvent(GroupsUiEvent.OnToPayClicked)
    }

    // Why: the tabs Em aberto (false) and Quitados (true).
    fun showSettled(isShown: Boolean) {
        viewModel.onEvent(GroupsUiEvent.OnShowSettledChanged(isShown))
    }

    fun filter(query: String) {
        viewModel.onEvent(GroupsUiEvent.OnFilterChanged(query))
    }

    // Why: the toggle "Lista" / "Grade", by GroupsDisplay's code; a code this version doesn't know does nothing.
    fun changeDisplay(display: String) {
        GroupsDisplay.fromCodeOrNull(display)?.let { viewModel.onEvent(GroupsUiEvent.OnDisplayChanged(it)) }
    }

    // Why: the Select "Tamanho", by GroupGridSize's code.
    fun changeGridSize(size: String) {
        GroupGridSize.fromCodeOrNull(size)?.let { viewModel.onEvent(GroupsUiEvent.OnGridSizeChanged(it)) }
    }

    // Why: a row's ⋯ menu or its right click.
    fun invite(groupId: String) {
        viewModel.onEvent(GroupsUiEvent.OnInviteClicked(groupId))
    }

    fun leaveGroup(groupId: String) {
        viewModel.onEvent(GroupsUiEvent.OnLeaveGroupClicked(groupId))
    }

    fun deleteGroup(groupId: String) {
        viewModel.onEvent(GroupsUiEvent.OnDeleteGroupClicked(groupId))
    }

    // Why: the main button of the dialog Sair or Apagar opened.
    fun confirmGroupExit() {
        viewModel.onEvent(GroupsUiEvent.OnGroupExitConfirmed)
    }

    fun dismissGroupExit() {
        viewModel.onEvent(GroupsUiEvent.OnGroupExitDismissed)
    }

    // Why: "Desfazer" on the notice of a payment just recorded.
    fun undoPayment() {
        viewModel.onEvent(GroupsUiEvent.OnUndoPaymentClicked)
    }

    fun recordedPaymentNoticeShown() {
        viewModel.onEvent(GroupsUiEvent.OnRecordedPaymentNoticeShown)
    }

    fun deletedGroupNoticeShown() {
        viewModel.onEvent(GroupsUiEvent.OnDeletedGroupNoticeShown)
    }

    // Why: "Buscar…" in the sidebar, the phone's magnifier, or ⌘K / Ctrl+K.
    fun openSearch() {
        viewModel.onEvent(GroupsUiEvent.OnSearchOpened)
    }

    // Why: Esc, a click outside, or a result picked.
    fun closeSearch() {
        viewModel.onEvent(GroupsUiEvent.OnSearchClosed)
    }

    fun search(query: String) {
        viewModel.onEvent(GroupsUiEvent.OnSearchQueryChanged(query))
    }

    // Why: by SearchScope's name; a name this version doesn't know does nothing.
    fun searchIn(scope: String) {
        SearchScope.entries.firstOrNull { it.name == scope }?.let {
            viewModel.onEvent(
                GroupsUiEvent.OnSearchScopeChanged(it),
            )
        }
    }

    fun openSearchedPerson(groupId: String) {
        viewModel.onEvent(GroupsUiEvent.OnSearchPersonClicked(groupId))
    }

    fun openSearchedBill(
        groupId: String,
        expenseId: String,
    ) {
        viewModel.onEvent(GroupsUiEvent.OnSearchBillClicked(groupId, expenseId))
    }

    fun dispose() {
        holder.clear()
    }
}

internal fun toJsGroupRow(row: GroupRow): JsGroupRow = JsGroupRow(
    id = row.id,
    name = row.name,
    initials = row.initials,
    color = row.color.code,
    faces = row.faces.map { JsMemberFace(it.initial, it.color.code) }.toTypedArray(),
    extraMemberCount = row.extraMemberCount,
    memberCount = row.memberCount,
    isPending = row.isPending,
    pendingBillDescription = row.pendingBillDescription,
    currency = row.currency.code,
    fractionDigits = row.currency.fractionDigits,
    balanceKind = row.balanceKind.name,
    netMinor = row.netMinor.toDouble(),
    receivesMinor = row.receivesMinor.toDouble(),
    owesMinor = row.owesMinor.toDouble(),
    detail = row.detail.name,
    lastActivityMillis = row.lastActivity.epochMillis.toDouble(),
    activityAge = row.lastActivity.age.name,
    activityCount = row.lastActivity.count,
    isSettled = row.isSettled,
    usesTwoNameLines = row.usesTwoNameLines,
    settleBeforeLeavingMinor = row.settleBeforeLeavingMinor.toDouble(),
    hasOpenDebts = row.hasOpenDebts,
)

private fun toJsGroupsSummary(summary: GroupsSummary): JsGroupsSummary = JsGroupsSummary(
    totals = summary.totals.map(::toJsBalanceTotal).toTypedArray(),
    kind = summary.kind.name,
    canOpenToReceive = summary.canOpenToReceive,
    canOpenToPay = summary.canOpenToPay,
    toReceiveGroupCount = summary.toReceiveGroupCount,
    toPayGroupCount = summary.toPayGroupCount,
)

private fun toJsBalanceTotal(total: BalanceTotal): JsBalanceTotal = JsBalanceTotal(
    currency = total.currency.code,
    fractionDigits = total.currency.fractionDigits,
    netMinor = total.netMinor.toDouble(),
    toReceiveMinor = total.toReceiveMinor.toDouble(),
    toPayMinor = total.toPayMinor.toDouble(),
)
