package com.quare.blitzsplit.core.activity.domain.model

// Why: "Valor R$ 612,00 → R$ 642,00", one row of an edit, the same rows as the bill's history (7.3).
data class ActivityChange(
    val field: ActivityField,
    val before: ActivityValue,
    val after: ActivityValue,
)
