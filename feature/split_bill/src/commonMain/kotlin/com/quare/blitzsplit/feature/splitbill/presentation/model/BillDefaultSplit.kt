package com.quare.blitzsplit.feature.splitbill.presentation.model

import com.quare.blitzsplit.core.model.DefaultSplitMode

/*
 * Why: the group's "Divisão padrão" (6.7) a new bill opens with, as Dividir's notice words it ("Padrão do Apê 302:
 * Partes, 3 para 1"). Each bill may still change it, only for itself.
 */
data class BillDefaultSplit(
    val mode: DefaultSplitMode,
    // Why: the split it gives the bill's people now, which "Voltar ao padrão" goes back to.
    val editor: SplitEditor,
    // Why: "3 para 1", "75% e 25%": the parts or percentages of who is in it, in the group's order.
    val values: List<Long>,
    // Why: "Igual, sem Diego": who an "Igual" default leaves out.
    val excluded: List<BillPerson>,
    /*
     * Why: the category whose exception it is ("Exceção do grupo: Mercado divide igual", 7.1k, 7.2g); null for the
     * group's own default.
     */
    val exceptionCategoryId: String?,
) {
    // Why: the same way to split with the same weights, whatever the total.
    fun isFollowedBy(split: SplitEditor): Boolean = split.mode == editor.mode &&
        split.entries.map { it.memberId to it.value } == editor.entries.map { it.memberId to it.value }
}
