package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteGroupCategoryUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.SaveGroupCategoryUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SaveGroupCategoryUseCaseTest {
    private val repository = FakeCategoryRepository()
    private val save = SaveGroupCategoryUseCase(repository) { 1_000L }

    @Test
    fun `GIVEN a new main category WHEN saving THEN stores it trimmed with its icon and who created it`() = runTest {
        // Given
        val name = "  Pet  "

        // When
        val id = save(null, GROUP_ID, name, null, CategoryIcon.PAW, "me").getOrThrow()

        // Then
        val stored = repository.groupCategories.value.single()
        assertEquals(listOf(id, "Pet", "me"), listOf(stored.id, stored.name, stored.createdByMemberId))
        assertEquals(CategoryIcon.PAW, stored.icon)
    }

    @Test
    fun `GIVEN a new subcategory WHEN saving THEN it keeps no icon of its own`() = runTest {
        // Given
        val parentId = "food"

        // When
        save(null, GROUP_ID, "Feira", parentId, CategoryIcon.PAW, "me")

        // Then
        assertNull(repository.groupCategories.value.single().icon)
    }

    @Test
    fun `GIVEN a blank name WHEN saving THEN fails and stores nothing`() = runTest {
        // Given
        val name = "   "

        // When
        val result = save(null, GROUP_ID, name, null, CategoryIcon.PAW, "me")

        // Then
        assertTrue(result.isFailure)
        assertTrue(repository.groupCategories.value.isEmpty())
    }

    @Test
    fun `GIVEN a category WHEN editing it THEN changes its name and place, and deleting it leaves a tombstone`() =
        runTest {
            // Given
            repository.groupCategories.value = listOf(
                GroupCategory("cat-1", GROUP_ID, null, "Pet", CategoryIcon.PAW, "ana", isDeleted = false, isPending = false),
            )

            // When
            save("cat-1", GROUP_ID, "Bichos", "home", CategoryIcon.PAW, "me")
            val edited = repository.observeGroupCategories(GROUP_ID).first().single()
            DeleteGroupCategoryUseCase(repository) { 2_000L }("cat-1")

            // Then
            assertEquals(Triple("Bichos", "home", null), Triple(edited.name, edited.parentId, edited.icon))
            assertTrue(repository.groupCategories.value.single().isDeleted)
        }

    private companion object {
        const val GROUP_ID = "group-1"
    }
}
