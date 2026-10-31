package com.quare.blitzsplit.core.groups.domain.model

/*
 * Why: "Você e Ana pagaram": Conta, Despesas and Dividir name whoever paid a bill alike, this device's user first,
 * then in the order they were added to the group, then whoever left it, in the bill's order.
 */
class PayerOrder(
    members: List<GroupMember>,
) {
    private val meId = members.firstOrNull { it.isMe }?.id
    private val positionById = members.withIndex().associate { (position, member) -> member.id to position }

    fun sort(payerIds: List<String>): List<String> = payerIds
        .distinct()
        .sortedWith(
            compareBy<String> { it != meId }
                .thenBy { positionById[it] ?: Int.MAX_VALUE },
        )
}
