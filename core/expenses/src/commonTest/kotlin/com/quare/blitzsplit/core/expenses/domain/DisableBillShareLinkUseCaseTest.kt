package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.BillLinkVisibility
import com.quare.blitzsplit.core.expenses.domain.model.BillShareLink
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DisableBillShareLinkUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeBillShareLinkRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DisableBillShareLinkUseCaseTest {
    private lateinit var repository: FakeBillShareLinkRepository
    private lateinit var useCase: DisableBillShareLinkUseCase

    @Test
    fun `GIVEN a bill with a link WHEN disabling it THEN the bill has no link anymore`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = useCase(BILL_ID)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(BILL_ID), repository.disabledExpenseIds)
        assertTrue(BILL_ID !in repository.links.value)
    }

    @Test
    fun `GIVEN no internet WHEN disabling the link THEN fails`() = runTest {
        // Given
        prepareScenario(error = IllegalStateException("offline"))

        // When
        val result = useCase(BILL_ID)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    private fun prepareScenario(error: Throwable? = null) {
        repository = FakeBillShareLinkRepository(
            initialLinks = mapOf(BILL_ID to BillShareLink("7Hk2pQ8x", BillLinkVisibility.ANYONE)),
        ).also { it.disableError = error }
        useCase = DisableBillShareLinkUseCase(repository)
    }

    private companion object {
        const val BILL_ID = "bill-1"
    }
}
