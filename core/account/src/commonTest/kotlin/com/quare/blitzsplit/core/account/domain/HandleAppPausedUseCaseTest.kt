package com.quare.blitzsplit.core.account.domain

import com.quare.blitzsplit.core.account.domain.usecase.impl.HandleAppPausedUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class HandleAppPausedUseCaseTest {
    @Test
    fun `GIVEN the app in the foreground WHEN it leaves to the background THEN tells the account`() {
        // Given
        val account = FakeAccountRepository()
        val useCase = HandleAppPausedUseCase(account)

        // When
        useCase()

        // Then
        assertEquals(1, account.appPauseCount)
        assertEquals(0, account.appResumeCount)
    }
}
