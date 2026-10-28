package com.quare.blitzsplit.core.account.domain

import com.quare.blitzsplit.core.account.domain.usecase.impl.ConnectWithBrowserUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ConnectWithBrowserUseCaseTest {
    @Test
    fun `GIVEN the profile on screen WHEN connecting Google in the browser THEN the trip leaves from the profile`() =
        runTest {
            // Given
            val account = FakeAccountRepository()
            val navigator = BackStackNavigator(startRoute = Route.Groups).also { it.navigate(Route.Profile) }
            val useCase = ConnectWithBrowserUseCase(account, navigator)

            // When
            useCase(AccountProvider.GOOGLE)

            // Then
            assertEquals(listOf(AccountProvider.GOOGLE), account.browserConnections)
            assertEquals(listOf(true), account.browserConnectionsFromProfile)
        }

    @Test
    fun `GIVEN the groups on screen WHEN connecting Apple in the browser THEN the trip leaves from elsewhere`() =
        runTest {
            // Given
            val account = FakeAccountRepository()
            val navigator = BackStackNavigator(startRoute = Route.Groups)
            val useCase = ConnectWithBrowserUseCase(account, navigator)

            // When
            useCase(AccountProvider.APPLE)

            // Then
            assertEquals(listOf(AccountProvider.APPLE), account.browserConnections)
            assertEquals(listOf(false), account.browserConnectionsFromProfile)
        }
}
