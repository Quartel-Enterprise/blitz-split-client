package com.quare.blitzsplit.feature.groups.presentation.model

// Why: how Início's "Saldos com pessoas" draws its people, picked by HomePeopleLayout.
enum class HomePeopleMode {
    // Why: a compact row per person: face, name, then the amount with its sign and word on the same line.
    LIST,

    // Why: fixed-width cards that scroll sideways, everyone in them.
    CAROUSEL,

    // Why: fixed-width cards aligned to the start, wrapping onto at most MAX_CARD_LINES lines.
    CARDS,
}
