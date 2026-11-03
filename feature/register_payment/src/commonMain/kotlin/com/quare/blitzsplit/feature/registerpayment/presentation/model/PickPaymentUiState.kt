package com.quare.blitzsplit.feature.registerpayment.presentation.model

// Why: "Quem pagou a quem?", Início's "Registrar pagamento" before the form: the open pairs of every group.
data class PickPaymentUiState(
    /*
     * Why: nothing is drawn until Room is read, so "Ninguém deve nada" never flashes before the pairs; while no pair
     * came, also until the account's copy on this device is whole (its first full sync may still bring some).
     */
    val isLoaded: Boolean,
    // Why: "Sem internet. Seus dados aparecem quando reconectar." under the shimmer while that sync waits offline.
    val isWaitingForInternet: Boolean,
    // Why: the groups with an open pair, the most recently used first.
    val groups: List<PickPaymentGroup>,
) {
    // Why: "Ninguém deve nada agora", with no pair to pick, only once that is concrete.
    val showsEmpty: Boolean
        get() = isLoaded && groups.isEmpty()
}
