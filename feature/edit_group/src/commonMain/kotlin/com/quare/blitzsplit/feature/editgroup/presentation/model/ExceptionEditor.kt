package com.quare.blitzsplit.feature.editgroup.presentation.model

import com.quare.blitzsplit.core.groups.domain.model.GroupDefaultSplit
import com.quare.blitzsplit.core.model.DefaultSplitMode

/*
 * Why: "Exceção por categoria" (6.7f), open over "Divisão padrão" while not null: one category and its own way to split
 * ("Igual", "Partes", "Porcentagem"), with the same people rows as the default. It changes the default only on Salvar.
 */
data class ExceptionEditor(
    // Why: the exception being changed; null for "Adicionar exceção".
    val editedCategoryId: String?,
    // Why: null until a category is picked.
    val categoryId: String?,
    // Why: the main category whose subcategories show, to pick "Mercado" inside "Alimentação".
    val openMainId: String?,
    val mode: DefaultSplitMode,
    val people: List<DefaultSplitPerson>,
    // Why: what the rows give, worked out once per change.
    val figures: DefaultSplitFigures,
) {
    val includedCount: Int
        get() = figures.weights.values.count { it > 0 }

    val percentSum: Long
        get() = if (mode == DefaultSplitMode.PERCENT) figures.weights.values.sum() else GroupDefaultSplit.FULL_PERCENT

    // Why: "A soma dá 90%. Faltam 10%."
    val remainingPercent: Long
        get() = GroupDefaultSplit.FULL_PERCENT - percentSum

    val addsUp: Boolean
        get() = includedCount > 0 && percentSum == GroupDefaultSplit.FULL_PERCENT

    val showsPercentError: Boolean
        get() = mode == DefaultSplitMode.PERCENT && percentSum != GroupDefaultSplit.FULL_PERCENT
}
