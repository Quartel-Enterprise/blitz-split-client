package com.quare.blitzsplit.core.contacts.domain

import com.quare.blitzsplit.core.contacts.domain.model.HiddenContact
import com.quare.blitzsplit.core.contacts.domain.usecase.impl.SetContactHiddenUseCase
import com.quare.blitzsplit.core.contacts.testing.FakeHiddenContactRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SetContactHiddenUseCaseTest {
    private lateinit var repository: FakeHiddenContactRepository
    private lateinit var useCase: SetContactHiddenUseCase

    @Test
    fun `GIVEN Carla in the contacts WHEN hiding her THEN keeps her hidden on the device from now`() = runTest {
        // Given
        prepareScenario(now = 500L)

        // When
        val result = useCase("user:carla", isHidden = true)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            listOf(
                HiddenContact(
                    "user:carla",
                    isHidden = true,
                    hiddenAt = 500L,
                ),
            ),
            repository.hidden.value,
        )
        assertTrue(repository.hasPendingChanges.value)
    }

    @Test
    fun `GIVEN Carla hidden WHEN showing her THEN she is shown again`() = runTest {
        // Given
        prepareScenario(
            now = 900L,
            stored = listOf(
                HiddenContact(
                    "user:carla",
                    isHidden = true,
                    hiddenAt = 500L,
                ),
            ),
        )

        // When
        val result = useCase("user:carla", isHidden = false)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            listOf(
                HiddenContact(
                    "user:carla",
                    isHidden = false,
                    hiddenAt = 500L,
                ),
            ),
            repository.hidden.value,
        )
    }

    @Test
    fun `GIVEN the device fails to store it WHEN hiding Carla THEN returns that failure`() = runTest {
        // Given
        prepareScenario(setHiddenError = IllegalStateException("disk full"))

        // When
        val result = useCase("user:carla", isHidden = true)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(repository.hidden.value.isEmpty())
    }

    private fun prepareScenario(
        now: Long = 0L,
        stored: List<HiddenContact> = emptyList(),
        setHiddenError: Throwable? = null,
    ) {
        repository = FakeHiddenContactRepository(
            now = { now },
            initialHidden = stored,
        ).also {
            it.setHiddenError = setHiddenError
        }
        useCase = SetContactHiddenUseCase(repository)
    }
}
