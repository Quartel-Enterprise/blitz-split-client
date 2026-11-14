package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.Category
import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import com.quare.blitzsplit.core.expenses.domain.model.tint
import com.quare.blitzsplit.core.expenses.domain.usecase.FindCustomColorUser
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveMapCategories
import kotlinx.coroutines.flow.first

/*
 * Why: the categories the map sees are the ones the person can see: the app's with this device's colors and every group's
 * on the device. A subcategory shows its main category's color, so only main categories count.
 */
class FindCustomColorUserUseCase(
    private val observeMapCategories: ObserveMapCategories,
) : FindCustomColorUser {
    override suspend fun invoke(color: CustomColor): Category? = observeMapCategories()
        .first()
        .branches
        .map { it.category }
        .firstOrNull { it.color == color.tint }
}
