package com.quare.blitzsplit.core.sync.domain.usecase

fun interface DeleteAccount {
    suspend operator fun invoke(): Result<Unit>
}
