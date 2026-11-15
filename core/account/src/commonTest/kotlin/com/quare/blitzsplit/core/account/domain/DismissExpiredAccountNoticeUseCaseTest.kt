package com.quare.blitzsplit.core.account.domain

import com.quare.blitzsplit.core.account.domain.usecase.impl.DismissExpiredAccountNoticeUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.model.InactivityExpiry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNull

class DismissExpiredAccountNoticeUseCaseTest {
    @Test
    fun `GIVEN the notice of an expired account WHEN it is dismissed THEN the account no longer has it`() = runTest {
        // Given
        val account = FakeAccountRepository()
        account.snapshot.update { it.copy(inactivityExpiry = InactivityExpiry.NOTICE) }
        val useCase = DismissExpiredAccountNoticeUseCase(account)

        // When
        useCase()

        // Then
        assertNull(account.observeAccount().first().inactivityExpiry)
    }
}
