@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.groups.presentation.model.HomeSidebarUiEvent
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.HomeSidebarViewModel
import kotlinx.coroutines.flow.map
import org.koin.mp.KoinPlatform

class JsHomeSidebarState(
    val displayName: String,
    val avatarInitial: String,
    val avatarColor: String,
    val photoUrl: String?,
    // Why: "google" or "apple", null while there is no account.
    val accountProvider: String?,
    // Why: "Em aberto", in Grupos' order.
    val openGroups: Array<JsGroupRow>,
    val activityUnreadCount: Int,
    val isLoaded: Boolean,
)

// Why: the home's sidebar around the group's pages, Estatísticas and Perfil, which keep it while they change.
class HomeSidebarPresenter {
    private val holder = ViewModelHolder(HomeSidebarViewModel::class) { KoinPlatform.getKoin().get() }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsHomeSidebarState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsHomeSidebarState(
                displayName = state.displayName,
                avatarInitial = state.avatarInitial,
                avatarColor = state.avatarColor.code,
                photoUrl = state.photoUrl,
                accountProvider = state.accountProvider?.code,
                openGroups = state.openGroups.map(::toJsGroupRow).toTypedArray(),
                activityUnreadCount = state.activityUnreadCount,
                isLoaded = state.isLoaded,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    fun openGroup(groupId: String) {
        viewModel.onEvent(HomeSidebarUiEvent.OnGroupClicked(groupId))
    }

    // Why: the page's tab name ("home", "groups"…); one this version doesn't know does nothing.
    fun openTab(tab: String) {
        val homeTab = homeTabOfOrNull(tab) ?: return
        viewModel.onEvent(HomeSidebarUiEvent.OnTabClicked(homeTab))
    }

    fun openProfile() {
        viewModel.onEvent(HomeSidebarUiEvent.OnProfileClicked)
    }

    // Why: the logo, which opens Home.
    fun openBrand() {
        viewModel.onEvent(HomeSidebarUiEvent.OnBrandClicked)
    }

    fun dispose() {
        holder.clear()
    }
}

// Why: the home tab a page sits in, as the web names it ("home", "groups", "balances", "contacts", "activity").
internal fun toJsHomeTab(tab: HomeTab): String = tab.name.lowercase()

private fun homeTabOfOrNull(name: String): HomeTab? = HomeTab.entries.firstOrNull { toJsHomeTab(it) == name }
