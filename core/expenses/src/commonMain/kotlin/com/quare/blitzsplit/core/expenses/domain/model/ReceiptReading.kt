package com.quare.blitzsplit.core.expenses.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

// Why: what "Ler cupom fiscal" (7.1e) read from a receipt's photo, which Rachar conta fills the bill and Itens with.
data class ReceiptReading(
    // Why: what was paid, in the group currency's minor unit.
    val totalMinor: Long,
    // Why: the place, "Supermercado Bom Preço", which becomes the bill's description; null when not printed.
    val description: String?,
    // Why: one of the app's category keys, suggested by the place; null when none fits.
    val categoryId: String?,
    val date: LocalDate?,
    // Why: printed beside the date, null without one.
    val time: LocalTime?,
    // Why: in the receipt's order; empty for one that lists none (a card slip).
    val items: List<ReceiptItem>,
    // Why: "Serviço (10%)" over the items, 0 for none.
    val servicePercent: Long,
)
