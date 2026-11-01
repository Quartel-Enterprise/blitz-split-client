package com.quare.blitzsplit.core.expenses.domain.usecase

fun interface DeleteSettlement {
    // Why: "Desfazer" (every payment of a settling up, all or none) and "Apagar este acerto?".
    suspend operator fun invoke(settlementIds: List<String>): Result<Unit>
}
