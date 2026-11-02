package com.quare.blitzsplit.core.expenses.domain.usecase

fun interface SaveCategoryTerm {
    /*
     * Why: "Novo termo", "Editar termo" (renaming takes the old word out, replacedTerm) and "Mover" (9.2c): a term
     * has one category, so saving a word that exists moves it. Also "Ensinar" (9.3f).
     */
    suspend operator fun invoke(
        term: String,
        categoryId: String,
        replacedTerm: String?,
    ): Result<Unit>
}
