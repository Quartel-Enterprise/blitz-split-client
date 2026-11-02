package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.SharedBill
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.LoadSharedBillUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeBillShareLinkRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LoadSharedBillUseCaseTest {
    private lateinit var repository: FakeBillShareLinkRepository
    private lateinit var useCase: LoadSharedBillUseCase

    @Test
    fun `GIVEN a link only members open WHEN loading its bill THEN reads it members only`() = runTest {
        // Given
        prepareScenario(sharedBills = mapOf(CODE to SharedBill.MembersOnly))

        // When
        val result = useCase(CODE)

        // Then
        assertEquals(SharedBill.MembersOnly, result.getOrThrow())
        assertEquals(listOf(CODE), repository.loadedCodes)
    }

    @Test
    fun `GIVEN a code that is no link WHEN loading its bill THEN reads it not found`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = useCase(CODE)

        // Then
        assertEquals(SharedBill.NotFound, result.getOrThrow())
    }

    @Test
    fun `GIVEN no internet WHEN loading the bill THEN fails`() = runTest {
        // Given
        prepareScenario(error = IllegalStateException("offline"))

        // When
        val result = useCase(CODE)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    private fun prepareScenario(
        sharedBills: Map<String, SharedBill> = emptyMap(),
        error: Throwable? = null,
    ) {
        repository = FakeBillShareLinkRepository(initialSharedBills = sharedBills).also { it.sharedBillError = error }
        useCase = LoadSharedBillUseCase(repository)
    }

    private companion object {
        const val CODE = "7Hk2pQ8x"
    }
}
