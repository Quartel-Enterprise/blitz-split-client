package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: how the person pays ("Payment methods" in Profile), which they mark on the bills they paid ("Paid with"). Private:
 * only they see it, on their devices and in their own export; the group's other members never do.
 */
data class PayingMethod(
    val id: String,
    val type: PayingMethodType,
    // Why: what the person calls it ("Nubank"), empty for none, so each UI shows the type's name instead.
    val name: String,
    // Why: a card's swatch, null for anything but a card.
    val cardColor: CardColor?,
    val isDefault: Boolean,
    val addedAt: Long,
)
