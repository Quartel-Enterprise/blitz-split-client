package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AvatarColor

/*
 * Why: the home's sidebar around the screens outside the home's tabs (the web's group pages, Estatísticas and Perfil,
 * the Mac's "Em aberto"), which the tab's own state doesn't carry.
 */
data class HomeSidebarUiState(
    val displayName: String,
    val avatarInitial: String,
    val avatarColor: AvatarColor,
    val photoUrl: String?,
    // Why: the account under the web's profile, null while there is none.
    val accountProvider: AccountProvider?,
    // Why: "Em aberto", the groups Grupos lists open, in its order: the latest activity first.
    val openGroups: List<GroupRow>,
    // Why: Atividade's unread badge.
    val activityUnreadCount: Int,
    // Why: nothing is drawn until Room is read, so the sidebar never fills in under the page.
    val isLoaded: Boolean,
)
