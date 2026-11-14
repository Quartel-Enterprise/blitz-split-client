package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.CardColor
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethod
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethodType
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveBillPayingMethodUseCase
import com.quare.blitzsplit.core.expenses.testing.FakePayingMethodRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObserveBillPayingMethodUseCaseTest {
    private lateinit var repository: FakePayingMethodRepository
    private lateinit var useCase: ObserveBillPayingMethodUseCase

    @Test
    fun `GIVEN a bill marked with a card WHEN observing its payment method THEN gives the card`() = runTest {
        // Given
        prepareScenario(marks = mapOf(DINNER to nubank.id))

        // When
        val method = useCase(DINNER).first()

        // Then
        assertEquals(nubank, method)
    }

    @Test
    fun `GIVEN a bill never marked WHEN observing its payment method THEN gives none`() = runTest {
        // Given
        prepareScenario(marks = mapOf("another-bill" to nubank.id))

        // When
        val method = useCase(DINNER).first()

        // Then
        assertNull(method)
    }

    @Test
    fun `GIVEN a bill marked with a method since deleted WHEN observing its payment method THEN gives none`() =
        runTest {
            // Given
            prepareScenario(marks = mapOf(DINNER to "deleted-method"))

            // When
            val method = useCase(DINNER).first()

            // Then
            assertNull(method)
        }

    @Test
    fun `GIVEN a marked bill WHEN its method is deleted THEN the bill shows none`() = runTest {
        // Given
        prepareScenario(marks = mapOf(DINNER to nubank.id))
        val methods = useCase(DINNER)

        // When
        repository.deleteMethod(nubank.id)

        // Then
        assertNull(methods.first())
    }

    private val nubank = PayingMethod(
        id = "card-1",
        type = PayingMethodType.CREDIT_CARD,
        name = "Nubank",
        cardColor = CardColor.PURPLE,
        isDefault = true,
        addedAt = 1L,
    )

    private fun prepareScenario(marks: Map<String, String>) {
        repository = FakePayingMethodRepository(
            initialMethods = listOf(nubank),
            initialMarks = marks,
        )
        useCase = ObserveBillPayingMethodUseCase(repository)
    }

    private companion object {
        const val DINNER = "dinner"
    }
}
