package com.quare.blitzsplit.core.sync.domain.usecase

fun interface CancelSignOutWhenSynced {
    suspend operator fun invoke()
}
