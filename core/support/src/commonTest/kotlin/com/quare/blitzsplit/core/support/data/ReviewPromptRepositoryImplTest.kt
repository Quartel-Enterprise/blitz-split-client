package com.quare.blitzsplit.core.support.data

import com.quare.blitzsplit.core.provider.room.entity.ReviewPromptEntity
import com.quare.blitzsplit.core.provider.room.entity.SINGLE_ROW_ID
import com.quare.blitzsplit.core.support.data.datasource.ReviewPromptLocalDataSource
import com.quare.blitzsplit.core.support.data.repository.ReviewPromptRepositoryImpl
import com.quare.blitzsplit.core.support.domain.model.ReviewPromptState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReviewPromptRepositoryImplTest {
    private lateinit var local: FakeReviewPromptLocalDataSource
    private lateinit var repository: ReviewPromptRepositoryImpl

    @Test
    fun `GIVEN nothing saved yet WHEN reading THEN there are no bills and it never asked`() = runTest {
        // Given
        prepareScenario(stored = null)

        // When
        val state = repository.get()

        // Then
        assertEquals(
            ReviewPromptState(
                billsCreated = 0,
                firstEligibleAt = null,
                lastPromptedAt = null,
                lastPromptedVersion = null,
            ),
            state,
        )
    }

    @Test
    fun `GIVEN nothing saved yet WHEN reading THEN nothing is saved by it`() = runTest {
        // Given
        prepareScenario(stored = null)

        // When
        repository.get()

        // Then
        assertNull(local.stored)
    }

    @Test
    fun `GIVEN what the device remembers WHEN reading THEN every field comes back`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                billsCreated = 7,
                firstEligibleAt = 100L,
                lastPromptedAt = 200L,
                lastPromptedVersion = "3.0.0",
            ),
        )

        // When
        val state = repository.get()

        // Then
        assertEquals(
            ReviewPromptState(
                billsCreated = 7,
                firstEligibleAt = 100L,
                lastPromptedAt = 200L,
                lastPromptedVersion = "3.0.0",
            ),
            state,
        )
    }

    @Test
    fun `GIVEN nothing saved yet WHEN updating THEN the change starts from no bills and never asked`() = runTest {
        // Given
        prepareScenario(stored = null)
        val seen = mutableListOf<ReviewPromptState>()

        // When
        repository.update { it.also(seen::add) }

        // Then
        assertEquals(
            listOf(
                ReviewPromptState(
                    billsCreated = 0,
                    firstEligibleAt = null,
                    lastPromptedAt = null,
                    lastPromptedVersion = null,
                ),
            ),
            seen,
        )
    }

    @Test
    fun `GIVEN nothing saved yet WHEN updating every field THEN saves them in the single row and answers with them`() =
        runTest {
            // Given
            prepareScenario(stored = null)
            val asked = ReviewPromptState(
                billsCreated = 3,
                firstEligibleAt = 100L,
                lastPromptedAt = 200L,
                lastPromptedVersion = "3.0.0",
            )

            // When
            val saved = repository.update { asked }

            // Then
            assertEquals(asked, saved)
            assertEquals(
                entity(
                    billsCreated = 3,
                    firstEligibleAt = 100L,
                    lastPromptedAt = 200L,
                    lastPromptedVersion = "3.0.0",
                ),
                local.stored,
            )
        }

    @Test
    fun `GIVEN what the device remembers WHEN updating one field THEN the others stay`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                billsCreated = 7,
                firstEligibleAt = 100L,
                lastPromptedAt = 200L,
                lastPromptedVersion = "3.0.0",
            ),
        )

        // When
        val saved = repository.update { it.copy(billsCreated = it.billsCreated + 1) }

        // Then
        assertEquals(
            ReviewPromptState(
                billsCreated = 8,
                firstEligibleAt = 100L,
                lastPromptedAt = 200L,
                lastPromptedVersion = "3.0.0",
            ),
            saved,
        )
        assertEquals(saved, repository.get())
    }

    @Test
    fun `GIVEN one bill counted WHEN counting another THEN both are counted`() = runTest {
        // Given
        prepareScenario(stored = null)
        repository.update { it.copy(billsCreated = it.billsCreated + 1) }

        // When
        val saved = repository.update { it.copy(billsCreated = it.billsCreated + 1) }

        // Then
        assertEquals(2, saved.billsCreated)
        assertEquals(2, repository.get().billsCreated)
    }

    private fun entity(
        billsCreated: Int,
        firstEligibleAt: Long?,
        lastPromptedAt: Long?,
        lastPromptedVersion: String?,
    ): ReviewPromptEntity = ReviewPromptEntity(
        id = SINGLE_ROW_ID,
        billsCreated = billsCreated,
        firstEligibleAt = firstEligibleAt,
        lastPromptedAt = lastPromptedAt,
        lastPromptedVersion = lastPromptedVersion,
    )

    private fun prepareScenario(stored: ReviewPromptEntity?) {
        local = FakeReviewPromptLocalDataSource(stored)
        repository = ReviewPromptRepositoryImpl(local)
    }
}

private class FakeReviewPromptLocalDataSource(
    var stored: ReviewPromptEntity?,
) : ReviewPromptLocalDataSource {
    private val empty = ReviewPromptEntity(
        id = SINGLE_ROW_ID,
        billsCreated = 0,
        firstEligibleAt = null,
        lastPromptedAt = null,
        lastPromptedVersion = null,
    )

    override suspend fun get(): ReviewPromptEntity? = stored

    override suspend fun update(transform: (ReviewPromptEntity) -> ReviewPromptEntity): ReviewPromptEntity =
        transform(stored ?: empty).also { stored = it }
}
