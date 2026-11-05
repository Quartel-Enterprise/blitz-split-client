package com.quare.blitzsplit.feature.creategroup.presentation.model

import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.PhotoCropEvent
import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.presentation.analytics.toEventAnalytics

// Why: the screen these events come from, as their source param says.
private const val SOURCE = "create_group"

sealed interface CreateGroupUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            OnCloseClicked -> EventAnalytics.NotTracked

            is OnNameChanged -> EventAnalytics.NotTracked

            OnColorClicked -> EventAnalytics.Automatic("group_color_clicked", mapOf("source" to SOURCE))

            OnShapeAndColorClicked -> EventAnalytics.Automatic(
                "group_shape_and_color_clicked",
                mapOf("source" to SOURCE),
            )

            /*
             * Why: each UI opens the camera or the picker straight from its tap, so the photo handed back stands for
             * it.
             */
            is OnPhotoPicked -> EventAnalytics.Automatic("photo_picked", mapOf("source" to SOURCE))

            is OnPhotoCrop -> event.toEventAnalytics(SOURCE)

            OnPhotoUnreadable -> EventAnalytics.NotTracked

            OnPhotoRemoved -> EventAnalytics.Automatic("photo_removed", mapOf("source" to SOURCE))

            is OnTypeSelected -> EventAnalytics.Automatic(
                "group_type_selected",
                mapOf(
                    "type" to type,
                    "source" to SOURCE,
                ),
            )

            is OnCurrencySelected -> EventAnalytics.Automatic(
                "group_currency_selected",
                mapOf(
                    "currency" to currency,
                    "source" to SOURCE,
                ),
            )

            is OnDraftChanged -> EventAnalytics.NotTracked

            OnAddMemberClicked -> EventAnalytics.Automatic("create_group_add_member_clicked")

            is OnSuggestionClicked -> EventAnalytics.Automatic(
                "member_name_suggestion_clicked",
                mapOf("source" to SOURCE),
            )

            is OnRemoveMemberClicked -> EventAnalytics.Automatic("create_group_remove_member_clicked")

            // Why: the tap, and the group it created once saved, with what kind of group it is.
            OnCreateClicked -> EventAnalytics.Manual(setOf("create_group_create_clicked", "group_created"))
        }

    data object OnCloseClicked : CreateGroupUiEvent

    data class OnNameChanged(
        val name: String,
    ) : CreateGroupUiEvent

    // Why: the group's avatar takes the next color of the palette.
    data object OnColorClicked : CreateGroupUiEvent

    /*
     * Why: Android's "Trocar forma e cor do grupo" takes the next color and the next shape at once; five colors over
     * four shapes go through all twenty before they repeat.
     */
    data object OnShapeAndColorClicked : CreateGroupUiEvent

    // Why: each UI opens its own picker or camera straight from the tap and hands over the file it returned.
    class OnPhotoPicked(
        val source: ByteArray,
    ) : CreateGroupUiEvent

    // Why: what was done in "Ajustar foto", the editor a picked photo opens in.
    data class OnPhotoCrop(
        val event: PhotoCropEvent,
    ) : CreateGroupUiEvent

    // Why: the picker returned an item the UI couldn't read, such as a cloud-only photo while offline.
    data object OnPhotoUnreadable : CreateGroupUiEvent

    // Why: "Remover foto", which brings back the color and the initials.
    data object OnPhotoRemoved : CreateGroupUiEvent

    data class OnTypeSelected(
        val type: GroupType,
    ) : CreateGroupUiEvent

    data class OnCurrencySelected(
        val currency: GroupCurrency,
    ) : CreateGroupUiEvent

    data class OnDraftChanged(
        val draft: String,
    ) : CreateGroupUiEvent

    // Why: "Adicionar", and the keyboard's done key in the draft.
    data object OnAddMemberClicked : CreateGroupUiEvent

    data class OnSuggestionClicked(
        val name: String,
    ) : CreateGroupUiEvent

    data class OnRemoveMemberClicked(
        val memberId: String,
    ) : CreateGroupUiEvent

    data object OnCreateClicked : CreateGroupUiEvent
}
