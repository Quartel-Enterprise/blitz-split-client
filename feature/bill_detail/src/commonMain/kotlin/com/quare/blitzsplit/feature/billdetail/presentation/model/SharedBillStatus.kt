package com.quare.blitzsplit.feature.billdetail.presentation.model

// Why: what a bill's link page shows.
enum class SharedBillStatus {
    // Why: nothing yet, until the server answers.
    LOADING,
    OPEN,

    // Why: "Esse link não funciona mais": turned off, never a link, or the bill was deleted.
    NOT_FOUND,

    // Why: "Só quem está no grupo abre esta conta", with "Entrar" while not signed in.
    MEMBERS_ONLY,

    // Why: offline or the server didn't answer: "Tentar de novo".
    FAILED,
}
