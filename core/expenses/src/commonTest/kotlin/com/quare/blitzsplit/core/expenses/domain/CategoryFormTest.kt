package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.CategoryForm
import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CategoryFormTest {
    @Test
    fun `GIVEN Alimentacao as the parent WHEN creating a form THEN it starts inside it with its icon`() {
        // Given
        val parentId = AppCategory.FOOD.key

        // When
        val form = CategoryForm.create(tree, GROUP_ID, parentId, "Feira")

        // Then
        assertEquals(AppCategory.FOOD.key, form.parentId)
        assertEquals(CategoryIcon.FOOD, form.icon)
        assertEquals(GROUP_ID, form.groupId)
        assertNull(form.categoryId)
        assertNull(form.createdByMemberId)
        assertFalse(form.isSaving)
        assertFalse(form.isEditing)
        assertTrue(form.isSubcategory)
    }

    @Test
    fun `GIVEN a main category of the group as the parent WHEN creating a form THEN it takes the group icon`() {
        // Given
        val parentId = "praia"

        // When
        val form = CategoryForm.create(tree, GROUP_ID, parentId, "Quiosque")

        // Then
        assertEquals("praia", form.parentId)
        assertEquals(CategoryIcon.SUN, form.icon)
    }

    @Test
    fun `GIVEN a subcategory as the parent WHEN creating a form THEN it starts as a main category with the paw`() {
        // Given
        val parentId = AppCategory.FOOD_MARKET.key

        // When
        val form = CategoryForm.create(tree, GROUP_ID, parentId, "Feira")

        // Then
        assertNull(form.parentId)
        assertEquals(CategoryIcon.PAW, form.icon)
        assertFalse(form.isSubcategory)
    }

    @Test
    fun `GIVEN Outros as the parent WHEN creating a form THEN it starts as a main category with the paw`() {
        // Given
        val parentId = AppCategory.OTHER.key

        // When
        val form = CategoryForm.create(tree, GROUP_ID, parentId, "Feira")

        // Then
        assertNull(form.parentId)
        assertEquals(CategoryIcon.PAW, form.icon)
    }

    @Test
    fun `GIVEN an unknown parent WHEN creating a form THEN it starts as a main category with the paw`() {
        // Given
        val parentId = "of-a-group-gone"

        // When
        val form = CategoryForm.create(tree, GROUP_ID, parentId, "Feira")

        // Then
        assertNull(form.parentId)
        assertEquals(CategoryIcon.PAW, form.icon)
    }

    @Test
    fun `GIVEN a deleted main category as the parent WHEN creating a form THEN it starts as a main category`() {
        // Given
        val categories = CategoryTree.of(listOf(praia.copy(isDeleted = true)))

        // When
        val form = CategoryForm.create(categories, GROUP_ID, "praia", "Quiosque")

        // Then
        assertNull(form.parentId)
        assertEquals(CategoryIcon.PAW, form.icon)
    }

    @Test
    fun `GIVEN a name with spaces and over 30 letters WHEN creating a form THEN it is trimmed and capped at 30`() {
        // Given
        val name = "  " + "a".repeat(40) + "  "

        // When
        val form = CategoryForm.create(tree, GROUP_ID, null, name)

        // Then
        assertEquals("a".repeat(30), form.name)
    }

    @Test
    fun `GIVEN a short name with spaces WHEN creating a form THEN it keeps the name trimmed`() {
        // Given
        val name = "  Pet  "

        // When
        val form = CategoryForm.create(tree, GROUP_ID, null, name)

        // Then
        assertEquals("Pet", form.name)
    }

    @Test
    fun `GIVEN a group category WHEN editing it THEN the form copies its name place icon and creator`() {
        // Given
        val categoryId = "churrasco"

        // When
        val form = CategoryForm.editingOrNull(tree, GROUP_ID, categoryId)

        // Then
        assertEquals("churrasco", form?.categoryId)
        assertEquals(GROUP_ID, form?.groupId)
        assertEquals("Churrasco", form?.name)
        assertEquals(AppCategory.FOOD.key, form?.parentId)
        assertEquals(CategoryIcon.FOOD, form?.icon)
        assertEquals("ana", form?.createdByMemberId)
        assertEquals(false, form?.isSaving)
        assertEquals(true, form?.isEditing)
    }

    @Test
    fun `GIVEN a main group category WHEN editing it THEN the form keeps its own icon and no parent`() {
        // Given
        val categoryId = "praia"

        // When
        val form = CategoryForm.editingOrNull(tree, GROUP_ID, categoryId)

        // Then
        assertNull(form?.parentId)
        assertEquals(CategoryIcon.SUN, form?.icon)
        assertEquals("carla", form?.createdByMemberId)
    }

    @Test
    fun `GIVEN an app category WHEN editing it THEN there is no form`() {
        // Given
        val categoryId = AppCategory.FOOD_MARKET.key

        // When
        val form = CategoryForm.editingOrNull(tree, GROUP_ID, categoryId)

        // Then
        assertNull(form)
    }

    @Test
    fun `GIVEN an unknown category WHEN editing it THEN there is no form`() {
        // Given
        val categoryId = "of-a-group-gone"

        // When
        val form = CategoryForm.editingOrNull(tree, GROUP_ID, categoryId)

        // Then
        assertNull(form)
    }

    @Test
    fun `GIVEN a deleted group category WHEN editing it THEN there is no form`() {
        // Given
        val categories = CategoryTree.of(listOf(churrasco.copy(isDeleted = true)))

        // When
        val form = CategoryForm.editingOrNull(categories, GROUP_ID, "churrasco")

        // Then
        assertNull(form)
    }

    @Test
    fun `GIVEN a name over 30 letters WHEN typing it THEN the form keeps the first 30`() {
        // Given
        val form = newMain()

        // When
        val typed = form.withName("b".repeat(35))

        // Then
        assertEquals("b".repeat(30), typed.name)
    }

    @Test
    fun `GIVEN a main category WHEN picking an icon THEN the form takes it`() {
        // Given
        val form = newMain()

        // When
        val picked = form.withIcon(CategoryIcon.SUN)

        // Then
        assertEquals(CategoryIcon.SUN, picked.icon)
    }

    @Test
    fun `GIVEN a subcategory WHEN picking an icon THEN nothing changes`() {
        // Given
        val form = CategoryForm.create(tree, GROUP_ID, AppCategory.FOOD.key, "Feira")

        // When
        val picked = form.withIcon(CategoryIcon.SUN)

        // Then
        assertEquals(form, picked)
    }

    @Test
    fun `GIVEN a new main category WHEN putting it inside Alimentacao THEN it takes the parent and its icon`() {
        // Given
        val form = newMain()

        // When
        val moved = form.withParent(tree, AppCategory.FOOD.key)

        // Then
        assertEquals(AppCategory.FOOD.key, moved.parentId)
        assertEquals(CategoryIcon.FOOD, moved.icon)
    }

    @Test
    fun `GIVEN a new main category WHEN putting it inside a group main category THEN it takes the group icon`() {
        // Given
        val form = newMain()

        // When
        val moved = form.withParent(tree, "praia")

        // Then
        assertEquals("praia", moved.parentId)
        assertEquals(CategoryIcon.SUN, moved.icon)
    }

    @Test
    fun `GIVEN a subcategory WHEN picking no parent THEN it becomes a main category keeping its icon`() {
        // Given
        val form = CategoryForm.create(tree, GROUP_ID, AppCategory.FOOD.key, "Feira")

        // When
        val moved = form.withParent(tree, null)

        // Then
        assertNull(moved.parentId)
        assertEquals(CategoryIcon.FOOD, moved.icon)
    }

    @Test
    fun `GIVEN a subcategory WHEN picking an unknown parent THEN it becomes a main category keeping its icon`() {
        // Given
        val form = CategoryForm.create(tree, GROUP_ID, AppCategory.FOOD.key, "Feira")

        // When
        val moved = form.withParent(tree, "of-a-group-gone")

        // Then
        assertNull(moved.parentId)
        assertEquals(CategoryIcon.FOOD, moved.icon)
    }

    @Test
    fun `GIVEN a new main category WHEN putting it inside Outros THEN it stays a main category`() {
        // Given
        val form = newMain()

        // When
        val moved = form.withParent(tree, AppCategory.OTHER.key)

        // Then
        assertNull(moved.parentId)
        assertEquals(CategoryIcon.PAW, moved.icon)
    }

    @Test
    fun `GIVEN a new main category WHEN putting it inside a subcategory THEN it stays a main category`() {
        // Given
        val form = newMain()

        // When
        val moved = form.withParent(tree, AppCategory.FOOD_MARKET.key)

        // Then
        assertNull(moved.parentId)
        assertEquals(CategoryIcon.PAW, moved.icon)
    }

    @Test
    fun `GIVEN a main group category without subcategories WHEN putting it inside itself THEN it stays a main one`() {
        // Given
        val categories = CategoryTree.of(listOf(praia))
        val form = CategoryForm.editingOrNull(categories, GROUP_ID, "praia")!!

        // When
        val moved = form.withParent(categories, "praia")

        // Then
        assertNull(moved.parentId)
        assertEquals(CategoryIcon.SUN, moved.icon)
    }

    @Test
    fun `GIVEN a main category with subcategories WHEN putting it inside another THEN nothing changes`() {
        // Given
        val form = CategoryForm.editingOrNull(tree, GROUP_ID, "praia")!!

        // When
        val moved = form.withParent(tree, AppCategory.FOOD.key)

        // Then
        assertSame(form, moved)
    }

    @Test
    fun `GIVEN a new category WHEN listing the parents THEN every main category but Outros shows`() {
        // Given
        val form = newMain()

        // When
        val parents = form.findParentsIn(tree).map { it.id }

        // Then
        val appMains = AppCategory.mains.map { it.key } - AppCategory.OTHER.key
        assertEquals(appMains + "praia", parents)
    }

    @Test
    fun `GIVEN a main group category being edited WHEN listing the parents THEN it is not among them`() {
        // Given
        val form = CategoryForm.editingOrNull(tree, GROUP_ID, "praia")!!

        // When
        val parents = form.findParentsIn(tree).map { it.id }

        // Then
        assertFalse("praia" in parents)
        assertFalse(AppCategory.OTHER.key in parents)
        assertTrue(AppCategory.FOOD.key in parents)
    }

    @Test
    fun `GIVEN a main category with subcategories WHEN asking if the parent is locked THEN it is`() {
        // Given
        val form = CategoryForm.editingOrNull(tree, GROUP_ID, "praia")!!

        // When
        val isLocked = form.isParentLockedIn(tree)

        // Then
        assertTrue(isLocked)
    }

    @Test
    fun `GIVEN a new category or a subcategory WHEN asking if the parent is locked THEN it is not`() {
        // Given
        val new = newMain()
        val subcategory = CategoryForm.editingOrNull(tree, GROUP_ID, "churrasco")!!

        // When
        val newIsLocked = new.isParentLockedIn(tree)
        val subcategoryIsLocked = subcategory.isParentLockedIn(tree)

        // Then
        assertFalse(newIsLocked)
        assertFalse(subcategoryIsLocked)
    }

    @Test
    fun `GIVEN a group subcategory with the same name in other case WHEN checking the name THEN it is taken`() {
        // Given
        val form = CategoryForm.create(tree, GROUP_ID, AppCategory.FOOD.key, " CHURRASCO ")

        // When
        val isTaken = form.isNameTakenIn(tree)

        // Then
        assertTrue(isTaken)
    }

    @Test
    fun `GIVEN a group category with accents WHEN checking the name typed without them THEN it is taken`() {
        // Given
        val categories = CategoryTree.of(listOf(churrasco.copy(name = "Café da manhã")))
        val form = CategoryForm.create(categories, GROUP_ID, AppCategory.FOOD.key, "cafe da manha")

        // When
        val isTaken = form.isNameTakenIn(categories)

        // Then
        assertTrue(isTaken)
    }

    @Test
    fun `GIVEN the category being edited WHEN checking its own name THEN it is not taken`() {
        // Given
        val form = CategoryForm.editingOrNull(tree, GROUP_ID, "churrasco")!!

        // When
        val isTaken = form.isNameTakenIn(tree)

        // Then
        assertFalse(isTaken)
    }

    @Test
    fun `GIVEN a group category under another parent WHEN checking the same name THEN it is not taken`() {
        // Given
        val form = CategoryForm.create(tree, GROUP_ID, AppCategory.HOME.key, "Churrasco")

        // When
        val isTaken = form.isNameTakenIn(tree)

        // Then
        assertFalse(isTaken)
    }

    @Test
    fun `GIVEN Mercado in Alimentacao WHEN checking mercado there THEN it is taken`() {
        // Given
        val form = CategoryForm.create(tree, GROUP_ID, AppCategory.FOOD.key, "mercado")

        // When
        val isTaken = form.isNameTakenIn(tree)

        // Then
        assertTrue(isTaken)
    }

    @Test
    fun `GIVEN Groceries is the English name of Mercado WHEN checking it in Alimentacao THEN it is taken`() {
        // Given
        val form = CategoryForm.create(tree, GROUP_ID, AppCategory.FOOD.key, "Groceries")

        // When
        val isTaken = form.isNameTakenIn(tree)

        // Then
        assertTrue(isTaken)
    }

    @Test
    fun `GIVEN Mercado in Alimentacao WHEN checking mercado in Casa THEN it is not taken`() {
        // Given
        val form = CategoryForm.create(tree, GROUP_ID, AppCategory.HOME.key, "Mercado")

        // When
        val isTaken = form.isNameTakenIn(tree)

        // Then
        assertFalse(isTaken)
    }

    @Test
    fun `GIVEN the app main category Alimentacao WHEN checking a new main category with its name THEN it is taken`() {
        // Given
        val form = CategoryForm.create(tree, GROUP_ID, null, "alimentacao")

        // When
        val isTaken = form.isNameTakenIn(tree)

        // Then
        assertTrue(isTaken)
    }

    @Test
    fun `GIVEN a blank name WHEN checking the name THEN it is never taken`() {
        // Given
        val form = newMain().withName("   ")

        // When
        val isTaken = form.isNameTakenIn(tree)

        // Then
        assertFalse(isTaken)
    }

    @Test
    fun `GIVEN a free valid name WHEN asking if it can be saved THEN it can`() {
        // Given
        val form = newMain().withName("Pet")

        // When
        val canSave = form.canSaveIn(tree)

        // Then
        assertTrue(canSave)
    }

    @Test
    fun `GIVEN a blank name WHEN asking if it can be saved THEN it cannot`() {
        // Given
        val form = newMain().withName("  ")

        // When
        val canSave = form.canSaveIn(tree)

        // Then
        assertFalse(canSave)
    }

    @Test
    fun `GIVEN a name over 30 letters WHEN asking if it can be saved THEN it cannot`() {
        // Given
        val form = newMain().copy(name = "c".repeat(31))

        // When
        val canSave = form.canSaveIn(tree)

        // Then
        assertFalse(canSave)
    }

    @Test
    fun `GIVEN a taken name WHEN asking if it can be saved THEN it cannot`() {
        // Given
        val form = CategoryForm.create(tree, GROUP_ID, AppCategory.FOOD.key, "Churrasco")

        // When
        val canSave = form.canSaveIn(tree)

        // Then
        assertFalse(canSave)
    }

    @Test
    fun `GIVEN a form already saving WHEN asking if it can be saved THEN it cannot`() {
        // Given
        val form = newMain().withName("Pet").copy(isSaving = true)

        // When
        val canSave = form.canSaveIn(tree)

        // Then
        assertFalse(canSave)
    }

    private fun newMain(): CategoryForm = CategoryForm.create(tree, GROUP_ID, null, "")

    private val churrasco = GroupCategory(
        id = "churrasco",
        groupId = GROUP_ID,
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
        groupId = GROUP_ID,
        parentId = null,
        name = "Praia",
        icon = CategoryIcon.SUN,
        color = null,
        createdByMemberId = "carla",
        isDeleted = false,
        isPending = false,
    )

    private val quiosque = churrasco.copy(
        id = "quiosque",
        parentId = "praia",
        name = "Quiosque",
    )

    private val tree = CategoryTree.of(listOf(churrasco, praia, quiosque))

    private companion object {
        const val GROUP_ID = "group-1"
    }
}
