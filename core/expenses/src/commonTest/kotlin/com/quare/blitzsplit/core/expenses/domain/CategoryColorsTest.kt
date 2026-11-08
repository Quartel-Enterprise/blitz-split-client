package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.CategoryColor
import com.quare.blitzsplit.core.expenses.domain.model.CategoryForm
import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.SaveGroupCategoryUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.SetAppCategoryColorUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CategoryColorsTest {
    private val repository = FakeCategoryRepository()

    @Test
    fun `GIVEN the app main categories WHEN reading their default colors THEN each has a color of its own`() {
        // Given
        val mains = AppCategory.mains

        // When
        val colors = mains.map { it.defaultColor }

        // Then
        assertEquals(mains.size, colors.toSet().size)
    }

    @Test
    fun `GIVEN a subcategory of the app WHEN reading its color THEN it is its main category color`() {
        // Given
        val tree = CategoryTree.appOnly

        // When
        val color = tree.findOrNull(AppCategory.FOOD_MARKET.key)?.color

        // Then
        assertEquals(AppCategory.FOOD.defaultColor, color)
    }

    @Test
    fun `GIVEN a color the person gave Food WHEN building the tree THEN Food and its subcategories wear it`() {
        // Given
        val appColors = mapOf(AppCategory.FOOD.key to CategoryColor.RED)

        // When
        val tree = CategoryTree.of(emptyList(), appColors)

        // Then
        assertEquals(CategoryColor.RED, tree.findOrNull(AppCategory.FOOD.key)?.color)
        assertEquals(CategoryColor.RED, tree.findOrNull(AppCategory.FOOD_BAR.key)?.color)
        assertEquals(AppCategory.HOME.defaultColor, tree.findOrNull(AppCategory.HOME.key)?.color)
    }

    @Test
    fun `GIVEN a picked group color WHEN building the tree THEN main and sub wear it`() {
        // Given
        val pet = groupCategory(
            id = "pet",
            parentId = null,
            color = CategoryColor.PINK,
        )
        val food = groupCategory(
            id = "bone",
            parentId = "pet",
            color = null,
        )

        // When
        val tree = CategoryTree.of(listOf(pet, food))

        // Then
        assertEquals(CategoryColor.PINK, tree.findOrNull("pet")?.color)
        assertEquals(CategoryColor.PINK, tree.findOrNull("bone")?.color)
    }

    @Test
    fun `GIVEN a new main category WHEN creating its form THEN it starts with an unused color`() {
        // Given
        val tree = CategoryTree.appOnly

        // When
        val form = CategoryForm.create(tree, "group-1", null, "Pet")

        // Then
        assertTrue(tree.branches.none { it.category.color == form.color })
    }

    @Test
    fun `GIVEN a subcategory form WHEN picking a color THEN it keeps the parent color`() {
        // Given
        val tree = CategoryTree.appOnly
        val form = CategoryForm.create(tree, "group-1", AppCategory.FOOD.key, "Feira")

        // When
        val picked = form.withColor(CategoryColor.RED)

        // Then
        assertEquals(AppCategory.FOOD.defaultColor, picked.color)
    }

    @Test
    fun `GIVEN a main form WHEN picking a color THEN the form takes it`() {
        // Given
        val form = CategoryForm.create(CategoryTree.appOnly, "group-1", null, "Pet")

        // When
        val picked = form.withColor(CategoryColor.BROWN)

        // Then
        assertEquals(CategoryColor.BROWN, picked.color)
    }

    @Test
    fun `GIVEN a main category WHEN saving it THEN its color is stored`() = runTest {
        // Given
        val save = SaveGroupCategoryUseCase(repository) { 1_000L }

        // When
        save(null, "group-1", "Pet", null, CategoryIcon.PAW, CategoryColor.BROWN, "me")

        // Then
        assertEquals(
            CategoryColor.BROWN,
            repository.groupCategories.value
                .single()
                .color,
        )
    }

    @Test
    fun `GIVEN a subcategory WHEN saving it THEN it stores no color of its own`() = runTest {
        // Given
        val save = SaveGroupCategoryUseCase(repository) { 1_000L }

        // When
        save(null, "group-1", "Feira", AppCategory.FOOD.key, CategoryIcon.PAW, CategoryColor.BROWN, "me")

        // Then
        assertNull(
            repository.groupCategories.value
                .single()
                .color,
        )
    }

    @Test
    fun `GIVEN an app main category WHEN setting another color THEN it is kept for the person`() = runTest {
        // Given
        val setColor = SetAppCategoryColorUseCase(repository)

        // When
        val result = setColor(AppCategory.FOOD.key, CategoryColor.RED)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mapOf(AppCategory.FOOD.key to CategoryColor.RED), repository.appColors.value)
    }

    @Test
    fun `GIVEN a changed color WHEN setting the app default back THEN nothing is kept`() = runTest {
        // Given
        val setColor = SetAppCategoryColorUseCase(repository)
        setColor(AppCategory.FOOD.key, CategoryColor.RED)

        // When
        setColor(AppCategory.FOOD.key, AppCategory.FOOD.defaultColor)

        // Then
        assertTrue(repository.appColors.value.isEmpty())
    }

    @Test
    fun `GIVEN a changed color WHEN restoring it THEN nothing is kept`() = runTest {
        // Given
        val setColor = SetAppCategoryColorUseCase(repository)
        setColor(AppCategory.HOME.key, CategoryColor.LIME)

        // When
        setColor(AppCategory.HOME.key, null)

        // Then
        assertTrue(repository.appColors.value.isEmpty())
    }

    @Test
    fun `GIVEN a subcategory key or an unknown key WHEN setting a color THEN it fails`() = runTest {
        // Given
        val setColor = SetAppCategoryColorUseCase(repository)

        // When
        val sub = setColor(AppCategory.FOOD_BAR.key, CategoryColor.RED)
        val unknown = setColor("pet", CategoryColor.RED)

        // Then
        assertTrue(sub.isFailure)
        assertTrue(unknown.isFailure)
        assertTrue(repository.appColors.value.isEmpty())
    }

    @Test
    fun `GIVEN a group category without a color WHEN building the tree THEN it keeps its place color`() {
        // Given
        val first = groupCategory(
            id = "first",
            parentId = null,
            color = null,
        )
        val second = groupCategory(
            id = "second",
            parentId = null,
            color = null,
        )

        // When
        val tree = CategoryTree.of(listOf(first, second))

        // Then
        assertEquals(CategoryColor.BLUE, tree.findOrNull("first")?.color)
        assertEquals(CategoryColor.ORANGE, tree.findOrNull("second")?.color)
    }

    @Test
    fun `GIVEN a main form with a picked color WHEN choosing no parent again THEN it keeps the color`() {
        // Given
        val pet = groupCategory(
            id = "pet",
            parentId = null,
            color = CategoryColor.PINK,
        )
        val tree = CategoryTree.of(listOf(pet))
        val form = CategoryForm.editingOrNull(tree, "group-1", "pet")!!.withColor(CategoryColor.RED)

        // When
        val again = form.withParent(tree, null)

        // Then
        assertEquals(CategoryColor.RED, again.color)
    }

    private fun groupCategory(
        id: String,
        parentId: String?,
        color: CategoryColor?,
    ): GroupCategory = GroupCategory(
        id = id,
        groupId = "group-1",
        parentId = parentId,
        name = id.replaceFirstChar { it.uppercase() },
        icon = CategoryIcon.PAW.takeIf { parentId == null },
        color = color,
        createdByMemberId = "me",
        isDeleted = false,
        isPending = false,
    )
}
