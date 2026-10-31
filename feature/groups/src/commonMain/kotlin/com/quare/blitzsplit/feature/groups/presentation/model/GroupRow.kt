package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency

data class GroupRow(
    val id: String,
    val name: String,
    // Why: "VF" for "Viagem Floripa", what the group's shape shows.
    val initials: String,
    val color: AvatarColor,
    // Why: up to three faces; the rest is "+N" (extraMemberCount).
    val faces: List<MemberFace>,
    val extraMemberCount: Int,
    // Why: "Só você" for one, "N membros" otherwise.
    val memberCount: Int,
    // Why: macOS's Membros column, "6 · Pierre, Ana, Bruno e mais 3": the first three names, in the group's order.
    val memberNames: List<String>,
    // Why: the orange dot and "na fila": the group, a bill or a payment changed here and not on the server yet.
    val isPending: Boolean,
    // Why: "na fila · Mercado", the latest bill waiting to go up; null when only the group or a payment waits.
    val pendingBillDescription: String?,
    val currency: GroupCurrency,
    val balanceKind: GroupBalanceKind,
    // Why: your net in the group, "+R$ 214,90" or "−R$ 98,00", and what makes it up when you get and owe at once.
    val netMinor: Long,
    val receivesMinor: Long,
    val owesMinor: Long,
    val lastActivity: LastActivity,
) {
    val isSettled: Boolean
        get() = balanceKind == GroupBalanceKind.EVERYONE_SETTLED

    // Why: a name takes one line with an ellipsis; only a very long one wraps to a second line.
    val usesTwoNameLines: Boolean
        get() = name.length > LONG_NAME_LENGTH

    val detail: GroupRowDetail
        get() = when {
            isPending -> GroupRowDetail.QUEUED
            isSettled -> GroupRowDetail.EVERYONE_SETTLED
            receivesMinor > 0 && owesMinor > 0 -> GroupRowDetail.MIXED
            balanceKind == GroupBalanceKind.YOU_ARE_SETTLED -> GroupRowDetail.OTHERS_STILL_SETTLING
            balanceKind == GroupBalanceKind.NO_EXPENSES && memberCount == 1 -> GroupRowDetail.INVITE_SOMEONE
            else -> GroupRowDetail.ACTIVITY
        }

    private companion object {
        // Why: about what one line of a phone's row holds next to the balance.
        const val LONG_NAME_LENGTH = 28
    }
}
