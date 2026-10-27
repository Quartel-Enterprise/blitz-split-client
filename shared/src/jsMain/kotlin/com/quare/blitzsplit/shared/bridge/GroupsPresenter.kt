@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiEvent
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.GroupsViewModel
import kotlinx.coroutines.flow.map
import org.koin.mp.KoinPlatform

class JsGroupsState(
    val avatarInitial: String,
    val avatarColor: String,
    // Why: null when everything is synced, which draws no badge.
    val syncBadge: String?,
    val canRetrySync: Boolean,
    val showsRecoveryCodePrompt: Boolean,
)

class GroupsPresenter {
    private val holder = ViewModelHolder(GroupsViewModel::class) { KoinPlatform.getKoin().get() }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsGroupsState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsGroupsState(
                avatarInitial = state.avatarInitial,
                avatarColor = state.avatarColor.code,
                syncBadge = state.syncBadge?.name,
                canRetrySync = state.canRetrySync,
                showsRecoveryCodePrompt = state.showsRecoveryCodePrompt,
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

    fun dispose() {
        holder.clear()
    }
}
