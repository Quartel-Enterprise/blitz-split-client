package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.model.AvatarColor

// Why: one line of "Por grupo": what one group adds to the net with that person.
data class PersonGroupBalance(
    val groupId: String,
    val groupName: String,
    val groupInitials: String,
    val groupColor: AvatarColor,
    // Why: who the person is in that group, and who this device's user is, the two ends of its payment.
    val memberId: String,
    val meMemberId: String,
    // Why: positive when they owe you in this group, negative when you owe them.
    val amountMinor: Long,
)
