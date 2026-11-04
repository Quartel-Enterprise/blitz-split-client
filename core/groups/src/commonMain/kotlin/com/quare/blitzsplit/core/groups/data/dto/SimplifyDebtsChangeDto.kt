package com.quare.blitzsplit.core.groups.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Why: groups.simplify_debts_change, which the server copies into the event of Atividade when the switch changed.
@Serializable
data class SimplifyDebtsChangeDto(
    @SerialName("before") val before: List<SimplifyDebtsPairDto>,
    @SerialName("after") val after: List<SimplifyDebtsPairDto>,
)
