package com.quare.blitzsplit.core.profile.domain

import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveMemberPaymentKeysUseCase
import com.quare.blitzsplit.core.profile.testing.FakePaymentKeyRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObserveMemberPaymentKeysUseCaseTest {
    private lateinit var repository: FakePaymentKeyRepository
    private lateinit var useCase: ObserveMemberPaymentKeysUseCase

    @Test
    fun `GIVEN Ana has a Pix and a Bitcoin key cached WHEN observing the keys of Ana THEN gives both in order`() =
        runTest {
            // Given
            prepareScenario(memberKeys = mapOf("ana" to listOf(anaPhone, anaLightning)))

            // When
            val keys = useCase("ana").first()

            // Then
            assertEquals(listOf(anaPhone, anaLightning), keys)
        }

    @Test
    fun `GIVEN keys of Ana and Bruno WHEN observing the keys of Bruno THEN gives only those of Bruno`() = runTest {
        // Given
        prepareScenario(
            memberKeys = mapOf(
                "ana" to listOf(anaPhone),
                "bruno" to listOf(brunoCpf),
            ),
        )

        // When
        val keys = useCase("bruno").first()

        // Then
        assertEquals(listOf(brunoCpf), keys)
    }

    @Test
    fun `GIVEN a member with no key cached WHEN observing their keys THEN gives none`() = runTest {
        // Given
        prepareScenario(memberKeys = mapOf("ana" to listOf(anaPhone)))

        // When
        val keys = useCase("carla").first()

        // Then
        assertTrue(keys.isEmpty())
    }

    @Test
    fun `GIVEN the keys of Ana open WHEN a sync brings a new key of hers THEN gives the new keys`() = runTest {
        // Given
        prepareScenario(memberKeys = mapOf("ana" to listOf(anaPhone)))
        val emitted = mutableListOf<List<PaymentKey>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { useCase("ana").collect(emitted::add) }

        // When
        repository.memberKeys.value = mapOf("ana" to listOf(anaPhone, anaLightning))

        // Then
        assertEquals(listOf(anaPhone, anaLightning), emitted.last())
    }

    private val anaPhone = PaymentKey(
        id = "ana-phone",
        type = PaymentKeyType.PHONE,
        value = "+5521987654321",
        isDefault = true,
        addedAt = 1L,
    )

    private val anaLightning = PaymentKey(
        id = "ana-lightning",
        type = PaymentKeyType.LIGHTNING,
        value = "ana@getalby.com",
        isDefault = true,
        addedAt = 2L,
    )

    private val brunoCpf = PaymentKey(
        id = "bruno-cpf",
        type = PaymentKeyType.CPF,
        value = "12345678909",
        isDefault = true,
        addedAt = 1L,
    )

    private fun prepareScenario(memberKeys: Map<String, List<PaymentKey>>) {
        repository = FakePaymentKeyRepository().also { it.memberKeys.value = memberKeys }
        useCase = ObserveMemberPaymentKeysUseCase(repository)
    }
}
