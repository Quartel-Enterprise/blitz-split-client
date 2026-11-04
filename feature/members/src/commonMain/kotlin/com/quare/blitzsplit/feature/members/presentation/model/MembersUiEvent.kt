package com.quare.blitzsplit.feature.members.presentation.model

import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.presentation.analytics.TrackedUiEvent

// Why: the screen these events come from, as their source param says.
private const val SOURCE = "members"

sealed interface MembersUiEvent : TrackedUiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            OnShareClicked -> EventAnalytics.Automatic("members_share_clicked")

            OnAddClicked -> EventAnalytics.Automatic("member_add_clicked")

            is OnMemberClicked -> EventAnalytics.Automatic("person_clicked", mapOf("source" to SOURCE))

            is OnRenameClicked -> EventAnalytics.Automatic("member_rename_clicked")

            is OnPersonalInviteClicked -> EventAnalytics.Automatic("personal_invite_clicked")

            // Why: the sheet closed, which each platform also does its own way (a swipe, a tap outside).
            OnPersonalInviteDismissed -> EventAnalytics.NotTracked

            OnPersonalLinkCopyClicked -> EventAnalytics.Automatic("personal_invite_copy_clicked")

            OnPersonalLinkShareClicked -> EventAnalytics.Automatic("personal_invite_share_clicked")

            is OnRemoveClicked -> EventAnalytics.Automatic("member_remove_clicked")

            OnRemoveConfirmed -> EventAnalytics.Automatic("member_remove_confirmed")

            OnRemoveDismissed -> EventAnalytics.Automatic("member_remove_cancelled")

            is OnReleaseClicked -> EventAnalytics.Automatic("member_release_clicked")

            OnReleaseConfirmed -> EventAnalytics.Automatic("member_release_confirmed")

            OnReleaseDismissed -> EventAnalytics.Automatic("member_release_cancelled")

            is OnDraftChanged -> EventAnalytics.NotTracked

            is OnSuggestionClicked -> EventAnalytics.Automatic(
                "member_name_suggestion_clicked",
                mapOf("source" to SOURCE),
            )

            OnSaveClicked -> EventAnalytics.Automatic("member_editor_save_clicked")

            OnEditorDismissed -> EventAnalytics.NotTracked
        }

    // Why: "Compartilhar" on the link card opens "Convidar", with the QR code.
    data object OnShareClicked : MembersUiEvent

    data object OnAddClicked : MembersUiEvent

    // Why: a member's row (their face or name), which opens their profile; the ⋮ keeps the member's menu.
    data class OnMemberClicked(
        val memberId: String,
    ) : MembersUiEvent

    data class OnRenameClicked(
        val memberId: String,
    ) : MembersUiEvent

    // Why: "Convite pessoal para Carla" in the member's menu, which opens the sheet with the member's link.
    data class OnPersonalInviteClicked(
        val memberId: String,
    ) : MembersUiEvent

    data object OnPersonalInviteDismissed : MembersUiEvent

    // Why: "Copiar" on the personal invite.
    data object OnPersonalLinkCopyClicked : MembersUiEvent

    // Why: "Compartilhar com Carla", the system's share sheet.
    data object OnPersonalLinkShareClicked : MembersUiEvent

    // Why: "Remover do grupo" in the member's menu, which asks first.
    data class OnRemoveClicked(
        val memberId: String,
    ) : MembersUiEvent

    data object OnRemoveConfirmed : MembersUiEvent

    data object OnRemoveDismissed : MembersUiEvent

    // Why: "Liberar nome" in the menu of someone who joined, for the group's creator, which asks first.
    data class OnReleaseClicked(
        val memberId: String,
    ) : MembersUiEvent

    data object OnReleaseConfirmed : MembersUiEvent

    data object OnReleaseDismissed : MembersUiEvent

    data class OnDraftChanged(
        val draft: String,
    ) : MembersUiEvent

    data class OnSuggestionClicked(
        val name: String,
    ) : MembersUiEvent

    data object OnSaveClicked : MembersUiEvent

    data object OnEditorDismissed : MembersUiEvent
}
