package com.quare.blitzsplit.core.sync.domain.usecase

// Why: "Exportar meus dados", the LGPD's portability: a JSON of what this device holds, offline too.
fun interface ExportUserData {
    suspend operator fun invoke(): String
}
