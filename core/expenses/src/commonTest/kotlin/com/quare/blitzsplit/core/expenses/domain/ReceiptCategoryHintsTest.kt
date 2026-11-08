package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTerm
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptCategoryHints
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReceiptCategoryHintsTest {
    @Test
    fun `GIVEN a group with its own main and subcategories WHEN building the hints THEN lists only the group ones`() {
        // Given
        val tree = CategoryTree.of(listOf(pet, racao, reforma))

        // When
        val hints = ReceiptCategoryHints.of(tree, emptyList())

        // Then
        assertEquals(
            mapOf(
                "pet" to null,
                "racao" to "pet",
                "reforma" to AppCategory.HOME.key,
            ),
            hints.groupCategories.associate { it.id to it.parentId },
        )
        assertEquals(
            setOf("Pet", "Ração", "Reforma"),
            hints.groupCategories.mapNotNull { it.name }.toSet(),
        )
    }

    @Test
    fun `GIVEN a group with no category of its own WHEN building the hints THEN lists no group category`() {
        // Given
        val tree = CategoryTree.appOnly

        // When
        val hints = ReceiptCategoryHints.of(tree, emptyList())

        // Then
        assertTrue(hints.groupCategories.isEmpty())
    }

    @Test
    fun `GIVEN deleted group categories WHEN building the hints THEN leaves them out`() {
        // Given
        val tree = CategoryTree.of(
            listOf(
                pet,
                racao.copy(isDeleted = true),
                reforma.copy(isDeleted = true),
            ),
        )

        // When
        val hints = ReceiptCategoryHints.of(tree, emptyList())

        // Then
        assertEquals(listOf("pet"), hints.groupCategories.map { it.id })
    }

    @Test
    fun `GIVEN a subcategory of a deleted group main category WHEN building the hints THEN leaves both out`() {
        // Given
        val tree = CategoryTree.of(listOf(pet.copy(isDeleted = true), racao))

        // When
        val hints = ReceiptCategoryHints.of(tree, emptyList())

        // Then
        assertTrue(hints.groupCategories.isEmpty())
    }

    @Test
    fun `GIVEN terms of app categories WHEN building the hints THEN keeps them as written`() {
        // Given
        val tree = CategoryTree.of(listOf(pet))
        val terms = listOf(
            CategoryTerm("farmácia", AppCategory.HEALTH_PHARMACY.key),
            CategoryTerm("aluguel", AppCategory.HOME_RENT.key),
            CategoryTerm("comida", AppCategory.FOOD.key),
        )

        // When
        val hints = ReceiptCategoryHints.of(tree, terms)

        // Then
        assertEquals(terms, hints.terms)
    }

    @Test
    fun `GIVEN a term of a group main category WHEN building the hints THEN keeps it`() {
        // Given
        val tree = CategoryTree.of(listOf(pet))
        val terms = listOf(CategoryTerm("petshop", "pet"))

        // When
        val hints = ReceiptCategoryHints.of(tree, terms)

        // Then
        assertEquals(listOf(CategoryTerm("petshop", "pet")), hints.terms)
    }

    @Test
    fun `GIVEN a term of a group subcategory under a group main WHEN building the hints THEN keeps it`() {
        // Given
        val tree = CategoryTree.of(listOf(pet, racao))
        val terms = listOf(CategoryTerm("ração", "racao"))

        // When
        val hints = ReceiptCategoryHints.of(tree, terms)

        // Then
        assertEquals(listOf(CategoryTerm("ração", "racao")), hints.terms)
    }

    @Test
    fun `GIVEN a term of a group subcategory under an app main WHEN building the hints THEN keeps it`() {
        // Given
        val tree = CategoryTree.of(listOf(reforma))
        val terms = listOf(CategoryTerm("cimento", "reforma"))

        // When
        val hints = ReceiptCategoryHints.of(tree, terms)

        // Then
        assertEquals(listOf(CategoryTerm("cimento", "reforma")), hints.terms)
    }

    @Test
    fun `GIVEN a term of another group category WHEN building the hints THEN drops it`() {
        // Given
        val tree = CategoryTree.of(listOf(pet))
        val terms = listOf(
            CategoryTerm("quiosque", "praia-de-outro-grupo"),
            CategoryTerm("petshop", "pet"),
        )

        // When
        val hints = ReceiptCategoryHints.of(tree, terms)

        // Then
        assertEquals(listOf(CategoryTerm("petshop", "pet")), hints.terms)
    }

    @Test
    fun `GIVEN a term of a deleted subcategory under a group main WHEN building the hints THEN points to the main`() {
        // Given
        val tree = CategoryTree.of(listOf(pet, racao.copy(isDeleted = true)))
        val terms = listOf(CategoryTerm("ração", "racao"))

        // When
        val hints = ReceiptCategoryHints.of(tree, terms)

        // Then
        assertEquals(listOf(CategoryTerm("ração", "pet")), hints.terms)
    }

    @Test
    fun `GIVEN a term of a deleted subcategory under an app main WHEN building the hints THEN points to that main`() {
        // Given
        val tree = CategoryTree.of(listOf(reforma.copy(isDeleted = true)))
        val terms = listOf(CategoryTerm("cimento", "reforma"))

        // When
        val hints = ReceiptCategoryHints.of(tree, terms)

        // Then
        assertEquals(listOf(CategoryTerm("cimento", AppCategory.HOME.key)), hints.terms)
    }

    @Test
    fun `GIVEN a term of a deleted group main category WHEN building the hints THEN drops it`() {
        // Given
        val tree = CategoryTree.of(listOf(pet.copy(isDeleted = true)))
        val terms = listOf(CategoryTerm("petshop", "pet"))

        // When
        val hints = ReceiptCategoryHints.of(tree, terms)

        // Then
        assertTrue(hints.terms.isEmpty())
    }

    @Test
    fun `GIVEN a term of a subcategory whose group main was deleted WHEN building the hints THEN drops it`() {
        // Given
        val tree = CategoryTree.of(listOf(pet.copy(isDeleted = true), racao))
        val terms = listOf(CategoryTerm("ração", "racao"))

        // When
        val hints = ReceiptCategoryHints.of(tree, terms)

        // Then
        assertTrue(hints.terms.isEmpty())
    }

    @Test
    fun `GIVEN a term of Outros WHEN building the hints THEN drops it`() {
        // Given
        val tree = CategoryTree.of(listOf(pet))
        val terms = listOf(CategoryTerm("diversos", AppCategory.OTHER.key))

        // When
        val hints = ReceiptCategoryHints.of(tree, terms)

        // Then
        assertTrue(hints.terms.isEmpty())
    }

    @Test
    fun `GIVEN a term of a category key the app does not know WHEN building the hints THEN drops it`() {
        // Given
        val tree = CategoryTree.appOnly
        val terms = listOf(CategoryTerm("hiper", "food.supermarket"))

        // When
        val hints = ReceiptCategoryHints.of(tree, terms)

        // Then
        assertTrue(hints.terms.isEmpty())
    }

    @Test
    fun `GIVEN more group categories than the reader takes WHEN building the hints THEN sends only the first ones`() {
        // Given
        val many = (1..ReceiptCategoryHints.MAX_CATEGORIES + 5).map { index ->
            pet.copy(
                id = "c$index",
                name = "Categoria $index",
            )
        }
        val tree = CategoryTree.of(many)

        // When
        val hints = ReceiptCategoryHints.of(tree, emptyList())

        // Then
        assertEquals(ReceiptCategoryHints.MAX_CATEGORIES, hints.groupCategories.size)
        assertEquals("c1", hints.groupCategories.first().id)
    }

    @Test
    fun `GIVEN a term of a group category past the limit WHEN building the hints THEN drops it`() {
        // Given
        val many = (1..ReceiptCategoryHints.MAX_CATEGORIES + 1).map { index ->
            pet.copy(
                id = "c$index",
                name = "Categoria $index",
            )
        }
        val lastId = "c${ReceiptCategoryHints.MAX_CATEGORIES + 1}"
        val terms = listOf(
            CategoryTerm("primeira", "c1"),
            CategoryTerm("ultima", lastId),
        )

        // When
        val hints = ReceiptCategoryHints.of(CategoryTree.of(many), terms)

        // Then
        assertEquals(listOf(CategoryTerm("primeira", "c1")), hints.terms)
    }

    @Test
    fun `GIVEN more terms than the reader takes WHEN building the hints THEN keeps the last ones as the newest`() {
        // Given
        val terms = (1..ReceiptCategoryHints.MAX_TERMS + 5).map { index ->
            CategoryTerm(
                term = "termo$index",
                categoryId = AppCategory.FOOD_MARKET.key,
            )
        }

        // When
        val hints = ReceiptCategoryHints.of(CategoryTree.appOnly, terms)

        // Then
        assertEquals(ReceiptCategoryHints.MAX_TERMS, hints.terms.size)
        assertEquals("termo6", hints.terms.first().term)
        assertEquals("termo${ReceiptCategoryHints.MAX_TERMS + 5}", hints.terms.last().term)
    }

    private val pet = GroupCategory(
        id = "pet",
        groupId = "apto-302",
        parentId = null,
        name = "Pet",
        icon = CategoryIcon.DOTS,
        color = null,
        createdByMemberId = "ana",
        isDeleted = false,
        isPending = false,
    )

    private val racao = GroupCategory(
        id = "racao",
        groupId = "apto-302",
        parentId = "pet",
        name = "Ração",
        icon = null,
        color = null,
        createdByMemberId = "ana",
        isDeleted = false,
        isPending = false,
    )

    private val reforma = GroupCategory(
        id = "reforma",
        groupId = "apto-302",
        parentId = AppCategory.HOME.key,
        name = "Reforma",
        icon = null,
        color = null,
        createdByMemberId = "pierre",
        isDeleted = false,
        isPending = false,
    )
}
