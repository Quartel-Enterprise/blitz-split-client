package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.CategoryForm
import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
import com.quare.blitzsplit.core.expenses.domain.usecase.SaveGroupCategory
import com.quare.blitzsplit.core.expenses.domain.usecase.invoke
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SaveGroupCategoryFormTest {
    private val calls = mutableListOf<List<Any?>>()
    private val save = SaveGroupCategory { categoryId, groupId, name, parentId, icon, createdByMemberId ->
        calls += listOf(categoryId, groupId, name, parentId, icon, createdByMemberId)
        Result.success(categoryId ?: "new-id")
    }

    @Test
    fun `GIVEN a new subcategory form WHEN saving it THEN passes its fields created by this member`() = runTest {
        // Given
        val form = form(
            categoryId = null,
            createdByMemberId = null,
        )

        // When
        val result = save(form, meId = "me")

        // Then
        assertEquals("new-id", result.getOrThrow())
        assertEquals(
            listOf<List<Any?>>(listOf(null, "group-2", "Feira", AppCategory.FOOD.key, CategoryIcon.FOOD, "me")),
            calls,
        )
    }

    @Test
    fun `GIVEN a category someone else created WHEN saving its edit THEN keeps who created it`() = runTest {
        // Given
        val form = form(
            categoryId = "feira",
            createdByMemberId = "ana",
        )

        // When
        save(form, meId = "me")

        // Then
        assertEquals(
            listOf<List<Any?>>(listOf("feira", "group-2", "Feira", AppCategory.FOOD.key, CategoryIcon.FOOD, "ana")),
            calls,
        )
    }

    @Test
    fun `GIVEN a new form and no member for this device WHEN saving it THEN nobody is its creator`() = runTest {
        // Given
        val form = form(
            categoryId = null,
            createdByMemberId = null,
        )

        // When
        save(form, meId = null)

        // Then
        assertEquals(null, calls.single().last())
    }

    private fun form(
        categoryId: String?,
        createdByMemberId: String?,
    ): CategoryForm = CategoryForm(
        categoryId = categoryId,
        groupId = "group-2",
        name = "Feira",
        parentId = AppCategory.FOOD.key,
        icon = CategoryIcon.FOOD,
        createdByMemberId = createdByMemberId,
        isSaving = false,
    )
}
