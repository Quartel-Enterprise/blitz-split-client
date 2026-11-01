package com.quare.blitzsplit.core.expenses.domain.model

// Why: amounts are whole numbers of the minor unit of the group's currency (centavos for BRL).
data class Expense(
    val id: String,
    val groupId: String,
    val description: String,
    val amountMinor: Long,
    val splitMode: SplitMode,
    // Why: who paid and how much, adding up to the total.
    val payers: List<ExpensePayer>,
    // Why: each member in the split with the part the split gave them, adding up to the total.
    val shares: List<ExpenseShare>,
    // Why: "Itens da conta" the shares come from, null for any other split.
    val items: BillItems?,
    // Why: "Quando", in epoch millis.
    val spentAt: Long,
    // Why: created or changed on this device and not up on the server yet ("na fila").
    val isPending: Boolean,
    // Why: the Last-Write-Wins version; a newer one while "Editar conta" is open means someone saved meanwhile.
    val updatedAt: Long,
    // Why: the member who last saved it, as the server recorded; null for one only this device changed.
    val updatedByMemberId: String?,
) {
    /*
     * Why: what the bill means for a member, what they paid minus their part: positive when the others owe them
     * ("a receber"), negative when they owe ("você deve"). Null when they neither paid nor are in the split.
     */
    fun balanceOf(memberId: String): Long? {
        val isInBill = payers.any { it.memberId == memberId } || shares.any { it.memberId == memberId }
        if (!isInBill) return null
        val paid = payers.filter { it.memberId == memberId }.sumOf { it.amountMinor }
        val owed = shares.filter { it.memberId == memberId }.sumOf { it.amountMinor }
        return paid - owed
    }
}
