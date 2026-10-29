package com.quare.blitzsplit.core.account.domain.usecase

fun interface UnlinkAccount {
    suspend operator fun invoke(): Result<Unit>
}
