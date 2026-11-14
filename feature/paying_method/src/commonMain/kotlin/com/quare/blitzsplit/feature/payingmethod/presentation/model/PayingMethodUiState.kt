package com.quare.blitzsplit.feature.payingmethod.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.CardColor
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethodRules
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethodType

data class PayingMethodUiState(
    // Why: "Edit payment method" with "Delete payment method" for a saved one, "New payment method" otherwise.
    val isEditing: Boolean,
    // Why: a saved method fills the form once read, so the form doesn't flash empty first.
    val isLoaded: Boolean,
    /*
     * Why: the types the form offers: Pix only under the Pix rule (docs/product/general.md) or for a Pix method being
     * edited, and cash only while the person has none, since one wallet is enough.
     */
    val types: List<PayingMethodType>,
    val type: PayingMethodType,
    val name: String,
    // Why: a card's swatch, kept while another type is picked so going back to a card finds it.
    val cardColor: CardColor,
    val isDefault: Boolean,
    val isSaving: Boolean,
    val isDeleteConfirmationShown: Boolean,
    // Why: the form as it opened, once filled; closing with anything else asks "Discard changes?".
    val openedInput: PayingMethodInput?,
) {
    val input: PayingMethodInput
        get() = PayingMethodInput(
            type = type,
            name = name.trim(),
            cardColor = cardColor.takeIf { type.isCard },
            isDefault = isDefault,
        )

    val hasUnsavedInput: Boolean
        get() = isLoaded && openedInput != null && input != openedInput

    // Why: the color row only shows for a card, whose icon it paints.
    val showsCardColors: Boolean
        get() = type.isCard

    val cardColors: List<CardColor>
        get() = CardColor.entries

    // Why: "Other" needs a name ("Name" rather than "Name (optional)"), which the UIs label and Save waits for.
    val isNameRequired: Boolean
        get() = type.needsName

    val nameMaxLength: Int
        get() = PayingMethodRules.MAX_NAME_LENGTH

    val canSave: Boolean
        get() = isLoaded && !isSaving && PayingMethodRules.isNameValid(type, name)
}

data class PayingMethodInput(
    val type: PayingMethodType,
    val name: String,
    val cardColor: CardColor?,
    val isDefault: Boolean,
)
