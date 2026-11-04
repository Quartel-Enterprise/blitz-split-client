package com.quare.blitzsplit.core.expenses.domain.usecase

// Why: "Simplificar dívidas" (13.3), turned on or off at once by any member, offline too, apart from Salvar.
fun interface SetSimplifiesDebts {
    suspend operator fun invoke(
        groupId: String,
        simplifiesDebts: Boolean,
    ): Result<Unit>
}
