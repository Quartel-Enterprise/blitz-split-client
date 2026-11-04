package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.presentation.analytics.TrackedUiEvent

// Why: the screen these events come from, as their source param says.
private const val SOURCE = "sidebar"

sealed interface HomeSidebarUiEvent : TrackedUiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            is OnGroupClicked -> EventAnalytics.Automatic("group_clicked", mapOf("source" to SOURCE))

            /*
             * Why: a tab of the home's bar, rail or sidebar, which each platform routes through whichever screen is in front:
             * where it went is its screen view.
             */
            is OnTabClicked -> EventAnalytics.NotTracked

            OnProfileClicked -> EventAnalytics.Automatic("profile_avatar_clicked", mapOf("source" to SOURCE))

            // Why: the web's logo, which opens Início; the tabs it stands beside are not tracked, their screen view is.
            OnBrandClicked -> EventAnalytics.Automatic("brand_clicked", mapOf("source" to SOURCE))
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

    // Why: the web's logo, which opens Início as its tab does.
    data object OnBrandClicked : HomeSidebarUiEvent
}
