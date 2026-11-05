package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

// Why: the screen these events come from, as their source param says.
private const val SOURCE = "sidebar"

sealed interface HomeSidebarUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            is OnGroupClicked -> EventAnalytics.Automatic("group_clicked", mapOf("source" to SOURCE))

            /*
             * Why: a tab of the home's bar, rail or sidebar, which each platform routes through whichever screen is in front:
             * where it went is its screen view.
             */
            is OnTabClicked -> EventAnalytics.NotTracked

            OnProfileClicked -> EventAnalytics.Automatic("profile_avatar_clicked", mapOf("source" to SOURCE))

            // Why: the web's logo, which opens Home; the tabs it stands beside are not tracked, their screen view is.
            OnBrandClicked -> EventAnalytics.Automatic("brand_clicked", mapOf("source" to SOURCE))

            // Why: only a search that opens, as from the tabs.
            OnSearchClicked -> EventAnalytics.Manual("search_opened")
        }

    // Why: a group of "Em aberto", opened as its row on Grupos opens it.
    data class OnGroupClicked(
        val groupId: String,
    ) : HomeSidebarUiEvent

    data class OnTabClicked(
        val tab: HomeTab,
    ) : HomeSidebarUiEvent

    // Why: the web's profile at the sidebar's foot.
    data object OnProfileClicked : HomeSidebarUiEvent

    // Why: the web's logo, which opens Home as its tab does.
    data object OnBrandClicked : HomeSidebarUiEvent

    /*
     * Why: "Buscar", the navigation's item on the pages outside the tabs too (Pierre's call), so the web's bars keep
     * the same items: it goes back to the tab the page sits in and opens the search over it, as iOS's search tab does
     * from a pushed screen.
     */
    data object OnSearchClicked : HomeSidebarUiEvent
}
