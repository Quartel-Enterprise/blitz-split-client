package com.quare.blitzsplit.core.sync.signin

import com.quare.blitzsplit.core.account.domain.model.NativeSignIn
import com.quare.blitzsplit.core.account.domain.usecase.impl.ConnectWithIdTokenUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAbandonedBrowserSignInUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountStatusUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveReturningBrowserSignInUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.sync.domain.model.SignInOutcome
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ProviderSignInImplTest {
    private lateinit var account: FakeAccountRepository
    private lateinit var signIn: ProviderSignInImpl

    @Test
    fun `GIVEN Apple native sign in WHEN its ID token comes with an authorization code THEN signs in with it`() =
        runTest {
            // Given
            prepareScenario()

            // When
            val step = signIn.onIdTokenReceived(
                provider = AccountProvider.APPLE,
                idToken = "apple-id-token",
                rawNonce = "raw-nonce",
                fullName = "Pierre Vieira",
                authorizationCode = "apple-authorization-code",
            )

            // Then
            assertEquals(SignInStep.SignedIn(SignInOutcome.PROFILE_RESTORED), step)
            assertEquals(listOf(AccountProvider.APPLE to "apple-id-token"), account.idTokenConnections)
            assertEquals(listOf<String?>("apple-authorization-code"), account.idTokenAuthorizationCodes)
        }

    @Test
    fun `GIVEN Google native sign in WHEN its ID token comes with no authorization code THEN signs in with none`() =
        runTest {
            // Given
            prepareScenario()

            // When
            signIn.onIdTokenReceived(
                provider = AccountProvider.GOOGLE,
                idToken = "google-id-token",
                rawNonce = null,
                fullName = null,
                authorizationCode = null,
            )

            // Then
            assertEquals(listOf<String?>(null), account.idTokenAuthorizationCodes)
        }

    private fun prepareScenario() {
        account = FakeAccountRepository()
        signIn = ProviderSignInImpl(
            nativeSignIn = NativeSignIn(setOf(AccountProvider.APPLE, AccountProvider.GOOGLE)),
            connectWithIdToken = ConnectWithIdTokenUseCase(account),
            connectWithBrowser = { _, _ -> Result.success(Unit) },
            completeSignIn = { Result.success(SignInOutcome.PROFILE_RESTORED) },
            observeAccountStatus = ObserveAccountStatusUseCase(account),
            observeReturningBrowserSignIn = ObserveReturningBrowserSignInUseCase(account),
            observeAbandonedBrowserSignIn = ObserveAbandonedBrowserSignInUseCase(account),
        )
    }
}
