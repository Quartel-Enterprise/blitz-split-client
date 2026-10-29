package com.quare.blitzsplit.core.sync.domain.usecase

// Why: "Desconectar Google/Apple": the same user goes on anonymous, with a new recovery code.
fun interface UnlinkAccount {
    suspend operator fun invoke(): Result<Unit>
}
