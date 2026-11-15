package com.quare.blitzsplit.core.expenses.customcolor

import com.quare.blitzsplit.core.expenses.domain.model.CustomColor

/*
 * Why: a long press on one of the person's colors: "Delete this color?" when nothing uses it, else "This color is in use",
 * which names the category (or the payment method) to change first and deletes nothing.
 */
data class CustomColorDeletion(
    val color: CustomColor,
    // Why: the first main category the person can see with this color, else their first card with it; null for none.
    val usedBy: CustomColorUser?,
    // Why: "Delete" waits while the device deletes it.
    val isDeleting: Boolean,
) {
    val isBlocked: Boolean
        get() = usedBy != null
}
