package com.quare.blitzsplit.core.account.domain

import com.quare.blitzsplit.core.account.domain.usecase.impl.HandleAppResumedUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class HandleAppResumedUseCaseTest {
    @Test
    fun `GIVEN the app in the background WHEN it comes back to the foreground THEN tells the account`() {
        // Given
        val account = FakeAccountRepository()
        val useCase = HandleAppResumedUseCase(account)

        // When
        useCase()

        // Then
        assertEquals(1, account.appResumeCount)
    }
}
