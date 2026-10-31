@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.groups.presentation.model.GroupRow
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
    val canRetrySync: Boolean,
    val showsRecoveryCodePrompt: Boolean,
    // Why: the most recently changed first; nothing draws until they were read.
    val groups: Array<JsGroupRow>,
    val hasLoadedGroups: Boolean,
    val showsEmptyState: Boolean,
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
                canRetrySync = state.canRetrySync,
                showsRecoveryCodePrompt = state.showsRecoveryCodePrompt,
                groups = state.groups.map(::toJsGroupRow).toTypedArray(),
                hasLoadedGroups = state.hasLoadedGroups,
                showsEmptyState = state.showsEmptyState,
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
)
