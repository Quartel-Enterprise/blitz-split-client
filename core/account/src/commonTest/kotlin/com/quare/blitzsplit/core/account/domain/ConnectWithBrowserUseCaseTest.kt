package com.quare.blitzsplit.core.account.domain

import com.quare.blitzsplit.core.account.domain.usecase.impl.ConnectWithBrowserUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.model.AccountProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ConnectWithBrowserUseCaseTest {
    @Test
    fun `GIVEN the profile WHEN connecting Google in the browser THEN the trip leaves from the profile`() = runTest {
        // Given
        val account = FakeAccountRepository()
        val useCase = ConnectWithBrowserUseCase(account)

        // When
        useCase(AccountProvider.GOOGLE, isFromProfile = true)

        // Then
        assertEquals(listOf(AccountProvider.GOOGLE), account.browserConnections)
        assertEquals(listOf(true), account.browserConnectionsFromProfile)
    }

    @Test
    fun `GIVEN another screen WHEN connecting Apple in the browser THEN the trip leaves from elsewhere`() = runTest {
        // Given
        val account = FakeAccountRepository()
        val useCase = ConnectWithBrowserUseCase(account)

        // When
        useCase(AccountProvider.APPLE, isFromProfile = false)

        // Then
        assertEquals(listOf(AccountProvider.APPLE), account.browserConnections)
        assertEquals(listOf(false), account.browserConnectionsFromProfile)
    }
}
