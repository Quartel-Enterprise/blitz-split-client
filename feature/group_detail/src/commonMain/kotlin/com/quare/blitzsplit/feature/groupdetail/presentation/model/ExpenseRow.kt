package com.quare.blitzsplit.feature.groupdetail.presentation.model

// Why: "Jantar no Ostradamus · 21:30 · Bruno pagou · R$ 642,00 · −R$ 118,00".
data class ExpenseRow(
    override val id: String,
    val description: String,
    // Why: each UI writes the time of day.
    val spentAt: Long,
    // Why: null when whoever paid is no longer in the group.
    val payerName: String?,
    // Why: "Você pagou".
    val payerIsMe: Boolean,
    val totalMinor: Long,
    // Why: what the bill does to this device's user's balance, "+R$ 600,00" or "−R$ 118,00"; null when not in it.
    val myBalanceMinor: Long?,
    // Why: "na fila" and the orange dot: saved here and not on the server yet.
    val isPending: Boolean,
) : DayEntry
