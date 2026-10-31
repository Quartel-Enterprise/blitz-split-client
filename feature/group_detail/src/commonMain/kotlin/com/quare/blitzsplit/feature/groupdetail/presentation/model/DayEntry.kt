package com.quare.blitzsplit.feature.groupdetail.presentation.model

// Why: a line of Despesas do grupo: a bill (ExpenseRow) or a payment that settled a debt (PaymentRow).
sealed interface DayEntry {
    val id: String
}
