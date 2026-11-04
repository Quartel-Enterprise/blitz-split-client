package com.quare.blitzsplit.feature.profile.presentation.model

// Why: what "Buscar atualizações" found when there was nothing to offer; each platform says it in its own words.
enum class UpdateCheckResult {
    // Why: "Você já tem a versão mais recente."
    UP_TO_DATE,

    // Why: "A atualização já está baixando." while Google Play's download runs (Android).
    DOWNLOADING,

    // Why: "Não deu para buscar atualizações. Confira a conexão e tente de novo."
    FAILED,
}
