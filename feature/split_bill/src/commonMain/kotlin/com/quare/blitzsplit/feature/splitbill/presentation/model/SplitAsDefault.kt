package com.quare.blitzsplit.feature.splitbill.presentation.model

/*
 * Why: "Usar como padrão do grupo" was checked on Dividir's "Pronto" (beta feedback 11): saving the bill makes its
 * split the group's "Divisão padrão", or the exception of the category it opened with.
 */
data class SplitAsDefault(
    // Why: the category whose exception it replaces ("para Mercado"); null for the group's own default.
    val exceptionCategoryId: String?,
)
