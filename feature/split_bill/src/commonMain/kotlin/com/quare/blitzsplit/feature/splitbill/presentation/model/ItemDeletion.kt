package com.quare.blitzsplit.feature.splitbill.presentation.model

/*
 * Why: "Apagar este item?" (7.6h): "“Cerveja long neck” (R$ 84,00) sai da conta e o serviço é refeito. Os itens
 * deixam de fechar com o total: falta dividir R$ 92,40."
 */
data class ItemDeletion(
    val itemId: String,
    val name: String,
    val totalMinor: Long,
    // Why: "e o serviço é refeito", while the bill has service.
    val redoesService: Boolean,
    // Why: what the items fall short of the total once it goes; null while they don't close either way.
    val leftMinor: Long?,
)
