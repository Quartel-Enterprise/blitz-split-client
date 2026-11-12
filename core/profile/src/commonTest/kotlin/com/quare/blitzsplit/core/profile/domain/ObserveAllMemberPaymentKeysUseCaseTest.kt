package com.quare.blitzsplit.core.profile.domain

import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveAllMemberPaymentKeysUseCase
import com.quare.blitzsplit.core.profile.testing.FakePaymentKeyRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObserveAllMemberPaymentKeysUseCaseTest {
    private lateinit var repository: FakePaymentKeyRepository
    private lateinit var useCase: ObserveAllMemberPaymentKeysUseCase

    @Test
    fun `GIVEN keys of Ana and Bruno cached WHEN observing every member key THEN gives the keys of both`() = runTest {
        // Given
        prepareScenario(
            memberKeys = mapOf(
                "ana" to listOf(anaPhone),
                "bruno" to listOf(brunoCpf),
            ),
        )

        // When
        val keys = useCase().first()

        // Then
        assertEquals(listOf(anaPhone, brunoCpf), keys)
    }

    @Test
    fun `GIVEN no member key cached WHEN observing every member key THEN gives none`() = runTest {
        // Given
        prepareScenario(memberKeys = emptyMap())

        // When
        val keys = useCase().first()

        // Then
        assertTrue(keys.isEmpty())
    }

    @Test
    fun `GIVEN the member keys open WHEN a sync brings a key of Bruno THEN gives the new keys`() = runTest {
        // Given
        prepareScenario(memberKeys = mapOf("ana" to listOf(anaPhone)))
        val emitted = mutableListOf<List<PaymentKey>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { useCase().collect(emitted::add) }

        // When
        repository.memberKeys.value = mapOf("ana" to listOf(anaPhone), "bruno" to listOf(brunoCpf))

        // Then
        assertEquals(listOf(anaPhone, brunoCpf), emitted.last())
    }

    private val anaPhone = PaymentKey(
        id = "ana-phone",
        type = PaymentKeyType.PHONE,
        value = "+5521987654321",
        isDefault = true,
        addedAt = 1L,
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
        useCase = ObserveAllMemberPaymentKeysUseCase(repository)
    }
}
