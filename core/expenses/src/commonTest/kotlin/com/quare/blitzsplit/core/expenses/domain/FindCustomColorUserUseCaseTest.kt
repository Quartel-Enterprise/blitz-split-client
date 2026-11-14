package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.CategoryColor
import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTint
import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import com.quare.blitzsplit.core.expenses.domain.model.tint
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.FindCustomColorUserUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveMapCategoriesUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FindCustomColorUserUseCaseTest {
    private lateinit var useCase: FindCustomColorUserUseCase

    private val blue = CustomColor(0x3A7BD5)
    private val navy = CustomColor(0x1A2340)

    @Test
    fun `GIVEN Food wears the color on this device WHEN finding who uses it THEN gives Food`() = runTest {
        // Given
        prepareScenario(appColors = mapOf(AppCategory.FOOD.key to blue.tint))

        // When
        val user = useCase(blue)

        // Then
        assertEquals(AppCategory.FOOD.key, user?.id)
    }

    @Test
    fun `GIVEN a group main category wears the color WHEN finding who uses it THEN gives that category`() = runTest {
        // Given
        prepareScenario(
            groupCategories = listOf(
                groupCategory(
                    id = "pet",
                    color = blue.tint,
                ),
            ),
        )

        // When
        val user = useCase(blue)

        // Then
        assertEquals("pet", user?.id)
    }

    @Test
    fun `GIVEN a category of a second group wears the color WHEN finding who uses it THEN gives that category`() =
        runTest {
            // Given
            prepareScenario(
                groupCategories = listOf(
                    groupCategory(
                        id = "pet",
                        color = navy.tint,
                    ),
                    groupCategory(
                        id = "beach",
                        groupId = "group-2",
                        color = blue.tint,
                    ),
                ),
            )

            // When
            val user = useCase(blue)

            // Then
            assertEquals("beach", user?.id)
        }

    @Test
    fun `GIVEN two main categories wear the color WHEN finding who uses it THEN gives the first listed`() = runTest {
        // Given
        prepareScenario(
            appColors = mapOf(AppCategory.HOME.key to blue.tint),
            groupCategories = listOf(
                groupCategory(
                    id = "pet",
                    color = blue.tint,
                ),
            ),
        )

        // When
        val user = useCase(blue)

        // Then
        assertEquals(AppCategory.HOME.key, user?.id)
    }

    @Test
    fun `GIVEN only a subcategory stores the color WHEN finding who uses it THEN gives none`() = runTest {
        // Given
        prepareScenario(
            groupCategories = listOf(
                groupCategory(
                    id = "bone",
                    parentId = AppCategory.FOOD.key,
                    color = blue.tint,
                ),
            ),
        )

        // When
        val user = useCase(blue)

        // Then
        assertNull(user)
    }

    @Test
    fun `GIVEN a deleted main category wore the color WHEN finding who uses it THEN gives none`() = runTest {
        // Given
        prepareScenario(
            groupCategories = listOf(
                groupCategory(
                    id = "pet",
                    color = blue.tint,
                    isDeleted = true,
                ),
            ),
        )

        // When
        val user = useCase(blue)

        // Then
        assertNull(user)
    }

    @Test
    fun `GIVEN categories in other colors WHEN finding who uses the color THEN gives none`() = runTest {
        // Given
        prepareScenario(
            appColors = mapOf(AppCategory.FOOD.key to navy.tint),
            groupCategories = listOf(
                groupCategory(
                    id = "pet",
                    color = CategoryColor.TEAL.tint,
                ),
            ),
        )

        // When
        val user = useCase(blue)

        // Then
        assertNull(user)
    }

    private fun groupCategory(
        id: String,
        color: CategoryTint?,
        groupId: String = "group-1",
        parentId: String? = null,
        isDeleted: Boolean = false,
    ): GroupCategory = GroupCategory(
        id = id,
        groupId = groupId,
        parentId = parentId,
        name = id,
        icon = CategoryIcon.PAW.takeIf { parentId == null },
        color = color,
        createdByMemberId = "ana",
        isDeleted = isDeleted,
        isPending = false,
    )

    private fun prepareScenario(
        appColors: Map<String, CategoryTint> = emptyMap(),
        groupCategories: List<GroupCategory> = emptyList(),
    ) {
        val repository = FakeCategoryRepository(initialGroupCategories = groupCategories)
        repository.appColors.value = appColors
        useCase = FindCustomColorUserUseCase(ObserveMapCategoriesUseCase(repository))
    }
}
