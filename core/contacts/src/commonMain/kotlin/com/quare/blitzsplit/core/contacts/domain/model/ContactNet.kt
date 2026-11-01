package com.quare.blitzsplit.core.contacts.domain.model

import com.quare.blitzsplit.core.model.GroupCurrency

// Why: amounts of two currencies are written apart, never added up.
data class ContactNet(
    val currency: GroupCurrency,
    // Why: positive when they owe you ("te deve"), negative when you owe them ("você deve").
    val amountMinor: Long,
)
