package com.quare.blitzsplit.core.groups.domain

import com.quare.blitzsplit.core.groups.domain.model.CategorySplit
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupDefaultSplit
import com.quare.blitzsplit.core.groups.domain.usecase.impl.GetSplitIncomesUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.SaveSplitAsDefaultUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.UpdateDefaultSplitUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SaveSplitAsDefaultUseCaseTest {
    private lateinit var repository: FakeGroupRepository
    private lateinit var useCase: SaveSplitAsDefaultUseCase

    @Test
    fun `GIVEN a default with exceptions WHEN saving as the group default THEN it keeps every exception`() = runTest {
        // Given
        prepareScenario(defaultSplit = split(DefaultSplitMode.PERCENT, mapOf(ANA to 60L, BRUNO to 40L), market, bar))
        val weights = mapOf(ANA to 3L, BRUNO to 1L)

        // When
        val result = useCase(GROUP_ID, null, DefaultSplitMode.SHARES, weights)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            GROUP_ID to split(DefaultSplitMode.SHARES, weights, market, bar),
            repository.savedDefaultSplits.single(),
        )
    }

    @Test
    fun `GIVEN a group without a default WHEN saving as the group default THEN it becomes the default`() = runTest {
        // Given
        prepareScenario(defaultSplit = null)
        val weights = mapOf(ANA to 70L, BRUNO to 30L)

        // When
        useCase(GROUP_ID, null, DefaultSplitMode.PERCENT, weights)

        // Then
        assertEquals(split(DefaultSplitMode.PERCENT, weights), repository.savedDefaultSplits.single().second)
    }

    @Test
    fun `GIVEN a Pela renda default WHEN saving as the group default THEN the incomes leave the device`() = runTest {
        // Given
        prepareScenario(
            defaultSplit = split(DefaultSplitMode.INCOME, mapOf(ANA to 75L, BRUNO to 25L)),
            incomes = mapOf(ANA to 900_000L, BRUNO to 300_000L),
        )
        val weights = mapOf(ANA to 2L, BRUNO to 1L)

        // When
        useCase(GROUP_ID, null, DefaultSplitMode.SHARES, weights)

        // Then
        assertEquals(split(DefaultSplitMode.SHARES, weights), repository.savedDefaultSplits.single().second)
        assertTrue(repository.savedIncomes.getValue(GROUP_ID).isEmpty())
    }

    @Test
    fun `GIVEN no exception WHEN saving Igual with everyone in as the group default THEN none is kept`() = runTest {
        // Given
        prepareScenario(defaultSplit = split(DefaultSplitMode.SHARES, mapOf(ANA to 3L, BRUNO to 1L)))
        val weights = mapOf(ANA to 1L, BRUNO to 1L)

        // When
        val result = useCase(GROUP_ID, null, DefaultSplitMode.EQUAL, weights)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(GROUP_ID to null, repository.savedDefaultSplits.single())
        assertNull(
            repository.groups.value
                .single()
                .defaultSplit,
        )
    }

    @Test
    fun `GIVEN an exception WHEN saving Igual with everyone in as the group default THEN it is kept`() = runTest {
        // Given
        prepareScenario(defaultSplit = split(DefaultSplitMode.SHARES, mapOf(ANA to 3L, BRUNO to 1L), market))
        val weights = mapOf(ANA to 1L, BRUNO to 1L)

        // When
        useCase(GROUP_ID, null, DefaultSplitMode.EQUAL, weights)

        // Then
        assertEquals(split(DefaultSplitMode.EQUAL, weights, market), repository.savedDefaultSplits.single().second)
    }

    @Test
    fun `GIVEN the category has an exception WHEN saving as it THEN only it changes in its place`() = runTest {
        // Given
        val main = split(DefaultSplitMode.SHARES, mapOf(ANA to 3L, BRUNO to 1L), bar, market, rent)
        prepareScenario(defaultSplit = main)
        val weights = mapOf(ANA to 1L, BRUNO to 0L)

        // When
        val result = useCase(GROUP_ID, MARKET, DefaultSplitMode.EQUAL, weights)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            main.copy(exceptions = listOf(bar, CategorySplit(MARKET, DefaultSplitMode.EQUAL, weights), rent)),
            repository.savedDefaultSplits.single().second,
        )
    }

    @Test
    fun `GIVEN no exception for the category WHEN saving as its exception THEN it comes after the others`() = runTest {
        // Given
        val main = split(DefaultSplitMode.SHARES, mapOf(ANA to 3L, BRUNO to 1L), bar)
        prepareScenario(defaultSplit = main)
        val weights = mapOf(ANA to 50L, BRUNO to 50L)

        // When
        useCase(GROUP_ID, MARKET, DefaultSplitMode.PERCENT, weights)

        // Then
        assertEquals(
            main.copy(exceptions = listOf(bar, CategorySplit(MARKET, DefaultSplitMode.PERCENT, weights))),
            repository.savedDefaultSplits.single().second,
        )
    }

    @Test
    fun `GIVEN a Pela renda default WHEN saving as an exception THEN the default and its incomes stay`() = runTest {
        // Given
        val main = split(DefaultSplitMode.INCOME, mapOf(ANA to 75L, BRUNO to 25L))
        val incomes = mapOf(ANA to 900_000L, BRUNO to 300_000L)
        prepareScenario(
            defaultSplit = main,
            incomes = incomes,
        )
        val weights = mapOf(ANA to 1L, BRUNO to 1L)

        // When
        useCase(GROUP_ID, MARKET, DefaultSplitMode.EQUAL, weights)

        // Then
        assertEquals(
            main.copy(exceptions = listOf(CategorySplit(MARKET, DefaultSplitMode.EQUAL, weights))),
            repository.savedDefaultSplits.single().second,
        )
        assertEquals(incomes, repository.savedIncomes[GROUP_ID])
    }

    @Test
    fun `GIVEN a group without a default WHEN saving as an exception THEN Igual among everyone has it`() = runTest {
        // Given
        prepareScenario(defaultSplit = null)
        val weights = mapOf(ANA to 2L, BRUNO to 1L)

        // When
        val result = useCase(GROUP_ID, MARKET, DefaultSplitMode.SHARES, weights)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            split(DefaultSplitMode.EQUAL, emptyMap(), CategorySplit(MARKET, DefaultSplitMode.SHARES, weights)),
            repository.savedDefaultSplits.single().second,
        )
    }

    @Test
    fun `GIVEN the device fails to save WHEN saving as the group default THEN it fails`() = runTest {
        // Given
        val error = IllegalStateException("disk full")
        prepareScenario(
            defaultSplit = null,
            defaultSplitError = error,
        )

        // When
        val result = useCase(GROUP_ID, null, DefaultSplitMode.SHARES, mapOf(ANA to 2L, BRUNO to 1L))

        // Then
        assertSame(error, result.exceptionOrNull())
        assertTrue(repository.savedDefaultSplits.isEmpty())
    }

    @Test
    fun `GIVEN the device fails to save WHEN saving as an exception THEN it fails`() = runTest {
        // Given
        val error = IllegalStateException("disk full")
        prepareScenario(
            defaultSplit = split(DefaultSplitMode.SHARES, mapOf(ANA to 3L, BRUNO to 1L)),
            defaultSplitError = error,
        )

        // When
        val result = useCase(GROUP_ID, MARKET, DefaultSplitMode.EQUAL, mapOf(ANA to 1L, BRUNO to 0L))

        // Then
        assertSame(error, result.exceptionOrNull())
        assertTrue(repository.savedDefaultSplits.isEmpty())
    }

    @Test
    fun `GIVEN Pela renda incomes that cannot be read WHEN saving as an exception THEN nothing is saved`() = runTest {
        // Given
        val error = IllegalStateException("disk unreadable")
        prepareScenario(
            defaultSplit = split(DefaultSplitMode.INCOME, mapOf(ANA to 75L, BRUNO to 25L)),
            incomesError = error,
        )

        // When
        val result = useCase(GROUP_ID, MARKET, DefaultSplitMode.EQUAL, mapOf(ANA to 1L, BRUNO to 1L))

        // Then
        assertSame(error, result.exceptionOrNull())
        assertTrue(repository.savedDefaultSplits.isEmpty())
    }

    private val market = CategorySplit(MARKET, DefaultSplitMode.SHARES, mapOf(ANA to 2L, BRUNO to 1L))
    private val bar = CategorySplit("food.bar", DefaultSplitMode.EQUAL, mapOf(ANA to 1L, BRUNO to 0L))
    private val rent = CategorySplit("home.rent", DefaultSplitMode.PERCENT, mapOf(ANA to 70L, BRUNO to 30L))

    private fun split(
        mode: DefaultSplitMode,
        weights: Map<String, Long>,
        vararg exceptions: CategorySplit,
    ): GroupDefaultSplit = GroupDefaultSplit(mode, weights, exceptions.toList())

    private fun prepareScenario(
        defaultSplit: GroupDefaultSplit?,
        incomes: Map<String, Long> = emptyMap(),
        defaultSplitError: Throwable? = null,
        incomesError: Throwable? = null,
    ) {
        val group = Group(
            id = GROUP_ID,
            name = "Apê 302",
            type = GroupType.HOME,
            color = AvatarColor.PEACH,
            shape = GroupShape.COOKIE,
            photo = null,
            currency = GroupCurrency.BRL,
            members = emptyList(),
            isPending = false,
            inviteCode = null,
            isCreatedByMe = true,
            defaultSplit = defaultSplit,
            updatedAt = 0L,
            simplifiesDebts = false,
            isSimplifyDebtsPending = false,
            simplifyDebtsChangedBy = null,
            simplifyDebtsConflictBy = null,
        )
        repository = FakeGroupRepository(listOf(group)).also {
            it.savedIncomes[GROUP_ID] = incomes
            it.defaultSplitError = defaultSplitError
            it.incomesError = incomesError
        }
        useCase = SaveSplitAsDefaultUseCase(
            observeGroup = ObserveGroupUseCase(repository),
            getSplitIncomes = GetSplitIncomesUseCase(repository),
            updateDefaultSplit = UpdateDefaultSplitUseCase(repository) { 1_000L },
        )
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val ANA = "m-ana"
        const val BRUNO = "m-bruno"
        const val MARKET = "food.market"
    }
}
