package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.customcolor.CustomColorUseCases
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteGroupCategory
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveCategoryMap
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveCategoryTree
import com.quare.blitzsplit.core.expenses.domain.usecase.SaveGroupCategory

// Why: what "Categoria" (7.7) and the group's own categories (7.8) need, in one parameter of Rachar conta's ViewModel.
data class BillCategoryUseCases(
    val observeCategoryTree: ObserveCategoryTree,
    val observeCategoryMap: ObserveCategoryMap,
    val saveGroupCategory: SaveGroupCategory,
    val deleteGroupCategory: DeleteGroupCategory,
    // Why: the person's own colors in the form's "Color".
    val customColors: CustomColorUseCases,
)
