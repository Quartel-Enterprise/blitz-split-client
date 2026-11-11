package com.quare.blitzsplit.core.profile.domain

import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveProfileShareLinkUseCase
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObserveProfileShareLinkUseCaseTest {
    private lateinit var repository: FakeProfileRepository
    private lateinit var useCase: ObserveProfileShareLinkUseCase

    @Test
    fun `GIVEN the account has a share code WHEN observing the link THEN gives the full profile link`() = runTest {
        // Given
        prepareScenario(shareCode = "7Hk2pQ8xZa")

        // When
        val link = useCase().first()

        // Then
        assertEquals("https://blitzsplit.com/u/7Hk2pQ8xZa", link)
    }

    @Test
    fun `GIVEN no account owns the profile WHEN observing the link THEN gives none`() = runTest {
        // Given
        prepareScenario(shareCode = null)

        // When
        val link = useCase().first()

        // Then
        assertNull(link)
    }

    private fun prepareScenario(shareCode: String?) {
        repository = FakeProfileRepository().apply { this.shareCode.value = shareCode }
        useCase = ObserveProfileShareLinkUseCase(repository)
    }
}
