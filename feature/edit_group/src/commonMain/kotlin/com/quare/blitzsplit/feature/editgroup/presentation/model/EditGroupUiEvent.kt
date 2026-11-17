package com.quare.blitzsplit.feature.editgroup.presentation.model

import com.quare.blitzsplit.core.groups.domain.model.ReminderKind
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.PhotoCropEvent
import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.presentation.analytics.toEventAnalytics

// Why: the screen these events come from, as their source param says.
private const val SOURCE = "edit_group"

sealed interface EditGroupUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            OnCloseClicked -> EventAnalytics.NotTracked

            is OnNameChanged -> EventAnalytics.NotTracked

            OnIconClicked -> EventAnalytics.Automatic("group_icon_clicked")

            OnIconDismissed -> EventAnalytics.NotTracked

            // Why: only a pick "Ícone do grupo" takes, while it is open.
            is OnIconColorPicked -> EventAnalytics.Manual("group_icon_color_picked")

            OnIconConfirmed -> EventAnalytics.Automatic("group_icon_confirmed")

            is OnColorSelected -> EventAnalytics.Automatic(
                "group_color_selected",
                mapOf(
                    "color" to color,
                    "source" to SOURCE,
                ),
            )

            // Why: each UI opens the camera or the picker straight from its tap, so the photo handed back stands for it.
            is OnPhotoPicked -> EventAnalytics.Automatic("photo_picked", mapOf("source" to SOURCE))

            is OnPhotoCrop -> event.toEventAnalytics(SOURCE)

            OnPhotoUnreadable -> EventAnalytics.NotTracked

            is OnTypeSelected -> EventAnalytics.Automatic(
                "group_type_selected",
                mapOf(
                    "type" to type,
                    "source" to SOURCE,
                ),
            )

            // Why: only a currency the form takes, never one picked once the group has a bill.
            is OnCurrencySelected -> EventAnalytics.Manual("group_currency_selected")

            OnDefaultSplitClicked -> EventAnalytics.Automatic("default_split_clicked")

            is OnReminderEnabledChanged -> EventAnalytics.Automatic(
                "group_reminder_enabled_changed",
                mapOf("enabled" to isEnabled, "source" to "edit_group"),
            )

            is OnReminderKindSelected -> EventAnalytics.Automatic(
                "group_reminder_kind_selected",
                mapOf("kind" to kind.code, "source" to "edit_group"),
            )

            is OnReminderDaySelected -> EventAnalytics.Automatic(
                "group_reminder_day_selected",
                mapOf("source" to "edit_group"),
            )

            is OnReminderTimeSelected -> EventAnalytics.Automatic(
                "group_reminder_time_selected",
                mapOf("source" to "edit_group"),
            )

            // Why: only when the group's reminder is on and the event could be built.
            OnAddReminderToCalendarClicked -> EventAnalytics.Manual("group_reminder_calendar_clicked")

            OnSaveClicked -> EventAnalytics.Automatic("edit_group_save_clicked")

            // Why: a tap that asks the question, or one on the unavailable switch, which only says why.
            OnSimplifyDebtsClicked -> EventAnalytics.Manual(
                setOf("simplify_debts_clicked", "simplify_debts_unavailable_clicked"),
            )

            /*
             * Why: the change it applied, or only an answer to a question still open, since Apple's alert also closes
             * itself after its button.
             */
            OnSimplifyDebtsConfirmed -> EventAnalytics.Manual("simplify_debts_toggled")

            OnSimplifyDebtsDismissed -> EventAnalytics.Manual("simplify_debts_cancelled")

            OnSimplifyDebtsInfoClicked -> EventAnalytics.Automatic(
                "simplify_debts_info_clicked",
                mapOf("source" to SOURCE),
            )

            OnSimplifyConflictDismissed -> EventAnalytics.NotTracked

            OnSimplifyUnavailableNoticeDismissed -> EventAnalytics.NotTracked

            OnSimplifyConflictActivityClicked -> EventAnalytics.Automatic(
                "simplify_conflict_activity_clicked",
                mapOf("source" to SOURCE),
            )
        }

    data object OnCloseClicked : EditGroupUiEvent

    // Why: the snackbar "needs a third person" (13.1c) went, by itself or by a swipe.
    data object OnSimplifyUnavailableNoticeDismissed : EditGroupUiEvent

    data class OnNameChanged(
        val name: String,
    ) : EditGroupUiEvent

    // Why: the avatar or "Trocar ícone ou foto" opens "Ícone do grupo" (Android and iOS) on the group's icon.
    data object OnIconClicked : EditGroupUiEvent

    // Why: ✕, a swipe or back leave the form's icon as it was.
    data object OnIconDismissed : EditGroupUiEvent

    data class OnIconColorPicked(
        val color: AvatarColor,
    ) : EditGroupUiEvent

    // Why: "Usar este ícone" puts the color picked in the form, in place of the photo.
    data object OnIconConfirmed : EditGroupUiEvent

    // Why: the web's and the Mac's "Cor do ícone", in the form, which takes the photo off too.
    data class OnColorSelected(
        val color: AvatarColor,
    ) : EditGroupUiEvent

    // Why: each UI opens its own picker or camera straight from the tap and hands over the file it returned.
    class OnPhotoPicked(
        val source: ByteArray,
    ) : EditGroupUiEvent

    // Why: what was done in "Ajustar foto", the editor a picked photo opens in.
    data class OnPhotoCrop(
        val event: PhotoCropEvent,
    ) : EditGroupUiEvent

    // Why: the picker returned an item the UI couldn't read, such as a cloud-only photo while offline.
    data object OnPhotoUnreadable : EditGroupUiEvent

    data class OnTypeSelected(
        val type: GroupType,
    ) : EditGroupUiEvent

    data class OnCurrencySelected(
        val currency: GroupCurrency,
    ) : EditGroupUiEvent

    // Why: "Divisão padrão" opens over the form, which keeps what was typed.
    data object OnDefaultSplitClicked : EditGroupUiEvent

    // Why: the reminder's switch: off sends no push and shows no banner for the group.
    data class OnReminderEnabledChanged(
        val isEnabled: Boolean,
    ) : EditGroupUiEvent

    // Why: "Weekly" or "Monthly", whose day goes back to the first one the kind has.
    data class OnReminderKindSelected(
        val kind: ReminderKind,
    ) : EditGroupUiEvent

    // Why: the weekday (1 Monday to 7 Sunday) while weekly, the day of the month (1 to 31) while monthly.
    data class OnReminderDaySelected(
        val day: Int,
    ) : EditGroupUiEvent

    // Why: the time as minutes after midnight, in the member's own clock.
    data class OnReminderTimeSelected(
        val minute: Int,
    ) : EditGroupUiEvent

    // Why: "Add to calendar" under the reminder: hands the repeating event to the device's calendar.
    data object OnAddReminderToCalendarClicked : EditGroupUiEvent

    data object OnSaveClicked : EditGroupUiEvent

    // Why: the switch of "Simplificar dívidas" opens its confirmation, never changing it by itself (13.3).
    data object OnSimplifyDebtsClicked : EditGroupUiEvent

    // Why: "Simplificar", "Ligar mesmo assim" or "Desligar" applies it at once, offline too.
    data object OnSimplifyDebtsConfirmed : EditGroupUiEvent

    // Why: "Agora não", "Cancelar", a tap outside or back.
    data object OnSimplifyDebtsDismissed : EditGroupUiEvent

    // Why: "Como funciona" (13.2).
    data object OnSimplifyDebtsInfoClicked : EditGroupUiEvent

    // Why: the snackbar, banner or toast of 13.1e went away.
    data object OnSimplifyConflictDismissed : EditGroupUiEvent

    // Why: its "Ver na Atividade", where both changes show.
    data object OnSimplifyConflictActivityClicked : EditGroupUiEvent
}
