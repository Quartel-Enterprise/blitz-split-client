package com.quare.blitzsplit.core.activity.domain.usecase

fun interface LoadBillHistory {
    suspend operator fun invoke(billId: String): Result<Unit>
}
