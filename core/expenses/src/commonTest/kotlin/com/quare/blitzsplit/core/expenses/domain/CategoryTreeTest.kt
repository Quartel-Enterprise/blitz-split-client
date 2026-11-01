package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import com.quare.blitzsplit.core.model.AvatarColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CategoryTreeTest {
    @Test
    fun `GIVEN group categories WHEN building the tree THEN group mains come before Outros and subs last`() {
        // Given
        val tree = CategoryTree.of(listOf(praia, churrasco))

        // When
        val mains = tree.branches.map { it.category.id }
        val food = tree.branchOf(AppCategory.FOOD.key)

        // Then
        assertEquals(listOf("praia", AppCategory.OTHER.key), mains.takeLast(2))
        assertEquals("churrasco", food?.subcategories?.last()?.id)
        assertEquals(CategoryIcon.FOOD, food?.subcategories?.last()?.icon)
        assertEquals(AvatarColor.BLUE, food?.subcategories?.last()?.color)
    }

    @Test
    fun `GIVEN a deleted subcategory WHEN resolving its bills THEN they read as its parent`() {
        // Given
        val tree = CategoryTree.of(listOf(churrasco.copy(isDeleted = true)))

        // When
        val resolved = tree.resolve("churrasco")

        // Then
        assertEquals(AppCategory.FOOD.key, resolved.id)
        assertNull(tree.findOrNull("churrasco"))
    }

    @Test
    fun `GIVEN a deleted main group category WHEN resolving it or its subcategory THEN both read as Outros`() {
        // Given
        val quiosque = churrasco.copy(
            id = "quiosque",
            parentId = "praia",
        )
        val tree = CategoryTree.of(listOf(praia.copy(isDeleted = true), quiosque))

        // When
        val main = tree.resolve("praia")
        val sub = tree.resolve("quiosque")

        // Then
        assertEquals(AppCategory.OTHER.key, main.id)
        assertEquals(AppCategory.OTHER.key, sub.id)
    }

    @Test
    fun `GIVEN no category or an unknown one WHEN resolving THEN reads as Outros`() {
        // Given
        val tree = CategoryTree.appOnly

        // When
        val none = tree.resolve(null)
        val unknown = tree.resolve("food.unknown")

        // Then
        assertTrue(none.isOther)
        assertTrue(unknown.isOther)
    }

    @Test
    fun `GIVEN a subcategory WHEN asking for its main THEN gives the main and a main gives itself`() {
        // Given
        val tree = CategoryTree.of(listOf(churrasco))

        // When
        val ofSub = tree.mainOf(tree.resolve(AppCategory.FOOD_MARKET.key))
        val ofMain = tree.mainOf(tree.resolve(AppCategory.HOME.key))

        // Then
        assertEquals(AppCategory.FOOD.key, ofSub.id)
        assertEquals(AppCategory.HOME.key, ofMain.id)
    }

    @Test
    fun `GIVEN Churrasco in Alimentacao WHEN checking names THEN it is taken there only`() {
        // Given
        val tree = CategoryTree.of(listOf(churrasco, praia))

        // When
        val taken = tree.hasGroupName(AppCategory.FOOD.key, " churrasco ", exceptId = null)
        val elsewhere = tree.hasGroupName(AppCategory.HOME.key, "Churrasco", exceptId = null)
        val itself = tree.hasGroupName(AppCategory.FOOD.key, "Churrasco", exceptId = "churrasco")

        // Then
        assertTrue(taken)
        assertFalse(elsewhere)
        assertFalse(itself)
        assertTrue(tree.hasSubcategories(AppCategory.FOOD.key))
        assertFalse(tree.hasSubcategories("praia"))
    }

    private val churrasco = GroupCategory(
        id = "churrasco",
        groupId = "group-1",
        parentId = AppCategory.FOOD.key,
        name = "Churrasco",
        icon = null,
        createdByMemberId = "ana",
        isDeleted = false,
        isPending = false,
    )

    private val praia = GroupCategory(
        id = "praia",
        groupId = "group-1",
        parentId = null,
        name = "Praia",
        icon = CategoryIcon.SUN,
        createdByMemberId = "carla",
        isDeleted = false,
        isPending = true,
    )
}
