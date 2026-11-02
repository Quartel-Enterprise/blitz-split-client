package com.quare.blitzsplit.core.expenses.domain.model

// Why: a term of the person's map (9.1) as it counts now, the app's or one they created.
data class MapTerm(
    val term: String,
    val key: String,
    val categoryId: String,
    // Why: one of the 60 the app comes with: "Termo do app. Restaurar padrão traz de volta como veio."
    val isAppTerm: Boolean,
    // Why: changed on this device and not up on the server yet, the orange dot.
    val isPending: Boolean,
) {
    val asTerm: CategoryTerm
        get() = CategoryTerm(
            term = term,
            categoryId = categoryId,
        )
}
