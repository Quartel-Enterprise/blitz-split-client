package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.CategoryColor

fun interface SetAppCategoryColor {
    /*
     * Why: "Cor da categoria" on one of the app's main categories (7.7), on this device only, offline included.
     * A null color brings the app's default back; a key that isn't an app main category fails.
     */
    suspend operator fun invoke(
        categoryKey: String,
        color: CategoryColor?,
    ): Result<Unit>
}
