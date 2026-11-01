package com.quare.blitzsplit.feature.registerpayment.presentation.model

// Why: "Quem pagou a quem?", Início's "Registrar pagamento" before the form: the open pairs of every group.
data class PickPaymentUiState(
    // Why: nothing is drawn until Room is read, so "Ninguém deve nada" never flashes before the pairs.
    val isLoaded: Boolean,
    // Why: the groups with an open pair, the most recently used first.
    val groups: List<PickPaymentGroup>,
) {
    // Why: "Ninguém deve nada agora", with no pair to pick.
    val showsEmpty: Boolean
        get() = isLoaded && groups.isEmpty()
}
