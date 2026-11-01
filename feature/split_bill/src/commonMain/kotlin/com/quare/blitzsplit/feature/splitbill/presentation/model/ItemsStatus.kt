package com.quare.blitzsplit.feature.splitbill.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.BillItem

// Why: whether the items close with the bill's total (7.5), the card above "Quanto fica para cada um".
sealed interface ItemsStatus {
    /*
     * Why: "2 itens sem ninguém": "Caipirinha e Água com gás ainda não têm dono: R$ 80,00 fora da divisão.", or, when
     * no item has anyone yet (a receipt just read, 7.5b), "Toque em cada item e marque quem consumiu."
     */
    data class Loose(
        val items: List<BillItem>,
        val isEveryItem: Boolean,
        val looseMinor: Long,
    ) : ItemsStatus

    // Why: "Faltam R$ 15,00": "Itens, serviço e couvert somam R$ 627,00, mas o cupom diz R$ 642,00." (7.5d)
    data class AmountLeft(
        val amountMinor: Long,
        val sumMinor: Long,
        val totalMinor: Long,
        val isFromReceipt: Boolean,
    ) : ItemsStatus

    // Why: "Passou R$ 15,00", the same sentence under it.
    data class AmountOver(
        val amountMinor: Long,
        val sumMinor: Long,
        val totalMinor: Long,
        val isFromReceipt: Boolean,
    ) : ItemsStatus

    // Why: "Fechou: R$ 642,00": "R$ 570,00 em itens, R$ 57,00 de serviço e R$ 15,00 de couvert."
    data class AddsUp(
        val totalMinor: Long,
        val itemsMinor: Long,
        val serviceMinor: Long,
        val couvertMinor: Long,
        val extraMinor: Long,
    ) : ItemsStatus
}
