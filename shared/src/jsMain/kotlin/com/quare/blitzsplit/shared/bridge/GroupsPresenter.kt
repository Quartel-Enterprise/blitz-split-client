@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.groups.presentation.model.BalanceTotal
import com.quare.blitzsplit.feature.groups.presentation.model.GroupRow
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsSummary
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiEvent
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
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    fun observeComingSoon(onShow: (Boolean) -> Unit): JsWatch {
        val watch = FlowWatcher(viewModel.uiAction.map { true }).observe(onShow)
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

    fun deletedGroupNoticeShown() {
        viewModel.onEvent(GroupsUiEvent.OnDeletedGroupNoticeShown)
    }

    fun dispose() {
        holder.clear()
    }
}

private fun toJsGroupRow(row: GroupRow): JsGroupRow = JsGroupRow(
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
