package com.quare.blitzsplit.core.account.domain.usecase

fun interface RestoreWithRecoveryCode {
    suspend operator fun invoke(code: String): Result<Unit>
}
