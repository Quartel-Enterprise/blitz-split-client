package com.quare.blitzsplit.core.expenses.domain.model

// Why: what the person changed in their category map (flow 9), on top of the app's 60 terms.
data class CategoryTermChange(
    // Why: the term as it is matched (CategoryMatcher.keyOf), one change per word.
    val key: String,
    val term: String,
    // Why: where it sends a title now; null for a term removed.
    val categoryId: String?,
    // Why: "Restaurar padrão" undid it, so the app's term (if any) counts again.
    val isRestored: Boolean,
    val isPending: Boolean,
)
