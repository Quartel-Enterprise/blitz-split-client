package com.quare.blitzsplit.feature.billdetail.presentation.model

// Why: the line under "Sua parte", saying who settles it with whom.
sealed interface MyPartNote {
    /*
     * Why: "Entra no que você deve a Bruno em Saldos.", or "a Ana e Bruno" when the bill puts what this device's user
     * owes on several payers. Someone no longer in the group has no name, and each UI calls them "Ex-membro".
     */
    data class OwesPayers(
        val names: List<String?>,
    ) : MyPartNote

    /*
     * Why: "Ana, Bruno, Carla e Elisa te devem R$ 62,40 cada." when they owe the same, "... te devem R$ 392,00 no
     * total." otherwise. Someone no longer in the group has no name, and each UI calls them "Ex-membro".
     */
    data class OwedBy(
        val names: List<String?>,
        val eachMinor: Long?,
        val totalMinor: Long,
    ) : MyPartNote
}
