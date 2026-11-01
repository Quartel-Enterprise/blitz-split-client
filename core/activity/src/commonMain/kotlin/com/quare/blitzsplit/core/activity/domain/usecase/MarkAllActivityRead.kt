package com.quare.blitzsplit.core.activity.domain.usecase

// Why: "Marcar tudo como lido", every event the device knows of, the older pages loaded included.
fun interface MarkAllActivityRead {
    suspend operator fun invoke(): Result<Unit>
}
