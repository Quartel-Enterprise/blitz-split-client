package com.quare.blitzsplit.feature.activity.presentation.model

import com.quare.blitzsplit.core.activity.domain.model.ActivityField

// Why: "Valor  R$ 612,00 → R$ 642,00", the same rows as the bill's history (7.3).
data class ActivityChangeRow(
    val field: ActivityField,
    val before: ActivityChangeValue,
    val after: ActivityChangeValue,
)
