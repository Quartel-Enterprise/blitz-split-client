package com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.usecase.ConsumeCategoryTeachOffer
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveCategoryMap
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveCategoryTeachOffer
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveMapCategories
import com.quare.blitzsplit.core.expenses.domain.usecase.SaveCategoryTerm

// Why: what "Ensinar ao mapa?" (9.3e) needs, in one parameter of Despesas do grupo's ViewModel.
class CategoryTeachUseCases(
    val observeOffer: ObserveCategoryTeachOffer,
    val consumeOffer: ConsumeCategoryTeachOffer,
    val observeMap: ObserveCategoryMap,
    // Why: every group's categories, as Mapa de categorias reads them, for where a word of the title goes now.
    val observeMapCategories: ObserveMapCategories,
    val saveTerm: SaveCategoryTerm,
)
