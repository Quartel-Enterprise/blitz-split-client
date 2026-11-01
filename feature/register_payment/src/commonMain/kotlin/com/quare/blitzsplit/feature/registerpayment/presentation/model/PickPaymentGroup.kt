package com.quare.blitzsplit.feature.registerpayment.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency

// Why: a group with an open pair, under its name.
data class PickPaymentGroup(
    val groupId: String,
    val name: String,
    val initials: String,
    val color: AvatarColor,
    val currency: GroupCurrency,
    // Why: the pairs with this device's user first, then the others, the largest first in each.
    val pairs: List<PickPaymentPair>,
)
