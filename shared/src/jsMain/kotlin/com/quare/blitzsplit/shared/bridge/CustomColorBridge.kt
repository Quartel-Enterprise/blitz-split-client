@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.expenses.customcolor.CustomColorDeletion
import com.quare.blitzsplit.core.expenses.customcolor.CustomColorEvent
import com.quare.blitzsplit.core.expenses.customcolor.CustomColorState
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTint
import com.quare.blitzsplit.core.expenses.domain.model.ColorDraft
import com.quare.blitzsplit.core.expenses.domain.model.CustomColor

// Why: a custom color's tones as CSS colors ("#RRGGBB"), the same the Android and Apple apps derive.
class JsColorTones(
    val lightBg: String,
    val lightFg: String,
    val darkBg: String,
    val darkFg: String,
    val lightData: String,
    val darkData: String,
)

// Why: the tones of a category color code that is a custom color's hex; null for a named one, whose tones the page has.
fun getCustomColorTones(code: String): JsColorTones? {
    val tint = CategoryTint.fromCodeOrNull(code) as? CategoryTint.Custom ?: return null
    val tones = tint.color.toTones()
    return JsColorTones(
        lightBg = hexOf(tones.lightContainer),
        lightFg = hexOf(tones.lightContent),
        darkBg = hexOf(tones.darkContainer),
        darkFg = hexOf(tones.darkContent),
        lightData = hexOf(tones.lightData),
        darkData = hexOf(tones.darkData),
    )
}

// Why: "New color": the gradient's position, the fields as typed and the color they make.
class JsColorDraft(
    val hue: Double,
    val saturation: Double,
    val value: Double,
    val hexText: String,
    val redText: String,
    val greenText: String,
    val blueText: String,
    val color: String,
    val isHexInvalid: Boolean,
    val isRgbInvalid: Boolean,
    val canSave: Boolean,
)

// Why: "Delete this color?", or "This color is in use" while usedBy names the category to change first.
class JsCustomColorDeletion(
    val color: String,
    val usedBy: JsCategory?,
    val isDeleting: Boolean,
)

// Why: the person's own colors in a picker; each a hex code.
class JsCustomColors internal constructor(
    val palette: Array<String>,
    val canAdd: Boolean,
    val draft: JsColorDraft?,
    val deletion: JsCustomColorDeletion?,
    private val state: CustomColorState,
) {
    // Why: the swatches after the named ones in a picker showing selectedCode, another member's color included.
    fun swatchesFor(selectedCode: String?): Array<String> = state
        .customSwatchesFor(CategoryTint.fromCodeOrNull(selectedCode))
        .map { it.hex }
        .toTypedArray()

    fun isDeletable(code: String): Boolean = CustomColor.fromHexOrNull(code)?.let(state::isDeletable) == true
}

// Why: what the page sends about the person's own colors, on the screen's own ViewModel.
class JsCustomColorActions internal constructor(
    private val send: (CustomColorEvent) -> Unit,
) {
    // Why: "+"; categoryKey is the app category "Change the color of" is open for, null for the form.
    fun add(
        currentCode: String,
        categoryKey: String?,
    ) {
        val current = CategoryTint.fromCodeOrNull(currentCode) ?: return
        send(CustomColorEvent.OnAddClicked(current, categoryKey))
    }

    fun changeHsv(
        hue: Double,
        saturation: Double,
        value: Double,
    ) {
        send(CustomColorEvent.OnHsvChanged(hue, saturation, value))
    }

    fun changeHex(text: String) {
        send(CustomColorEvent.OnHexChanged(text))
    }

    // Why: channel is "red", "green" or "blue".
    fun changeChannel(
        channel: String,
        text: String,
    ) {
        val picked =
            ColorDraft.ColorChannel.entries.firstOrNull { it.name.equals(channel, ignoreCase = true) } ?: return
        send(CustomColorEvent.OnChannelChanged(picked, text))
    }

    fun save() {
        send(CustomColorEvent.OnSaveClicked)
    }

    fun dismiss() {
        send(CustomColorEvent.OnDraftDismissed)
    }

    fun longPress(code: String) {
        val color = CustomColor.fromHexOrNull(code) ?: return
        send(CustomColorEvent.OnLongPressed(color))
    }

    fun confirmDelete() {
        send(CustomColorEvent.OnDeleteConfirmed)
    }

    fun dismissDeletion() {
        send(CustomColorEvent.OnDeletionDismissed)
    }
}

internal fun toJsCustomColors(state: CustomColorState): JsCustomColors = JsCustomColors(
    palette = state.palette.map { it.hex }.toTypedArray(),
    canAdd = state.canAdd,
    draft = state.draft?.let(::toJsColorDraft),
    deletion = state.deletion?.let(::toJsCustomColorDeletion),
    state = state,
)

private fun toJsColorDraft(draft: ColorDraft): JsColorDraft = JsColorDraft(
    hue = draft.hue,
    saturation = draft.saturation,
    value = draft.value,
    hexText = draft.hexText,
    redText = draft.redText,
    greenText = draft.greenText,
    blueText = draft.blueText,
    color = draft.color.hex,
    isHexInvalid = draft.isHexInvalid,
    isRgbInvalid = draft.isRgbInvalid,
    canSave = draft.canSave,
)

private fun toJsCustomColorDeletion(deletion: CustomColorDeletion): JsCustomColorDeletion = JsCustomColorDeletion(
    color = deletion.color.hex,
    usedBy = deletion.usedBy?.let(::toJsCategory),
    isDeleting = deletion.isDeleting,
)

private fun hexOf(rgb: Int): String = CustomColor(rgb).hex
