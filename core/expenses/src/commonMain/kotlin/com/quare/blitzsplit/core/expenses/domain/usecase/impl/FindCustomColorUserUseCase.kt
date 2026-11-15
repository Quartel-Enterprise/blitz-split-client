package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.customcolor.CustomColorUser
import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import com.quare.blitzsplit.core.expenses.domain.model.tint
import com.quare.blitzsplit.core.expenses.domain.usecase.FindCustomColorUser
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveMapCategories
import com.quare.blitzsplit.core.expenses.domain.usecase.ObservePayingMethods
import kotlinx.coroutines.flow.first

/*
 * Why: the categories the map sees are the ones the person can see: the app's with this device's colors and every group's
 * on the device. A subcategory shows its main category's color, so only main categories count. Then the person's own
 * payment methods, whose cards pick from the same colors.
 */
class FindCustomColorUserUseCase(
    private val observeMapCategories: ObserveMapCategories,
    private val observePayingMethods: ObservePayingMethods,
) : FindCustomColorUser {
    override suspend fun invoke(color: CustomColor): CustomColorUser? {
        val tint = color.tint
        val category = observeMapCategories()
            .first()
            .branches
            .map { it.category }
            .firstOrNull { it.color == tint }
        if (category != null) return CustomColorUser.OfCategory(category)
        return observePayingMethods()
            .first()
            .firstOrNull { it.cardColor == tint }
            ?.let(CustomColorUser::OfPayingMethod)
    }
}
