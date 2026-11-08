package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.CategoryColor
import com.quare.blitzsplit.core.expenses.domain.model.CategoryEditor
import com.quare.blitzsplit.core.expenses.domain.model.CategoryForm
import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CategoryEditorTest {
    @Test
    fun `GIVEN a new subcategory of Alimentacao WHEN reading its parent THEN it is Alimentacao`() {
        // Given
        val form = CategoryForm.create(tree, GROUP_ID, AppCategory.FOOD.key, "Feira")

        // When
        val parent = editor(form).parent

        // Then
        assertEquals(AppCategory.FOOD.key, parent?.id)
    }

    @Test
    fun `GIVEN a new main category WHEN reading its parent THEN there is none`() {
        // Given
        val form = newMain()

        // When
        val parent = editor(form).parent

        // Then
        assertNull(parent)
    }

    @Test
    fun `GIVEN a main category with subcategories being edited WHEN reading the editor THEN its parent is locked`() {
        // Given
        val form = CategoryForm.editingOrNull(tree, GROUP_ID, "praia")!!

        // When
        val subject = editor(form)

        // Then
        assertTrue(subject.isParentLocked)
        assertFalse("praia" in subject.parents.map { it.id })
        assertFalse(AppCategory.OTHER.key in subject.parents.map { it.id })
    }

    @Test
    fun `GIVEN a name the group already has there WHEN reading the editor THEN the name is taken and it cannot save`() {
        // Given
        val form = CategoryForm.create(tree, GROUP_ID, AppCategory.FOOD.key, "churrasco")

        // When
        val subject = editor(form)

        // Then
        assertTrue(subject.isNameTaken)
        assertFalse(subject.canSave)
    }

    @Test
    fun `GIVEN a free name WHEN reading the editor THEN it can save`() {
        // Given
        val form = CategoryForm.create(tree, GROUP_ID, AppCategory.FOOD.key, "Feira")

        // When
        val subject = editor(form)

        // Then
        assertFalse(subject.isNameTaken)
        assertTrue(subject.canSave)
    }

    @Test
    fun `GIVEN another group has the name WHEN the editor holds only this group categories THEN it can save`() {
        // Given
        val otherGroupChurrasco = churrasco.copy(
            id = "churrasco-2",
            groupId = "group-2",
        )
        val merged = CategoryTree.ofGroups(listOf(praia, otherGroupChurrasco))
        val categories = merged.ofGroup(GROUP_ID)
        val form = CategoryForm.create(categories, GROUP_ID, AppCategory.FOOD.key, "Churrasco")

        // When
        val subject = editor(form, categories = categories)

        // Then
        assertFalse(subject.isNameTaken)
        assertTrue(subject.canSave)
    }

    @Test
    fun `GIVEN a new subcategory WHEN reading its color THEN it is the parent color`() {
        // Given
        val form = CategoryForm.create(tree, GROUP_ID, "praia", "Quiosque")

        // When
        val color = editor(form).color

        // Then
        assertEquals(tree.findOrNull("praia")?.color, color)
    }

    @Test
    fun `GIVEN Alimentacao as the parent WHEN reading the color THEN it is orange`() {
        // Given
        val form = CategoryForm.create(tree, GROUP_ID, AppCategory.FOOD.key, "Feira")

        // When
        val color = editor(form).color

        // Then
        assertEquals(CategoryColor.ORANGE, color)
    }

    @Test
    fun `GIVEN a main group category being edited WHEN reading the color THEN it is its own`() {
        // Given
        val second = praia.copy(
            id = "bar",
            name = "Bar",
        )
        val categories = CategoryTree.of(listOf(praia, second))
        val form = CategoryForm.editingOrNull(categories, GROUP_ID, "bar")!!

        // When
        val color = editor(form, categories = categories).color

        // Then
        assertEquals(CategoryColor.ORANGE, color)
        assertEquals(categories.findOrNull("bar")?.color, color)
    }

    @Test
    fun `GIVEN a subcategory moved out of its parent WHEN reading the color THEN it is its color as a main category`() {
        // Given
        val form = CategoryForm.editingOrNull(tree, GROUP_ID, "quiosque")!!.withParent(tree, null)
        val saved = CategoryTree.of(
            listOf(
                churrasco,
                praia,
                quiosque.copy(
                    parentId = null,
                    color = form.color,
                ),
            ),
        )

        // When
        val color = editor(form).color

        // Then
        assertEquals(saved.findOrNull("quiosque")?.color, color)
        assertEquals(tree.nextMainColor, color)
    }

    @Test
    fun `GIVEN a new main category WHEN reading the color THEN it is the next main color of the group`() {
        // Given
        val form = newMain()

        // When
        val color = editor(form).color

        // Then
        assertEquals(tree.nextMainColor, color)
        assertEquals(CategoryColor.LIME, color)
    }

    @Test
    fun `GIVEN the device is offline WHEN reading the editor THEN it shows the offline notice`() {
        // Given
        val form = newMain()

        // When
        val subject = editor(form, isOnline = false)

        // Then
        assertTrue(subject.showsOfflineNotice)
    }

    @Test
    fun `GIVEN the device is online WHEN reading the editor THEN it hides the offline notice`() {
        // Given
        val form = newMain()

        // When
        val subject = editor(form, isOnline = true)

        // Then
        assertFalse(subject.showsOfflineNotice)
    }

    @Test
    fun `GIVEN a subcategory used in 3 bills WHEN asking to delete it THEN its bills move to its parent`() {
        // Given
        val form = CategoryForm.editingOrNull(tree, GROUP_ID, "churrasco")!!
        val subject = editor(form, usage = 3)

        // When
        val deletion = subject.findDeletionOrNull()

        // Then
        assertNotNull(deletion)
        assertEquals("churrasco", deletion.category.id)
        assertEquals(3, deletion.billCount)
        assertEquals(AppCategory.FOOD.key, deletion.movesTo.id)
        assertFalse(deletion.isDeleting)
    }

    @Test
    fun `GIVEN a subcategory of a group main category WHEN asking to delete it THEN its bills move to that main`() {
        // Given
        val form = CategoryForm.editingOrNull(tree, GROUP_ID, "quiosque")!!
        val subject = editor(form, usage = 1)

        // When
        val deletion = subject.findDeletionOrNull()

        // Then
        assertEquals("praia", deletion?.movesTo?.id)
    }

    @Test
    fun `GIVEN a main group category WHEN asking to delete it THEN its bills move to Outros`() {
        // Given
        val form = CategoryForm.editingOrNull(tree, GROUP_ID, "praia")!!
        val subject = editor(form, usage = 5)

        // When
        val deletion = subject.findDeletionOrNull()

        // Then
        assertEquals(AppCategory.OTHER.key, deletion?.movesTo?.id)
        assertEquals(5, deletion?.billCount)
    }

    @Test
    fun `GIVEN a new category WHEN asking to delete it THEN there is nothing to delete`() {
        // Given
        val subject = editor(newMain())

        // When
        val deletion = subject.findDeletionOrNull()

        // Then
        assertNull(deletion)
    }

    @Test
    fun `GIVEN an edited category whose bills are not read yet WHEN asking to delete it THEN it waits`() {
        // Given
        val form = CategoryForm.editingOrNull(tree, GROUP_ID, "praia")!!
        val subject = editor(form, usage = null)

        // When
        val deletion = subject.findDeletionOrNull()

        // Then
        assertFalse(subject.canDelete)
        assertNull(deletion)
    }

    @Test
    fun `GIVEN a main category with a subcategory WHEN counting its usage THEN both bills are summed`() {
        // Given
        val usageByCategory = mapOf("praia" to 2, "quiosque" to 3, AppCategory.OTHER.key to 7)

        // When
        val usage = CategoryEditor.usageOf(tree, "praia", usageByCategory)

        // Then
        assertEquals(5, usage)
    }

    @Test
    fun `GIVEN a subcategory WHEN counting its usage THEN only its own bills count`() {
        // Given
        val usageByCategory = mapOf(AppCategory.FOOD.key to 4, "churrasco" to 2)

        // When
        val usage = CategoryEditor.usageOf(tree, "churrasco", usageByCategory)

        // Then
        assertEquals(2, usage)
    }

    @Test
    fun `GIVEN a category without bills WHEN counting its usage THEN it is zero`() {
        // Given
        val usageByCategory = mapOf(AppCategory.FOOD.key to 4)

        // When
        val usage = CategoryEditor.usageOf(tree, "praia", usageByCategory)

        // Then
        assertEquals(0, usage)
    }

    @Test
    fun `GIVEN no category WHEN counting its usage THEN it is zero`() {
        // Given
        val usageByCategory = mapOf(AppCategory.FOOD.key to 4)

        // When
        val usage = CategoryEditor.usageOf(tree, null, usageByCategory)

        // Then
        assertEquals(0, usage)
    }

    private fun newMain(): CategoryForm = CategoryForm.create(tree, GROUP_ID, null, "")

    private fun editor(
        form: CategoryForm,
        categories: CategoryTree = tree,
        usage: Int? = 0,
        isOnline: Boolean = true,
    ): CategoryEditor = CategoryEditor(
        form = form,
        categories = categories,
        groupName = "Viagem Floripa",
        creatorName = null,
        usage = usage,
        deletion = null,
        isOnline = isOnline,
    )

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
