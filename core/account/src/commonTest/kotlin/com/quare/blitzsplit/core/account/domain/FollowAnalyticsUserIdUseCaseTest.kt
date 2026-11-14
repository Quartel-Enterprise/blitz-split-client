package com.quare.blitzsplit.core.account.domain

import com.quare.blitzsplit.core.account.domain.usecase.impl.FollowAnalyticsUserIdUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.provider.analytics.AnalyticsHub
import com.quare.blitzsplit.core.provider.analytics.AnalyticsService
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FollowAnalyticsUserIdUseCaseTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var account: FakeAccountRepository
    private lateinit var hub: AnalyticsHub
    private lateinit var service: RecordingAnalyticsService

    @Test
    fun `GIVEN a local only install WHEN following the user id THEN Analytics gets no user id`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = AccountStatus.LocalOnly)

            // When
            val userIds = service.userIds.toList()

            // Then
            assertEquals(listOf<String?>(null), userIds)
        }

    @Test
    fun `GIVEN an anonymous account WHEN following the user id THEN Analytics gets its id`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = AccountStatus.Anonymous(ANONYMOUS_ID))

        // When
        val userIds = service.userIds.toList()

        // Then
        assertEquals(listOf<String?>(ANONYMOUS_ID), userIds)
    }

    @Test
    fun `GIVEN a Google account WHEN following the user id THEN Analytics gets its id`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = AccountStatus.Connected(PIERRE_ID, AccountProvider.GOOGLE))

        // When
        val userIds = service.userIds.toList()

        // Then
        assertEquals(listOf<String?>(PIERRE_ID), userIds)
    }

    @Test
    fun `GIVEN an Apple account WHEN following the user id THEN Analytics gets its id`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = AccountStatus.Connected(PIERRE_ID, AccountProvider.APPLE))

        // When
        val userIds = service.userIds.toList()

        // Then
        assertEquals(listOf<String?>(PIERRE_ID), userIds)
    }

    @Test
    fun `GIVEN a Google account with an email WHEN following the user id THEN Analytics never gets the email`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Connected(PIERRE_ID, AccountProvider.GOOGLE),
                email = PIERRE_EMAIL,
            )

            // When
            val userIds = service.userIds.toList()

            // Then
            assertEquals(listOf<String?>(PIERRE_ID), userIds)
        }

    @Test
    fun `GIVEN a local only install WHEN it registers an anonymous account THEN Analytics gets the new id`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = AccountStatus.LocalOnly)

            // When
            account.status = AccountStatus.Anonymous(PIERRE_ID)

            // Then
            assertEquals(listOf<String?>(null, PIERRE_ID), service.userIds)
        }

    @Test
    fun `GIVEN an anonymous account WHEN it connects Google with the same id THEN Analytics is not told again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = AccountStatus.Anonymous(PIERRE_ID))

            // When
            account.status = AccountStatus.Connected(PIERRE_ID, AccountProvider.GOOGLE)

            // Then
            assertEquals(listOf<String?>(PIERRE_ID), service.userIds)
        }

    @Test
    fun `GIVEN a local only install WHEN it goes anonymous and then connects Apple THEN Analytics gets the id once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = AccountStatus.LocalOnly)
            account.status = AccountStatus.Anonymous(PIERRE_ID)

            // When
            account.status = AccountStatus.Connected(PIERRE_ID, AccountProvider.APPLE)

            // Then
            assertEquals(listOf<String?>(null, PIERRE_ID), service.userIds)
        }

    @Test
    fun `GIVEN a Google account WHEN only its email changes THEN Analytics is not told again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = AccountStatus.Connected(PIERRE_ID, AccountProvider.GOOGLE))

            // When
            account.email = PIERRE_EMAIL

            // Then
            assertEquals(listOf<String?>(PIERRE_ID), service.userIds)
        }

    @Test
    fun `GIVEN a Google account WHEN it signs out THEN Analytics clears the user id`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = AccountStatus.Connected(PIERRE_ID, AccountProvider.GOOGLE))

        // When
        account.status = AccountStatus.LocalOnly

        // Then
        assertEquals(listOf<String?>(PIERRE_ID, null), service.userIds)
    }

    @Test
    fun `GIVEN an anonymous account WHEN it is deleted THEN Analytics clears the user id`() = runTest(testDispatcher) {
        // Given
        prepareScenario(status = AccountStatus.Anonymous(ANONYMOUS_ID))

        // When
        account.status = AccountStatus.LocalOnly

        // Then
        assertEquals(listOf<String?>(ANONYMOUS_ID, null), service.userIds)
    }

    @Test
    fun `GIVEN an anonymous account WHEN a recovery code restores another account THEN Analytics gets its id`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = AccountStatus.Anonymous(ANONYMOUS_ID))

            // When
            account.status = AccountStatus.Anonymous(ANA_ID)

            // Then
            assertEquals(listOf<String?>(ANONYMOUS_ID, ANA_ID), service.userIds)
        }

    @Test
    fun `GIVEN a Google account WHEN another Google account replaces it THEN Analytics gets the new id`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = AccountStatus.Connected(PIERRE_ID, AccountProvider.GOOGLE))

            // When
            account.status = AccountStatus.Connected(ANA_ID, AccountProvider.GOOGLE)

            // Then
            assertEquals(listOf<String?>(PIERRE_ID, ANA_ID), service.userIds)
        }

    @Test
    fun `GIVEN a signed out install WHEN the same person connects again THEN Analytics gets the id back`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(status = AccountStatus.Connected(PIERRE_ID, AccountProvider.GOOGLE))
            account.status = AccountStatus.LocalOnly

            // When
            account.status = AccountStatus.Connected(PIERRE_ID, AccountProvider.GOOGLE)

            // Then
            assertEquals(listOf<String?>(PIERRE_ID, null, PIERRE_ID), service.userIds)
        }

    @Test
    fun `GIVEN an account followed before attaching WHEN the service attaches THEN it gets the account id`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                status = AccountStatus.Anonymous(ANONYMOUS_ID),
                attachesService = false,
            )
            val lateService = RecordingAnalyticsService()

            // When
            hub.attach(lateService)

            // Then
            assertEquals(listOf<String?>(ANONYMOUS_ID), lateService.userIds)
        }

    private fun TestScope.prepareScenario(
        status: AccountStatus,
        email: String? = null,
        attachesService: Boolean = true,
    ) {
        account = FakeAccountRepository(initialStatus = status).also { it.email = email }
        hub = AnalyticsHub()
        service = RecordingAnalyticsService()
        if (attachesService) {
            hub.attach(service)
            // Why: attaching hands the service the hub's current id (none yet); only what the use case forwards counts.
            service.userIds.clear()
        }
        val followAnalyticsUserId = FollowAnalyticsUserIdUseCase(
            repository = account,
            hub = hub,
        )
        backgroundScope.launch { followAnalyticsUserId() }
    }

    private companion object {
        const val ANONYMOUS_ID = "anonymous-id"
        const val PIERRE_ID = "pierre-id"
        const val ANA_ID = "ana-id"
        const val PIERRE_EMAIL = "pierre@example.com"
    }
}

private class RecordingAnalyticsService : AnalyticsService {
    val userIds = mutableListOf<String?>()

    override fun logEvent(
        name: String,
        params: Map<String, Any>,
    ) {
    }

    override fun setCollectionEnabled(isEnabled: Boolean) {
    }

    override fun setUserId(userId: String?) {
        userIds += userId
    }
}
