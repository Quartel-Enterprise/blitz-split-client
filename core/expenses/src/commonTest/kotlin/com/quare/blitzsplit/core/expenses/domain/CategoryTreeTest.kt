package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.CategoryColor
import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.model.GroupCurrency
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
        assertEquals(CategoryColor.ORANGE, food?.subcategories?.last()?.color)
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

    @Test
    fun `GIVEN two groups with a main category each WHEN merging their trees THEN each keeps its group color`() {
        // Given
        val bar = praia.copy(
            id = "bar",
            groupId = "group-2",
            name = "Bar",
        )
        val own = CategoryTree.of(listOf(bar))

        // When
        val tree = CategoryTree.ofGroups(listOf(praia, churrasco, bar))

        // Then
        assertEquals(listOf("praia", "bar", AppCategory.OTHER.key), tree.branches.map { it.category.id }.takeLast(3))
        assertEquals(own.findOrNull("bar")?.color, tree.findOrNull("bar")?.color)
        assertEquals(CategoryTree.of(listOf(praia)).findOrNull("praia")?.color, tree.findOrNull("praia")?.color)
        assertEquals(
            "churrasco",
            tree
                .branchOf(AppCategory.FOOD.key)
                ?.subcategories
                ?.last()
                ?.id,
        )
    }

    @Test
    fun `GIVEN a deleted and an unknown category WHEN reading a map term THEN the parent and nothing`() {
        // Given
        val tree = CategoryTree.ofGroups(listOf(churrasco.copy(isDeleted = true)))

        // When
        val deleted = tree.readAsOrNull("churrasco")
        val unknown = tree.readAsOrNull("of-a-group-gone")

        // Then
        assertEquals(AppCategory.FOOD.key, deleted?.id)
        assertNull(unknown)
        assertEquals(AppCategory.FOOD_MARKET.key, tree.readAsOrNull(AppCategory.FOOD_MARKET.key)?.id)
    }

    @Test
    fun `GIVEN two groups with the same subcategory name WHEN taking one group THEN only its categories count`() {
        // Given
        val otherChurrasco = churrasco.copy(
            id = "churrasco-2",
            groupId = "group-2",
        )
        val merged = CategoryTree.ofGroups(listOf(churrasco, praia, otherChurrasco))

        // When
        val tree = merged.ofGroup("group-2")

        // Then
        assertEquals("churrasco-2", tree.findOrNull("churrasco-2")?.id)
        assertNull(tree.findOrNull("churrasco"))
        assertNull(tree.findOrNull("praia"))
        assertFalse(tree.hasGroupName(AppCategory.FOOD.key, "Churrasco", exceptId = "churrasco-2"))
    }

    @Test
    fun `GIVEN a group without categories WHEN taking it out of the merged tree THEN only the app ones are left`() {
        // Given
        val merged = CategoryTree.ofGroups(listOf(churrasco, praia))

        // When
        val tree = merged.ofGroup("group-2")

        // Then
        assertEquals(CategoryTree.appOnly.all.map { it.id }, tree.all.map { it.id })
    }

    @Test
    fun `GIVEN a deleted subcategory WHEN taking its group out THEN its bills still read as its parent`() {
        // Given
        val merged = CategoryTree.ofGroups(listOf(churrasco.copy(isDeleted = true), praia))

        // When
        val tree = merged.ofGroup("group-1")

        // Then
        assertEquals(AppCategory.FOOD.key, tree.resolve("churrasco").id)
        assertEquals("praia", tree.findOrNull("praia")?.id)
    }

    @Test
    fun `GIVEN only the app categories WHEN asking the next main color THEN it is the first unused color`() {
        // Given
        val tree = CategoryTree.appOnly

        // When
        val color = tree.nextMainColor

        // Then
        assertEquals(CategoryColor.LIME, color)
    }

    @Test
    fun `GIVEN a deleted main a live main and a sub WHEN asking the next main color THEN the deleted one takes none`() {
        // Given
        val bar = praia.copy(
            id = "bar",
            name = "Bar",
        )
        val tree = CategoryTree.of(listOf(praia.copy(isDeleted = true), bar, churrasco))

        // When
        val color = tree.nextMainColor

        // Then
        assertEquals(CategoryColor.LIME, color)
    }

    @Test
    fun `GIVEN as many main categories as colors WHEN asking the next main color THEN it starts the palette again`() {
        // Given
        val mains = CategoryColor.entries.indices.map { index ->
            praia.copy(
                id = "main-$index",
                name = "Main $index",
                color = CategoryColor.entries[index],
            )
        }
        val tree = CategoryTree.of(mains)

        // When
        val color = tree.nextMainColor

        // Then
        assertEquals(CategoryColor.entries[tree.branches.size % CategoryColor.entries.size], color)
    }

    @Test
    fun `GIVEN a new main category WHEN it is saved THEN it gets the color the tree said came next`() {
        // Given
        val before = CategoryTree.of(listOf(praia))
        val bar = praia.copy(
            id = "bar",
            name = "Bar",
            color = before.nextMainColor,
        )

        // When
        val after = CategoryTree.of(listOf(praia, bar))

        // Then
        assertEquals(before.nextMainColor, after.findOrNull("bar")?.color)
    }

    @Test
    fun `GIVEN bills in a live and a deleted subcategory WHEN counting usage THEN each counts by what it reads as`() {
        // Given
        val tree = CategoryTree.of(listOf(churrasco.copy(isDeleted = true)))
        val bills = listOf(
            bill("1", AppCategory.FOOD_MARKET.key),
            bill("2", AppCategory.FOOD_MARKET.key),
            bill("3", "churrasco"),
            bill("4", AppCategory.FOOD.key),
            bill("5", null),
        )

        // When
        val usage = tree.usageOf(bills)

        // Then
        assertEquals(mapOf(AppCategory.FOOD_MARKET.key to 2, AppCategory.FOOD.key to 2), usage)
    }

    @Test
    fun `GIVEN a bill of an unknown category WHEN counting usage THEN it counts for Outros`() {
        // Given
        val tree = CategoryTree.appOnly
        val bills = listOf(bill("1", "of-a-group-gone"))

        // When
        val usage = tree.usageOf(bills)

        // Then
        assertEquals(mapOf(AppCategory.OTHER.key to 1), usage)
    }

    private fun bill(
        id: String,
        categoryId: String?,
    ): Expense = Expense(
        id = id,
        groupId = "group-1",
        description = id,
        currency = GroupCurrency.BRL,
        amountMinor = 1_000L,
        splitMode = SplitMode.EQUAL,
        payers = emptyList(),
        shares = emptyList(),
        items = null,
        categoryId = categoryId,
        spentAt = 0L,
        isPending = false,
        updatedAt = 0L,
        updatedByMemberId = null,
    )

    private val churrasco = GroupCategory(
        id = "churrasco",
        groupId = "group-1",
        parentId = AppCategory.FOOD.key,
        name = "Churrasco",
        icon = null,
        color = null,
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
        color = null,
        createdByMemberId = "carla",
        isDeleted = false,
        isPending = true,
    )
}
