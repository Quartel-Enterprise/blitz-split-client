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
    // Why: "Dívidas simplificadas" on the group's line of "Por grupo" (13.6).
    val isSimplified: Boolean,
    /*
     * Why: whom whoever pays this pair owed by the bills instead, when the simplification made or grew it, by name:
     * Cobrar explains it (13.6b); empty otherwise, and for someone no longer in the group.
     */
    val insteadOfNames: List<String>,
)
