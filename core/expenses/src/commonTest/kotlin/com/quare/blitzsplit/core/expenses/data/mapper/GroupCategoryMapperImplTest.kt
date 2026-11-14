package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.domain.model.CategoryColor
import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTint
import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import com.quare.blitzsplit.core.expenses.domain.model.tint
import com.quare.blitzsplit.core.provider.room.entity.GroupCategoryEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GroupCategoryMapperImplTest {
    private val mapper = GroupCategoryMapperImpl()

    @Test
    fun `GIVEN a stored hex color WHEN mapping to the domain THEN reads it as a custom color`() {
        // Given
        val entity = entity(color = "#3A7BD5")

        // When
        val category = mapper.toDomain(entity)

        // Then
        assertEquals(CategoryTint.Custom(CustomColor(0x3A7BD5)), category.color)
    }

    @Test
    fun `GIVEN a stored named key WHEN mapping to the domain THEN reads it as that named color`() {
        // Given
        val entity = entity(color = "teal")

        // When
        val category = mapper.toDomain(entity)

        // Then
        assertEquals(CategoryColor.TEAL.tint, category.color)
    }

    @Test
    fun `GIVEN a color code this version does not know WHEN mapping to the domain THEN reads as none picked`() {
        // Given
        val entities = listOf(entity(color = "violet"), entity(color = "3A7BD5"), entity(color = "#FA0"))

        // When
        val colors = entities.map { mapper.toDomain(it).color }

        // Then
        colors.forEach { assertNull(it) }
    }

    @Test
    fun `GIVEN a category in a custom color WHEN storing it THEN keeps its uppercase hex`() {
        // Given
        val category = GroupCategory(
            id = "pet",
            groupId = "group-1",
            parentId = null,
            name = "Pet",
            icon = CategoryIcon.PAW,
            color = CustomColor(0x3a7bd5).tint,
            createdByMemberId = "ana",
            isDeleted = false,
            isPending = true,
        )

        // When
        val entity = mapper.toEntity(category, createdAt = 1_000L)

        // Then
        assertEquals("#3A7BD5", entity.color)
    }

    private fun entity(color: String?): GroupCategoryEntity = GroupCategoryEntity(
        id = "pet",
        groupId = "group-1",
        createdBy = "user-1",
        createdByMemberId = "ana",
        parentId = null,
        name = "Pet",
        icon = "paw",
        color = color,
        isDeleted = false,
        addedAt = 1_000L,
        updatedAt = 2_000L,
        pendingSync = false,
    )
}
