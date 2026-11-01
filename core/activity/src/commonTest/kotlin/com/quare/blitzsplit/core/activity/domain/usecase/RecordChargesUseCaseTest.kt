package com.quare.blitzsplit.core.activity.domain.usecase

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountStatusUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.activity.domain.model.ChargeSent
import com.quare.blitzsplit.core.activity.domain.usecase.impl.RecordChargesUseCase
import com.quare.blitzsplit.core.activity.testing.FakeActivityRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RecordChargesUseCaseTest {
    private lateinit var activity: FakeActivityRepository
    private lateinit var useCase: RecordChargesUseCase

    private val charges = listOf(
        ChargeSent(
            groupId = "group-1",
            memberId = "carla",
            memberName = "Carla",
            amountMinor = 1_000L,
        ),
        ChargeSent(
            groupId = "group-2",
            memberId = "carla-2",
            memberName = "Carla",
            amountMinor = 2_290L,
        ),
    )

    @Test
    fun `GIVEN a Google account WHEN recording charges THEN saves them from the user at the time sent`() = runTest {
        // Given
        prepareScenario(
            status = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.GOOGLE),
        )

        // When
        val result = useCase(charges)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(charges, activity.recordedCharges)
        assertEquals(listOf<String?>(FakeAccountRepository.CONNECTED_USER_ID), activity.chargeActorUserIds)
        assertEquals(listOf(NOW), activity.chargeSentAts)
    }

    @Test
    fun `GIVEN no account yet WHEN recording charges offline THEN saves them with no actor`() = runTest {
        // Given
        prepareScenario(status = AccountStatus.LocalOnly)

        // When
        val result = useCase(charges)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(charges, activity.recordedCharges)
        assertEquals(listOf<String?>(null), activity.chargeActorUserIds)
    }

    @Test
    fun `GIVEN no charge WHEN recording THEN saves nothing`() = runTest {
        // Given
        prepareScenario(status = AccountStatus.LocalOnly)

        // When
        val result = useCase(emptyList())

        // Then
        assertTrue(result.isSuccess)
        assertTrue(activity.chargeSentAts.isEmpty())
    }

    @Test
    fun `GIVEN the device fails to save WHEN recording charges THEN returns the failure`() = runTest {
        // Given
        prepareScenario(status = AccountStatus.LocalOnly)
        activity.recordError = IllegalStateException("database closed")

        // When
        val result = useCase(charges)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    private fun prepareScenario(status: AccountStatus) {
        activity = FakeActivityRepository()
        useCase = RecordChargesUseCase(
            repository = activity,
            observeAccountStatus = ObserveAccountStatusUseCase(FakeAccountRepository(initialStatus = status)),
            currentTimeMillis = { NOW },
        )
    }

    private companion object {
        const val NOW = 9_000L
    }
}
