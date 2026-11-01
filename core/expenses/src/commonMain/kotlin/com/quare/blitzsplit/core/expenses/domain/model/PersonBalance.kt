package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.model.GroupCurrency
import kotlin.math.abs

/*
 * Why: someone in the global Saldos, netted across every group of one currency: what they owe you in one group
 * offsets what you owe them in another, so each person shows once.
 */
data class PersonBalance(
    // Why: the same person in every group, as PeopleBalances.keyOf makes it; with the currency, what Route.SettleUp opens.
    val key: String,
    // Why: as the group they were last active in names them; null for someone no longer in the group ("Ex-membro").
    val name: String?,
    val face: GroupFace?,
    val currency: GroupCurrency,
    // Why: positive when they owe you ("te deve"), negative when you owe them ("você deve").
    val netMinor: Long,
    // Why: "Por grupo", each group with an open balance between you two, the most recently active first.
    val groups: List<PersonGroupBalance>,
    // Why: nobody joined as them in any of these groups: "convidado", who only learns about it from Cobrar's message.
    val isGuest: Boolean,
    // Why: a guest's message says what went into the bill: the bills they have a part in, the most recent first.
    val guestBills: List<String>,
    /*
     * Why: "menos os R$ 182,50 que você já pagou", only while their parts minus what they paid is what they owe you,
     * so the message never states numbers that don't add up; null otherwise.
     */
    val guestPaidMinor: Long?,
) {
    // Why: "Acertar", one payment in each group, from whoever owes in that group to the other.
    val legs: List<SettlementLeg>
        get() = groups.map { part ->
            SettlementLeg(
                groupId = part.groupId,
                fromMemberId = if (part.amountMinor > 0) part.memberId else part.meMemberId,
                toMemberId = if (part.amountMinor > 0) part.meMemberId else part.memberId,
                amountMinor = abs(part.amountMinor),
            )
        }
}
