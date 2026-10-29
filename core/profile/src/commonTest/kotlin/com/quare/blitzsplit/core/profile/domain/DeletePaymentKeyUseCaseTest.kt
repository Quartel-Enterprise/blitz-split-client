package com.quare.blitzsplit.core.profile.domain

import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.profile.domain.usecase.impl.DeletePaymentKeyUseCase
import com.quare.blitzsplit.core.profile.testing.FakePaymentKeyRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeletePaymentKeyUseCaseTest {
    private lateinit var repository: FakePaymentKeyRepository
    private lateinit var useCase: DeletePaymentKeyUseCase

    @Test
    fun `GIVEN the Pix default and two other Pix keys WHEN deleting the default THEN the oldest one left takes over`() =
        runTest {
            // Given
            prepareScenario(keys = listOf(pixDefault, pixNewest, pixOldest, bitcoinDefault))

            // When
            useCase(pixDefault.id)

            // Then
            assertEquals(listOf(pixDefault.id), repository.deletedIds)
            assertEquals(listOf(listOf(pixOldest.copy(isDefault = true))), repository.savedKeys)
        }

    @Test
    fun `GIVEN the only Pix key and a Bitcoin key WHEN deleting the Pix key THEN no Bitcoin key changes`() = runTest {
        // Given
        prepareScenario(keys = listOf(pixDefault, bitcoinDefault, bitcoinOther))

        // When
        useCase(pixDefault.id)

        // Then
        assertEquals(listOf(pixDefault.id), repository.deletedIds)
        assertTrue(repository.savedKeys.isEmpty())
    }

    @Test
    fun `GIVEN the Bitcoin default WHEN deleting it THEN the other Bitcoin key takes over and no Pix key changes`() =
        runTest {
            // Given
            prepareScenario(keys = listOf(pixDefault, pixOldest, bitcoinDefault, bitcoinOther))

            // When
            useCase(bitcoinDefault.id)

            // Then
            assertEquals(listOf(listOf(bitcoinOther.copy(isDefault = true))), repository.savedKeys)
        }

    @Test
    fun `GIVEN a Pix key that is not the default WHEN deleting it THEN no default changes`() = runTest {
        // Given
        prepareScenario(keys = listOf(pixDefault, pixOldest, pixNewest))

        // When
        useCase(pixNewest.id)

        // Then
        assertEquals(listOf(pixNewest.id), repository.deletedIds)
        assertTrue(repository.savedKeys.isEmpty())
    }

    @Test
    fun `GIVEN an id no key has WHEN deleting THEN deletes and saves nothing`() = runTest {
        // Given
        prepareScenario(keys = listOf(pixDefault, pixOldest))

        // When
        useCase("gone")

        // Then
        assertTrue(repository.deletedIds.isEmpty())
        assertTrue(repository.savedKeys.isEmpty())
    }

    private val pixDefault = PaymentKey(
        id = "pix-default",
        type = PaymentKeyType.CPF,
        value = "12345678909",
        isDefault = true,
        addedAt = 20L,
    )

    private val pixOldest = PaymentKey(
        id = "pix-oldest",
        type = PaymentKeyType.EMAIL,
        value = "pierre@email.com",
        isDefault = false,
        addedAt = 10L,
    )

    private val pixNewest = PaymentKey(
        id = "pix-newest",
        type = PaymentKeyType.PHONE,
        value = "+5511912345678",
        isDefault = false,
        addedAt = 30L,
    )

    private val bitcoinDefault = PaymentKey(
        id = "btc-default",
        type = PaymentKeyType.LIGHTNING,
        value = "pierre@getalby.com",
        isDefault = true,
        addedAt = 5L,
    )

    private val bitcoinOther = PaymentKey(
        id = "btc-other",
        type = PaymentKeyType.ONCHAIN,
        value = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
        isDefault = false,
        addedAt = 40L,
    )

    private fun prepareScenario(keys: List<PaymentKey>) {
        repository = FakePaymentKeyRepository(initialKeys = keys)
        useCase = DeletePaymentKeyUseCase(repository)
    }
}
