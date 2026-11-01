package com.quare.blitzsplit.core.groups.domain.model

import com.quare.blitzsplit.core.model.DefaultSplitMode

/*
 * Why: a category whose new bills split another way than the group's default (6.7f): "Mercado" equally in a home that
 * splits 3 to 1. "Igual", "Partes" or "Porcentagem", with weights as the default keeps them.
 */
data class CategorySplit(
    // Why: an app category's key or a group category's id.
    val categoryId: String,
    val mode: DefaultSplitMode,
    val weights: Map<String, Long>,
) {
    // Why: the split a bill of this category opens with.
    val asDefault: GroupDefaultSplit
        get() = GroupDefaultSplit(
            mode = mode,
            weights = weights,
            exceptions = emptyList(),
        )

    companion object {
        // Why: the ways an exception may split; "Pela renda" stays the default's alone, as the board offers.
        val modes: List<DefaultSplitMode> =
            listOf(DefaultSplitMode.EQUAL, DefaultSplitMode.SHARES, DefaultSplitMode.PERCENT)
    }
}
