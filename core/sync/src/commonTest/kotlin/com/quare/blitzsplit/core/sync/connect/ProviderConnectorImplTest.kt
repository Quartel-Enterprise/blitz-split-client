package com.quare.blitzsplit.core.sync.connect

import com.quare.blitzsplit.core.account.domain.model.NativeSignIn
import com.quare.blitzsplit.core.account.domain.usecase.impl.ConnectWithIdTokenUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAbandonedBrowserSignInUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveReturningBrowserSignInUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.sync.domain.model.SignInOutcome
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ProviderConnectorImplTest {
    private lateinit var account: FakeAccountRepository
    private lateinit var connector: ProviderConnectorImpl
    private val nativeLaunches = mutableListOf<AccountProvider>()
    private val browserLinks = mutableListOf<AccountProvider>()
    private val browserLinksFromProfile = mutableListOf<Boolean>()

    @Test
    fun `GIVEN a browser link under way WHEN connecting again THEN starts no second link`() = runTest {
        // Given
        prepareScenario()
        connector.connect(AccountProvider.GOOGLE, isFromProfile = false, nativeLaunches::add)

        // When
        connector.connect(AccountProvider.APPLE, isFromProfile = false, nativeLaunches::add)

        // Then
        assertEquals(listOf(AccountProvider.GOOGLE), browserLinks)
        assertEquals(AccountProvider.GOOGLE, connector.connection.value.connectingProvider)
    }

    @Test
    fun `GIVEN the profile WHEN connecting Google in the browser THEN says the link leaves from the profile`() =
        runTest {
            // Given
            prepareScenario()

            // When
            connector.connect(AccountProvider.GOOGLE, isFromProfile = true, nativeLaunches::add)

            // Then
            assertEquals(listOf(AccountProvider.GOOGLE), browserLinks)
            assertEquals(listOf(true), browserLinksFromProfile)
        }

    @Test
    fun `GIVEN a native sheet open WHEN connecting again THEN opens no second sheet`() = runTest {
        // Given
        prepareScenario(native = setOf(AccountProvider.APPLE))
        connector.connect(AccountProvider.APPLE, isFromProfile = false, nativeLaunches::add)

        // When
        connector.connect(AccountProvider.APPLE, isFromProfile = false, nativeLaunches::add)

        // Then
        assertEquals(listOf(AccountProvider.APPLE), nativeLaunches)
    }

    @Test
    fun `GIVEN Apple native sign in WHEN its ID token arrives with an authorization code THEN links with that code`() =
        runTest {
            // Given
            prepareScenario(native = setOf(AccountProvider.APPLE))
            connector.connect(AccountProvider.APPLE, isFromProfile = false, nativeLaunches::add)

            // When
            connector.onIdTokenReceived(
                provider = AccountProvider.APPLE,
                idToken = "apple-id-token",
                rawNonce = "raw-nonce",
                fullName = "Pierre Vieira",
                authorizationCode = "apple-authorization-code",
            )

            // Then
            assertEquals(listOf(AccountProvider.APPLE to "apple-id-token"), account.idTokenConnections)
            assertEquals(listOf<String?>("apple-authorization-code"), account.idTokenAuthorizationCodes)
        }

    @Test
    fun `GIVEN Google native sign in WHEN its ID token arrives without an authorization code THEN links with none`() =
        runTest {
            // Given
            prepareScenario(native = setOf(AccountProvider.GOOGLE))
            connector.connect(AccountProvider.GOOGLE, isFromProfile = false, nativeLaunches::add)

            // When
            connector.onIdTokenReceived(
                provider = AccountProvider.GOOGLE,
                idToken = "google-id-token",
                rawNonce = null,
                fullName = null,
                authorizationCode = null,
            )

            // Then
            assertEquals(listOf<String?>(null), account.idTokenAuthorizationCodes)
        }

    private fun prepareScenario(native: Set<AccountProvider> = emptySet()) {
        account = FakeAccountRepository()
        connector = ProviderConnectorImpl(
            nativeSignIn = NativeSignIn(native),
            connectWithIdToken = ConnectWithIdTokenUseCase(account),
            connectWithBrowser = { provider, isFromProfile ->
                browserLinks += provider
                browserLinksFromProfile += isFromProfile
                Result.success(Unit)
            },
            completeSignIn = { Result.success(SignInOutcome.PROFILE_RESTORED) },
            observeAccount = ObserveAccountUseCase(account),
            observeReturningBrowserSignIn = ObserveReturningBrowserSignInUseCase(account),
            observeAbandonedBrowserSignIn = ObserveAbandonedBrowserSignInUseCase(account),
        )
    }
}
