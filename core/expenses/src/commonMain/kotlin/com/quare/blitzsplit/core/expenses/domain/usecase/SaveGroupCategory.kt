package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon

fun interface SaveGroupCategory {
    /*
     * Why: "Nova categoria" (categoryId null) or "Editar categoria" (7.8, 7.8e); the category's id. A subcategory
     * (parentId set) takes its parent's icon, so it stores none.
     */
    suspend operator fun invoke(
        categoryId: String?,
        groupId: String,
        name: String,
        parentId: String?,
        icon: CategoryIcon,
        createdByMemberId: String?,
    ): Result<String>
}
