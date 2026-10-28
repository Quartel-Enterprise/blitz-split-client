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

    @Test
    fun `GIVEN a browser link under way WHEN connecting again THEN starts no second link`() = runTest {
        // Given
        prepareScenario()
        connector.connect(AccountProvider.GOOGLE, nativeLaunches::add)

        // When
        connector.connect(AccountProvider.APPLE, nativeLaunches::add)

        // Then
        assertEquals(listOf(AccountProvider.GOOGLE), browserLinks)
        assertEquals(AccountProvider.GOOGLE, connector.connection.value.connectingProvider)
    }

    @Test
    fun `GIVEN a native sheet open WHEN connecting again THEN opens no second sheet`() = runTest {
        // Given
        prepareScenario(native = setOf(AccountProvider.APPLE))
        connector.connect(AccountProvider.APPLE, nativeLaunches::add)

        // When
        connector.connect(AccountProvider.APPLE, nativeLaunches::add)

        // Then
        assertEquals(listOf(AccountProvider.APPLE), nativeLaunches)
    }

    private fun prepareScenario(native: Set<AccountProvider> = emptySet()) {
        nativeLaunches.clear()
        browserLinks.clear()
        account = FakeAccountRepository()
        connector = ProviderConnectorImpl(
            nativeSignIn = NativeSignIn(native),
            connectWithIdToken = ConnectWithIdTokenUseCase(account),
            connectWithBrowser = { provider ->
                browserLinks += provider
                Result.success(Unit)
            },
            completeSignIn = { Result.success(SignInOutcome.PROFILE_RESTORED) },
            observeAccount = ObserveAccountUseCase(account),
            observeReturningBrowserSignIn = ObserveReturningBrowserSignInUseCase(account),
            observeAbandonedBrowserSignIn = ObserveAbandonedBrowserSignInUseCase(account),
        )
    }
}
