package com.quare.blitzsplit.core.sync.domain.usecase

fun interface SignOut {
    suspend operator fun invoke(): Result<Unit>
}
