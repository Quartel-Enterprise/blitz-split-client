package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
import com.quare.blitzsplit.core.expenses.domain.model.CategoryRules
import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import com.quare.blitzsplit.core.expenses.domain.repository.CategoryRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.SaveGroupCategory
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import kotlinx.coroutines.flow.first
import kotlin.uuid.Uuid

// Why: on the device right away, offline included; the group sees it once synced (7.8d).
class SaveGroupCategoryUseCase(
    private val repository: CategoryRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : SaveGroupCategory {
    override suspend fun invoke(
        categoryId: String?,
        groupId: String,
        name: String,
        parentId: String?,
        icon: CategoryIcon,
        createdByMemberId: String?,
    ): Result<String> {
        val trimmed = CategoryRules.toCapped(name.trim()).trimEnd()
        if (!CategoryRules.isNameValid(trimmed)) return Result.failure(IllegalArgumentException("Invalid name"))
        if (parentId != null && !canBeUnder(categoryId, groupId, parentId)) {
            return Result.failure(IllegalArgumentException("Invalid parent"))
        }
        val iconCode = icon.code.takeIf { parentId == null }
        val now = currentTimeMillis()
        if (categoryId != null) {
            return repository
                .updateGroupCategory(
                    id = categoryId,
                    name = trimmed,
                    parentId = parentId,
                    icon = iconCode,
                    updatedAt = now,
                ).map { categoryId }
        }
        val id = Uuid.random().toString()
        return repository
            .createGroupCategory(
                category = GroupCategory(
                    id = id,
                    groupId = groupId,
                    parentId = parentId,
                    name = trimmed,
                    icon = icon.takeIf { parentId == null },
                    createdByMemberId = createdByMemberId,
                    isDeleted = false,
                    isPending = true,
                ),
                createdAt = now,
            ).map { id }
    }

    /*
     * Why: two levels only. A subcategory goes under a live main category other than Outros (the app's or the
     * group's), and a category with subcategories stays a main one, or they would sit at a third level and read as
     * Outros.
     */
    private suspend fun canBeUnder(
        categoryId: String?,
        groupId: String,
        parentId: String,
    ): Boolean {
        val live = repository.observeGroupCategories(groupId).first().filterNot { it.isDeleted }
        if (categoryId != null && live.any { it.parentId == categoryId }) return false
        val appParent = AppCategory.fromKeyOrNull(parentId)
        return if (appParent != null) {
            appParent.isMain && appParent != AppCategory.OTHER
        } else {
            live.any { it.id == parentId && it.parentId == null && it.id != categoryId }
        }
    }
}
