package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.CardColor
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethod
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethodType
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.SavePayingMethodUseCase
import com.quare.blitzsplit.core.expenses.testing.FakePayingMethodRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SavePayingMethodUseCaseTest {
    private lateinit var repository: FakePayingMethodRepository
    private lateinit var useCase: SavePayingMethodUseCase

    @Test
    fun `GIVEN no methods WHEN adding a credit card THEN saves it with an id of its own added now`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = useCase(
            methodId = null,
            type = PayingMethodType.CREDIT_CARD,
            name = "Nubank",
            cardColor = CardColor.PURPLE,
            isDefault = false,
        )

        // Then
        assertTrue(result.isSuccess)
        val saved = repository.methods.value.single()
        assertTrue(saved.id.isNotBlank())
        assertEquals(PayingMethodType.CREDIT_CARD, saved.type)
        assertEquals("Nubank", saved.name)
        assertEquals(CardColor.PURPLE, saved.cardColor)
        assertEquals(NOW, saved.addedAt)
    }

    @Test
    fun `GIVEN a method WHEN adding another THEN the new one gets a different id`() = runTest {
        // Given
        prepareScenario(stored = listOf(nubank))

        // When
        useCase(
            methodId = null,
            type = PayingMethodType.PIX,
            name = "",
            cardColor = null,
            isDefault = false,
        )

        // Then
        assertEquals(2, repository.methods.value.size)
        assertNotEquals(
            nubank.id,
            repository.methods.value
                .last()
                .id,
        )
    }

    @Test
    fun `GIVEN a card WHEN editing it THEN keeps its id and when it was added`() = runTest {
        // Given
        prepareScenario(stored = listOf(nubank))

        // When
        val result = useCase(
            methodId = nubank.id,
            type = PayingMethodType.DEBIT_CARD,
            name = "Inter",
            cardColor = CardColor.ORANGE,
            isDefault = false,
        )

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            PayingMethod(
                id = nubank.id,
                type = PayingMethodType.DEBIT_CARD,
                name = "Inter",
                cardColor = CardColor.ORANGE,
                isDefault = false,
                addedAt = nubank.addedAt,
            ),
            repository.methods.value.single(),
        )
    }

    @Test
    fun `GIVEN a name with spaces around WHEN saving THEN saves it trimmed`() = runTest {
        // Given
        prepareScenario()

        // When
        useCase(
            methodId = null,
            type = PayingMethodType.BANK_TRANSFER,
            name = "  Itau  ",
            cardColor = null,
            isDefault = false,
        )

        // Then
        assertEquals(
            "Itau",
            repository.methods.value
                .single()
                .name,
        )
    }

    @Test
    fun `GIVEN a name over 30 characters WHEN saving THEN fails and saves nothing`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = useCase(
            methodId = null,
            type = PayingMethodType.CREDIT_CARD,
            name = "N".repeat(31),
            cardColor = null,
            isDefault = false,
        )

        // Then
        assertTrue(result.isFailure)
        assertTrue(repository.savedWrites.isEmpty())
    }

    @Test
    fun `GIVEN 30 characters with spaces around WHEN saving THEN saves them`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = useCase(
            methodId = null,
            type = PayingMethodType.CREDIT_CARD,
            name = "  " + "N".repeat(30) + "  ",
            cardColor = null,
            isDefault = false,
        )

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            "N".repeat(30),
            repository.methods.value
                .single()
                .name,
        )
    }

    @Test
    fun `GIVEN Other with a blank name WHEN saving THEN fails and saves nothing`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = useCase(
            methodId = null,
            type = PayingMethodType.OTHER,
            name = "   ",
            cardColor = null,
            isDefault = false,
        )

        // Then
        assertTrue(result.isFailure)
        assertTrue(repository.savedWrites.isEmpty())
    }

    @Test
    fun `GIVEN Other with a name WHEN saving THEN saves it`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = useCase(
            methodId = null,
            type = PayingMethodType.OTHER,
            name = "Meal voucher",
            cardColor = null,
            isDefault = false,
        )

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            PayingMethodType.OTHER,
            repository.methods.value
                .single()
                .type,
        )
    }

    @Test
    fun `GIVEN cash already registered WHEN adding another cash THEN fails and saves nothing`() = runTest {
        // Given
        prepareScenario(stored = listOf(cash))

        // When
        val result = useCase(
            methodId = null,
            type = PayingMethodType.CASH,
            name = "",
            cardColor = null,
            isDefault = false,
        )

        // Then
        assertTrue(result.isFailure)
        assertTrue(repository.savedWrites.isEmpty())
    }

    @Test
    fun `GIVEN cash already registered WHEN editing that cash THEN saves it`() = runTest {
        // Given
        prepareScenario(stored = listOf(cash))

        // When
        val result = useCase(
            methodId = cash.id,
            type = PayingMethodType.CASH,
            name = "Wallet",
            cardColor = null,
            isDefault = true,
        )

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            "Wallet",
            repository.methods.value
                .single()
                .name,
        )
    }

    @Test
    fun `GIVEN cash already registered WHEN turning a pix into cash THEN fails and saves nothing`() = runTest {
        // Given
        prepareScenario(stored = listOf(cash, pix))

        // When
        val result = useCase(
            methodId = pix.id,
            type = PayingMethodType.CASH,
            name = "",
            cardColor = null,
            isDefault = false,
        )

        // Then
        assertTrue(result.isFailure)
        assertTrue(repository.savedWrites.isEmpty())
    }

    @Test
    fun `GIVEN two cards WHEN adding a third card THEN saves it`() = runTest {
        // Given
        prepareScenario(stored = listOf(nubank, nubank.copy(id = "card-2")))

        // When
        val result = useCase(
            methodId = null,
            type = PayingMethodType.CREDIT_CARD,
            name = "Nubank",
            cardColor = null,
            isDefault = false,
        )

        // Then
        assertTrue(result.isSuccess)
        assertEquals(3, repository.methods.value.size)
    }

    @Test
    fun `GIVEN a default method WHEN adding a new default THEN the former loses it in the same write`() = runTest {
        // Given
        prepareScenario(stored = listOf(nubank.copy(isDefault = true), pix))

        // When
        useCase(
            methodId = null,
            type = PayingMethodType.CASH,
            name = "",
            cardColor = null,
            isDefault = true,
        )

        // Then
        val write = repository.savedWrites.single()
        assertEquals(2, write.size)
        assertEquals(nubank.copy(isDefault = false), write.single { it.id == nubank.id })
        assertTrue(write.single { it.id != nubank.id }.isDefault)
        assertEquals(1, repository.methods.value.count { it.isDefault })
    }

    @Test
    fun `GIVEN a default method WHEN adding a method that is not default THEN the former keeps it`() = runTest {
        // Given
        prepareScenario(stored = listOf(nubank.copy(isDefault = true)))

        // When
        useCase(
            methodId = null,
            type = PayingMethodType.PIX,
            name = "",
            cardColor = null,
            isDefault = false,
        )

        // Then
        assertEquals(1, repository.savedWrites.single().size)
        assertTrue(
            repository.methods.value
                .single { it.id == nubank.id }
                .isDefault,
        )
    }

    @Test
    fun `GIVEN the default method WHEN editing it still as default THEN writes only that method`() = runTest {
        // Given
        prepareScenario(stored = listOf(nubank.copy(isDefault = true), pix))

        // When
        useCase(
            methodId = nubank.id,
            type = PayingMethodType.CREDIT_CARD,
            name = "Inter",
            cardColor = CardColor.PURPLE,
            isDefault = true,
        )

        // Then
        val write = repository.savedWrites.single()
        assertEquals(listOf(nubank.id), write.map { it.id })
        assertTrue(write.single().isDefault)
    }

    @Test
    fun `GIVEN a pix with a card color WHEN saving THEN drops the color`() = runTest {
        // Given
        prepareScenario()

        // When
        useCase(
            methodId = null,
            type = PayingMethodType.PIX,
            name = "",
            cardColor = CardColor.RED,
            isDefault = false,
        )

        // Then
        assertNull(
            repository.methods.value
                .single()
                .cardColor,
        )
    }

    @Test
    fun `GIVEN a card turned into cash WHEN saving THEN drops its color`() = runTest {
        // Given
        prepareScenario(stored = listOf(nubank))

        // When
        useCase(
            methodId = nubank.id,
            type = PayingMethodType.CASH,
            name = "",
            cardColor = CardColor.PURPLE,
            isDefault = false,
        )

        // Then
        assertNull(
            repository.methods.value
                .single()
                .cardColor,
        )
    }

    @Test
    fun `GIVEN the device fails to save WHEN saving THEN fails`() = runTest {
        // Given
        prepareScenario(saveError = IllegalStateException("disk full"))

        // When
        val result = useCase(
            methodId = null,
            type = PayingMethodType.PIX,
            name = "",
            cardColor = null,
            isDefault = false,
        )

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN a method deleted on another device meanwhile WHEN saving its edit THEN saves it as a new method`() =
        runTest {
            // Given
            prepareScenario()

            // When
            useCase(
                methodId = "deleted-meanwhile",
                type = PayingMethodType.PIX,
                name = "Edited",
                cardColor = null,
                isDefault = false,
            )

            // Then
            val saved = repository.methods.value.single()
            assertNotEquals("deleted-meanwhile", saved.id)
            assertEquals(NOW, saved.addedAt)
        }

    private val nubank = PayingMethod(
        id = "card-1",
        type = PayingMethodType.CREDIT_CARD,
        name = "Nubank",
        cardColor = CardColor.PURPLE,
        isDefault = false,
        addedAt = 100L,
    )

    private val cash = PayingMethod(
        id = "cash-1",
        type = PayingMethodType.CASH,
        name = "",
        cardColor = null,
        isDefault = false,
        addedAt = 200L,
    )

    private val pix = PayingMethod(
        id = "pix-1",
        type = PayingMethodType.PIX,
        name = "",
        cardColor = null,
        isDefault = false,
        addedAt = 300L,
    )

    private fun prepareScenario(
        stored: List<PayingMethod> = emptyList(),
        saveError: Throwable? = null,
    ) {
        repository = FakePayingMethodRepository(initialMethods = stored).also { it.saveError = saveError }
        useCase = SavePayingMethodUseCase(
            repository = repository,
            currentTimeMillis = { NOW },
        )
    }

    private companion object {
        const val NOW = 7_000L
    }
}
