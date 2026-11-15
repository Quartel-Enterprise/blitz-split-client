package com.quare.blitzsplit.core.expenses.customcolor

import com.quare.blitzsplit.core.expenses.domain.model.Category
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethod

/*
 * Why: what keeps one of the person's colors from being deleted ("Food uses this color"): a main category, or one of
 * their own payment methods, whose card picks from the same colors. Each UI names it: the category's name, or the
 * method's (its type's when it has none).
 */
sealed interface CustomColorUser {
    data class OfCategory(
        val category: Category,
    ) : CustomColorUser

    data class OfPayingMethod(
        val method: PayingMethod,
    ) : CustomColorUser
}
