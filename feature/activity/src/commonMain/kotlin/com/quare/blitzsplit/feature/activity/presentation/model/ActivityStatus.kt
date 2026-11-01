package com.quare.blitzsplit.feature.activity.presentation.model

// Why: the chip above the feed, only when it asks for attention.
enum class ActivityStatus {
    // Why: "Offline · 2 alterações na fila", with queuedCount.
    OFFLINE,

    // Why: "Sem resposta do servidor" with "Tentar de novo".
    SERVER_ERROR,

    // Why: "Sincronizando…" on a new device while the feed first comes down.
    SYNCING,
}
