package com.quare.blitzsplit.core.expenses.domain.usecase

fun interface DeleteSettlement {
    // Why: "Desfazer" and "Apagar este acerto?".
    suspend operator fun invoke(settlementId: String): Result<Unit>
}
