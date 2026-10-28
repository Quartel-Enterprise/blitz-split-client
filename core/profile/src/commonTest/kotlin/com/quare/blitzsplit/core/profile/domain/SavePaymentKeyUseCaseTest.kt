package com.quare.blitzsplit.core.profile.domain

import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.profile.domain.usecase.impl.SavePaymentKeyUseCase
import com.quare.blitzsplit.core.profile.testing.FakePaymentKeyRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SavePaymentKeyUseCaseTest {
    private lateinit var repository: FakePaymentKeyRepository
    private lateinit var useCase: SavePaymentKeyUseCase

    @Test
    fun `GIVEN no keys WHEN adding a key THEN saves it with a fresh id and the current time`() = runTest {
        // Given
        prepareScenario(now = 1_000L)

        // When
        useCase(
            keyId = null,
            type = PaymentKeyType.CPF,
            value = "12345678909",
            isDefault = true,
        )

        // Then
        val saved = repository.savedKeys.single().single()
        assertTrue(uuid.matches(saved.id))
        assertEquals(PaymentKeyType.CPF, saved.type)
        assertEquals("12345678909", saved.value)
        assertEquals(1_000L, saved.addedAt)
    }

    @Test
    fun `GIVEN a saved key WHEN adding another THEN the new one gets an id of its own`() = runTest {
        // Given
        prepareScenario(keys = listOf(pixDefault))

        // When
        useCase(
            keyId = null,
            type = PaymentKeyType.EMAIL,
            value = "pierre@email.com",
            isDefault = false,
        )

        // Then
        val saved = repository.savedKeys.single().single()
        assertTrue(uuid.matches(saved.id))
        assertNotEquals(pixDefault.id, saved.id)
    }

    @Test
    fun `GIVEN no Pix key WHEN adding one not as default THEN it is the Pix default anyway`() = runTest {
        // Given
        prepareScenario(keys = listOf(bitcoinDefault))

        // When
        useCase(
            keyId = null,
            type = PaymentKeyType.CPF,
            value = "12345678909",
            isDefault = false,
        )

        // Then
        assertTrue(
            repository.savedKeys
                .single()
                .single()
                .isDefault,
        )
    }

    @Test
    fun `GIVEN a Pix default WHEN adding a Pix key not as default THEN saves only the new key`() = runTest {
        // Given
        prepareScenario(keys = listOf(pixDefault))

        // When
        useCase(
            keyId = null,
            type = PaymentKeyType.EMAIL,
            value = "pierre@email.com",
            isDefault = false,
        )

        // Then
        val saved = repository.savedKeys.single().single()
        assertFalse(saved.isDefault)
        assertTrue(
            repository.keys.value
                .single { it.id == pixDefault.id }
                .isDefault,
        )
    }

    @Test
    fun `GIVEN a Pix default WHEN adding a Pix key as default THEN demotes the old default in the same save`() =
        runTest {
            // Given
            prepareScenario(keys = listOf(pixDefault, pixOther))

            // When
            useCase(
                keyId = null,
                type = PaymentKeyType.EMAIL,
                value = "pierre@email.com",
                isDefault = true,
            )

            // Then
            val saved = repository.savedKeys.single()
            assertEquals(2, saved.size)
            assertTrue(saved.single { it.value == "pierre@email.com" }.isDefault)
            assertEquals(pixDefault.copy(isDefault = false), saved.single { it.id == pixDefault.id })
        }

    @Test
    fun `GIVEN a Pix and a Bitcoin default WHEN adding a Pix key as default THEN the Bitcoin default stays`() =
        runTest {
            // Given
            prepareScenario(keys = listOf(pixDefault, bitcoinDefault))

            // When
            useCase(
                keyId = null,
                type = PaymentKeyType.EMAIL,
                value = "pierre@email.com",
                isDefault = true,
            )

            // Then
            assertTrue(repository.savedKeys.single().none { it.id == bitcoinDefault.id })
            assertTrue(
                repository.keys.value
                    .single { it.id == bitcoinDefault.id }
                    .isDefault,
            )
        }

    @Test
    fun `GIVEN a Bitcoin default WHEN making another Bitcoin key the default THEN only the Bitcoin default moves`() =
        runTest {
            // Given
            prepareScenario(keys = listOf(pixDefault, bitcoinDefault, bitcoinOther))

            // When
            useCase(
                keyId = bitcoinOther.id,
                type = bitcoinOther.type,
                value = bitcoinOther.value,
                isDefault = true,
            )

            // Then
            val saved = repository.savedKeys.single()
            assertEquals(
                setOf(bitcoinOther.copy(isDefault = true), bitcoinDefault.copy(isDefault = false)),
                saved.toSet(),
            )
        }

    @Test
    fun `GIVEN the Pix default WHEN saving it with default turned off THEN it stays the default`() = runTest {
        // Given
        prepareScenario(keys = listOf(pixDefault, pixOther))

        // When
        useCase(
            keyId = pixDefault.id,
            type = pixDefault.type,
            value = pixDefault.value,
            isDefault = false,
        )

        // Then
        val saved = repository.savedKeys.single()
        assertEquals(listOf(pixDefault), saved)
    }

    @Test
    fun `GIVEN a saved key WHEN editing its value THEN keeps its id and the time it was added`() = runTest {
        // Given
        prepareScenario(
            now = 9_000L,
            keys = listOf(pixDefault, pixOther),
        )

        // When
        useCase(
            keyId = pixOther.id,
            type = PaymentKeyType.EMAIL,
            value = "larissa@email.com",
            isDefault = false,
        )

        // Then
        val saved = repository.savedKeys.single().single()
        assertEquals(pixOther.copy(value = "larissa@email.com"), saved)
    }

    @Test
    fun `GIVEN the only Pix key WHEN editing it with default turned off THEN it stays the default`() = runTest {
        // Given
        prepareScenario(keys = listOf(pixDefault, bitcoinDefault))

        // When
        useCase(
            keyId = pixDefault.id,
            type = PaymentKeyType.CPF,
            value = "52998224725",
            isDefault = false,
        )

        // Then
        assertEquals(listOf(pixDefault.copy(value = "52998224725")), repository.savedKeys.single())
    }

    @Test
    fun `GIVEN an id no key has WHEN saving THEN adds it as a new key with a fresh id`() = runTest {
        // Given
        prepareScenario(
            now = 9_000L,
            keys = listOf(pixDefault),
        )

        // When
        useCase(
            keyId = "gone",
            type = PaymentKeyType.EMAIL,
            value = "pierre@email.com",
            isDefault = false,
        )

        // Then
        val saved = repository.savedKeys.single().single()
        assertNotEquals("gone", saved.id)
        assertEquals(9_000L, saved.addedAt)
    }

    private val pixDefault = PaymentKey(
        id = "pix-1",
        type = PaymentKeyType.CPF,
        value = "12345678909",
        isDefault = true,
        addedAt = 10L,
    )

    private val pixOther = PaymentKey(
        id = "pix-2",
        type = PaymentKeyType.EMAIL,
        value = "pierre@email.com",
        isDefault = false,
        addedAt = 20L,
    )

    private val bitcoinDefault = PaymentKey(
        id = "btc-1",
        type = PaymentKeyType.LIGHTNING,
        value = "pierre@getalby.com",
        isDefault = true,
        addedAt = 30L,
    )

    private val bitcoinOther = PaymentKey(
        id = "btc-2",
        type = PaymentKeyType.ONCHAIN,
        value = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
        isDefault = false,
        addedAt = 40L,
    )

    private val uuid = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

    private fun prepareScenario(
        now: Long = 0L,
        keys: List<PaymentKey> = emptyList(),
    ) {
        repository = FakePaymentKeyRepository(initialKeys = keys)
        useCase = SavePaymentKeyUseCase(
            repository = repository,
            currentTimeMillis = { now },
        )
    }
}
