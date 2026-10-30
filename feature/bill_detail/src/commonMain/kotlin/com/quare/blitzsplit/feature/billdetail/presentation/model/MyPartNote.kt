package com.quare.blitzsplit.feature.billdetail.presentation.model

// Why: the line under "Sua parte", saying who settles it with whom.
sealed interface MyPartNote {
    // Why: "Entra no que você deve a Bruno em Saldos."
    data class OwesPayer(
        val payerName: String,
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
