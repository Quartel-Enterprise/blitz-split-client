package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.BillLinkVisibility
import com.quare.blitzsplit.core.expenses.domain.model.BillShareLink
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.SetBillShareLinkUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeBillShareLinkRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SetBillShareLinkUseCaseTest {
    private lateinit var repository: FakeBillShareLinkRepository
    private lateinit var useCase: SetBillShareLinkUseCase

    @Test
    fun `GIVEN a bill without a link WHEN letting anyone open it THEN reads the new link`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = useCase(BILL_ID, BillLinkVisibility.ANYONE)

        // Then
        assertEquals(
            BillShareLink(FakeBillShareLinkRepository.NEW_CODE, BillLinkVisibility.ANYONE),
            result.getOrThrow(),
        )
        assertEquals(listOf(BILL_ID to BillLinkVisibility.ANYONE), repository.updatedLinks)
    }

    @Test
    fun `GIVEN a bill gone from the server WHEN setting its link THEN reads no link`() = runTest {
        // Given
        prepareScenario(gone = true)

        // When
        val result = useCase(BILL_ID, BillLinkVisibility.MEMBERS)

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `GIVEN no internet WHEN setting the link THEN fails`() = runTest {
        // Given
        prepareScenario(error = IllegalStateException("offline"))

        // When
        val result = useCase(BILL_ID, BillLinkVisibility.MEMBERS)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    private fun prepareScenario(
        gone: Boolean = false,
        error: Throwable? = null,
    ) {
        repository = FakeBillShareLinkRepository().also {
            if (gone) it.goneExpenseIds += BILL_ID
            it.updateError = error
        }
        useCase = SetBillShareLinkUseCase(repository)
    }

    private companion object {
        const val BILL_ID = "bill-1"
    }
}
