package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.model.GroupCurrency

// Why: a line of the balance widget: "Apê 302", "−R$ 98 you owe".
data class OpenGroupBalance(
    val groupId: String,
    val name: String,
    val currency: GroupCurrency,
    // Why: positive when this device's user gets money back in the group, negative when they owe; never zero.
    val netMinor: Long,
)
