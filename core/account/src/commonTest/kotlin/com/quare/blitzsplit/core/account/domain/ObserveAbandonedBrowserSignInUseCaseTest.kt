package com.quare.blitzsplit.core.account.domain

import com.quare.blitzsplit.core.account.domain.model.AbandonedBrowserSignIn
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAbandonedBrowserSignInUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveAbandonedBrowserSignInUseCaseTest {
    @Test
    fun `GIVEN a screen observing WHEN the account reports an abandoned browser sign in THEN the screen receives it`() =
        runTest {
            // Given
            val account = FakeAccountRepository()
            val useCase = ObserveAbandonedBrowserSignInUseCase(account)
            val received = mutableListOf<AbandonedBrowserSignIn>()
            backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { useCase().collect(received::add) }

            // When
            account.abandonedBrowserSignIns.emit(AbandonedBrowserSignIn.FAILED)
            runCurrent()

            // Then
            assertEquals(listOf(AbandonedBrowserSignIn.FAILED), received)
        }
}
