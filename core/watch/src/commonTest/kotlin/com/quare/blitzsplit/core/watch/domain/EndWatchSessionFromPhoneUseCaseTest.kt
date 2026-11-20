package com.quare.blitzsplit.core.watch.domain

import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.watch.domain.model.WatchSignOutNotice
import com.quare.blitzsplit.core.watch.domain.usecase.impl.EndWatchSessionFromPhoneUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EndWatchSessionFromPhoneUseCaseTest {
    private lateinit var notice: WatchSignOutNotice
    private lateinit var useCase: EndWatchSessionFromPhoneUseCase
    private var signOutCount = 0

    @Test
    fun `GIVEN the Google user the phone left WHEN ending it THEN signs out once synced with the notice`() = runTest {
        // Given
        prepareScenario(status = AccountStatus.Connected(PIERRE_ID, AccountProvider.GOOGLE))

        // When
        val result = useCase(PIERRE_ID)

        // Then
        assertEquals(Result.success(true), result)
        assertEquals(1, signOutCount)
        assertTrue(notice.isShown.value)
    }

    @Test
    fun `GIVEN the watch has the anonymous user the phone left WHEN ending the session THEN signs out once synced`() =
        runTest {
            // Given
            prepareScenario(status = AccountStatus.Anonymous(PIERRE_ID))

            // When
            val result = useCase(PIERRE_ID)

            // Then
            assertEquals(Result.success(true), result)
            assertEquals(1, signOutCount)
            assertTrue(notice.isShown.value)
        }

    @Test
    fun `GIVEN the watch is signed in to another user WHEN the phone signs out THEN leaves the watch as it is`() =
        runTest {
            // Given
            prepareScenario(status = AccountStatus.Connected(ANA_ID, AccountProvider.APPLE))

            // When
            val result = useCase(PIERRE_ID)

            // Then
            assertEquals(Result.success(false), result)
            assertEquals(0, signOutCount)
            assertFalse(notice.isShown.value)
        }

    @Test
    fun `GIVEN a local only watch WHEN the phone signs out THEN leaves the watch as it is`() = runTest {
        // Given
        prepareScenario(status = AccountStatus.LocalOnly)

        // When
        val result = useCase(PIERRE_ID)

        // Then
        assertEquals(Result.success(false), result)
        assertEquals(0, signOutCount)
        assertFalse(notice.isShown.value)
    }

    private fun prepareScenario(status: AccountStatus) {
        notice = WatchSignOutNotice()
        useCase = EndWatchSessionFromPhoneUseCase(
            accountRepository = FakeAccountRepository(initialStatus = status),
            signOutWhenSynced = { signOutCount++ },
            notice = notice,
        )
    }

    private companion object {
        const val PIERRE_ID = "pierre"
        const val ANA_ID = "ana"
    }
}
