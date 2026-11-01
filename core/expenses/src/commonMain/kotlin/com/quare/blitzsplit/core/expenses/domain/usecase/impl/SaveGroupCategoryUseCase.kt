package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
import com.quare.blitzsplit.core.expenses.domain.model.CategoryRules
import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import com.quare.blitzsplit.core.expenses.domain.repository.CategoryRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.SaveGroupCategory
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
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
}
