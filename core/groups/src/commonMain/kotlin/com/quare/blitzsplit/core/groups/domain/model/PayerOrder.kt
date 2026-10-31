package com.quare.blitzsplit.core.groups.domain.model

/*
 * Why: "Você e Ana pagaram": Conta, Despesas and Dividir name whoever paid a bill alike, this device's user first,
 * then in the order they were added to the group, then whoever left it, in the bill's order.
 */
class PayerOrder(
    private val meId: String?,
    // Why: the group's members in the order they were added.
    memberIds: List<String>,
) {
    constructor(members: List<GroupMember>) : this(
        meId = members.firstOrNull { it.isMe }?.id,
        memberIds = members.map { it.id },
    )

    private val positionById = memberIds.withIndex().associate { (position, id) -> id to position }

    fun sort(payerIds: List<String>): List<String> = payerIds
        .distinct()
        .sortedWith(
            compareBy<String> { it != meId }
                .thenBy { positionById[it] ?: Int.MAX_VALUE },
        )
}
