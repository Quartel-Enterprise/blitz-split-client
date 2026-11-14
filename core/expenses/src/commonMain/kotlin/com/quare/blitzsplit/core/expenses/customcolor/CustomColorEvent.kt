package com.quare.blitzsplit.core.expenses.customcolor

import com.quare.blitzsplit.core.expenses.domain.model.CategoryTint
import com.quare.blitzsplit.core.expenses.domain.model.ColorDraft
import com.quare.blitzsplit.core.expenses.domain.model.CustomColor

/*
 * Why: what a category's color picker sends about the person's own colors, the same from Category map's "Change the
 * color of Food" and from a category's form, in a bill's Category or in the map. Each screen wraps it in its UiEvent and
 * hands it to its CustomColorController.
 */
sealed interface CustomColorEvent {
    /*
     * Why: "+", which opens "New color" on the color the category has. categoryKey is the app category whose color
     * "Change the color of Food" changes; null for the form open on screen, whose color the new one becomes.
     */
    data class OnAddClicked(
        val current: CategoryTint,
        val categoryKey: String?,
    ) : CustomColorEvent

    // Why: the gradient dragged: hue 0 to 360, saturation and brightness 0 to 1.
    data class OnHsvChanged(
        val hue: Double,
        val saturation: Double,
        val value: Double,
    ) : CustomColorEvent

    data class OnHexChanged(
        val text: String,
    ) : CustomColorEvent

    data class OnChannelChanged(
        val channel: ColorDraft.ColorChannel,
        val text: String,
    ) : CustomColorEvent

    // Why: "Add": the color joins the palette and becomes the category's.
    data object OnSaveClicked : CustomColorEvent

    // Why: "Cancel", or the picker closed some other way.
    data object OnDraftDismissed : CustomColorEvent

    // Why: a long press (a right click on the web and the Mac) on one of the person's colors.
    data class OnLongPressed(
        val color: CustomColor,
    ) : CustomColorEvent

    // Why: "Delete" on "Delete this color?".
    data object OnDeleteConfirmed : CustomColorEvent

    // Why: "Cancel" on "Delete this color?", "OK" on "This color is in use", or the dialog closed some other way.
    data object OnDeletionDismissed : CustomColorEvent
}
