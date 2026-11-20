package com.quare.blitzsplit.core.watch.domain

import com.quare.blitzsplit.core.watch.domain.model.WatchSignOutNotice
import com.quare.blitzsplit.core.watch.domain.usecase.impl.DismissSignedOutOnPhoneUseCase
import com.quare.blitzsplit.core.watch.domain.usecase.impl.ObserveSignedOutOnPhoneUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SignedOutOnPhoneUseCasesTest {
    private val notice = WatchSignOutNotice()
    private val observeSignedOutOnPhone = ObserveSignedOutOnPhoneUseCase(notice)
    private val dismissSignedOutOnPhone = DismissSignedOutOnPhoneUseCase(notice)

    @Test
    fun `GIVEN a watch the phone never signed out WHEN observing the notice THEN it is hidden`() = runTest {
        // Given
        val signedOut = observeSignedOutOnPhone()

        // When
        val isShown = signedOut.first()

        // Then
        assertFalse(isShown)
    }

    @Test
    fun `GIVEN the phone signed out of the watch WHEN observing the notice THEN it is shown`() = runTest {
        // Given
        notice.show()

        // When
        val isShown = observeSignedOutOnPhone().first()

        // Then
        assertTrue(isShown)
    }

    @Test
    fun `GIVEN the notice is shown WHEN dismissing it THEN it is hidden`() = runTest {
        // Given
        notice.show()

        // When
        dismissSignedOutOnPhone()

        // Then
        assertFalse(observeSignedOutOnPhone().first())
    }
}
