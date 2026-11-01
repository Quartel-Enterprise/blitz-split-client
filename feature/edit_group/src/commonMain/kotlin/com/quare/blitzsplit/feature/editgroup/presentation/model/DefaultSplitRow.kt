package com.quare.blitzsplit.feature.editgroup.presentation.model

/*
 * Why: what one person puts in the default split as it is now, worked out once per state for every UI: the weight it
 * saves, their part of every bill in hundredths of a percent ("16,7%", "75% de cada conta") and their part of the
 * example bill ("Pierre R$ 75,00").
 */
data class DefaultSplitRow(
    val person: DefaultSplitPerson,
    val weight: Long,
    // Why: of 10.000; for "Porcentagem" and "Pela renda" the percentage itself, even while they don't add up to 100.
    val basisPoints: Long,
    // Why: in the example bill's minor unit; null while the split has nobody in it.
    val exampleMinor: Long?,
)
