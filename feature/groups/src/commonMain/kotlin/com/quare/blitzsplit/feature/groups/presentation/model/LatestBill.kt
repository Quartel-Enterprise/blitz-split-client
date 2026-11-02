package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.GroupCurrency

/*
 * Why: "Última atividade" on the Flip's cover screen, "Ana lançou Uber aeroporto, R$ 87,60, em Viagem Floripa": the
 * bill saved last on the device, in any group.
 */
data class LatestBill(
    val description: String,
    val amountMinor: Long,
    val currency: GroupCurrency,
    val groupName: String,
    val savedBy: LatestBillAuthor,
)
