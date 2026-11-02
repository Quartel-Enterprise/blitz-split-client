package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.BillLinkVisibility
import com.quare.blitzsplit.core.expenses.domain.model.BillShareLink
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.LoadBillShareLinkUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeBillShareLinkRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoadBillShareLinkUseCaseTest {
    private lateinit var repository: FakeBillShareLinkRepository
    private lateinit var useCase: LoadBillShareLinkUseCase

    @Test
    fun `GIVEN a bill with a link WHEN loading it THEN reads that bill link`() = runTest {
        // Given
        prepareScenario(
            links = mapOf(BILL_ID to link, "bill-2" to BillShareLink("Zz9Yy8Xx", BillLinkVisibility.ANYONE)),
        )

        // When
        val result = useCase(BILL_ID)

        // Then
        assertEquals(link, result.getOrThrow())
        assertEquals(listOf(BILL_ID), repository.loadedLinkIds)
    }

    @Test
    fun `GIVEN a bill without a link WHEN loading it THEN reads no link`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = useCase(BILL_ID)

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `GIVEN no internet WHEN loading the link THEN fails`() = runTest {
        // Given
        prepareScenario(error = IllegalStateException("offline"))

        // When
        val result = useCase(BILL_ID)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    private val link = BillShareLink(
        code = "7Hk2pQ8x",
        visibility = BillLinkVisibility.MEMBERS,
    )

    private fun prepareScenario(
        links: Map<String, BillShareLink> = emptyMap(),
        error: Throwable? = null,
    ) {
        repository = FakeBillShareLinkRepository(initialLinks = links).also { it.getLinkError = error }
        useCase = LoadBillShareLinkUseCase(repository)
    }

    private companion object {
        const val BILL_ID = "bill-1"
    }
}
