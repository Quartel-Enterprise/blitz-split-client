package com.quare.blitzsplit.core.contacts.domain.usecase

fun interface SetContactHidden {
    // Why: "Ocultar de Contatos" (only at zero) or "Mostrar".
    suspend operator fun invoke(
        personKey: String,
        isHidden: Boolean,
    ): Result<Unit>
}
