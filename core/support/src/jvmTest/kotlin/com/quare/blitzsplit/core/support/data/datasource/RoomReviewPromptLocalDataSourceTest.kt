package com.quare.blitzsplit.core.support.data.datasource

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.quare.blitzsplit.core.provider.room.db.AppDatabase
import com.quare.blitzsplit.core.provider.room.entity.ReviewPromptEntity
import com.quare.blitzsplit.core.provider.room.entity.SINGLE_ROW_ID
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Why: the data source over a real in-memory Room, since the single row and its read and write in one step are Room's.
class RoomReviewPromptLocalDataSourceTest {
    private lateinit var database: AppDatabase
    private lateinit var dataSource: RoomReviewPromptLocalDataSource

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `GIVEN a fresh install WHEN reading THEN there is nothing yet`() = runTest {
        // Given
        prepareScenario()

        // When
        val stored = dataSource.get()

        // Then
        assertNull(stored)
    }

    @Test
    fun `GIVEN a fresh install WHEN updating THEN the change starts from no bills and never asked`() = runTest {
        // Given
        prepareScenario()
        val seen = mutableListOf<ReviewPromptEntity>()

        // When
        dataSource.update { it.also(seen::add) }

        // Then
        assertEquals(listOf(entity()), seen)
    }

    @Test
    fun `GIVEN a fresh install WHEN updating every field THEN answers with what was saved and reads it back`() =
        runTest {
            // Given
            prepareScenario()
            val asked = entity(
                billsCreated = 3,
                firstEligibleAt = 100L,
                lastPromptedAt = 200L,
                lastPromptedVersion = "3.0.0",
            )

            // When
            val saved = dataSource.update { asked }

            // Then
            assertEquals(asked, saved)
            assertEquals(asked, dataSource.get())
        }

    @Test
    fun `GIVEN one bill counted WHEN counting another THEN both are counted`() = runTest {
        // Given
        prepareScenario()
        dataSource.update { it.copy(billsCreated = it.billsCreated + 1) }

        // When
        val saved = dataSource.update { it.copy(billsCreated = it.billsCreated + 1) }

        // Then
        assertEquals(entity(billsCreated = 2), saved)
        assertEquals(entity(billsCreated = 2), dataSource.get())
    }

    @Test
    fun `GIVEN the grace started WHEN remembering it asked THEN the bills and the grace stay`() = runTest {
        // Given
        prepareScenario()
        dataSource.update {
            it.copy(
                billsCreated = 3,
                firstEligibleAt = 100L,
            )
        }

        // When
        dataSource.update {
            it.copy(
                lastPromptedAt = 200L,
                lastPromptedVersion = "3.0.0",
            )
        }

        // Then
        assertEquals(
            entity(
                billsCreated = 3,
                firstEligibleAt = 100L,
                lastPromptedAt = 200L,
                lastPromptedVersion = "3.0.0",
            ),
            dataSource.get(),
        )
    }

    @Test
    fun `GIVEN it asked once WHEN the values are cleared THEN nothing of them is left`() = runTest {
        // Given
        prepareScenario()
        dataSource.update {
            it.copy(
                billsCreated = 3,
                firstEligibleAt = 100L,
                lastPromptedAt = 200L,
                lastPromptedVersion = "3.0.0",
            )
        }

        // When
        dataSource.update { entity() }

        // Then
        assertEquals(entity(), dataSource.get())
    }

    private fun entity(
        billsCreated: Int = 0,
        firstEligibleAt: Long? = null,
        lastPromptedAt: Long? = null,
        lastPromptedVersion: String? = null,
    ): ReviewPromptEntity = ReviewPromptEntity(
        id = SINGLE_ROW_ID,
        billsCreated = billsCreated,
        firstEligibleAt = firstEligibleAt,
        lastPromptedAt = lastPromptedAt,
        lastPromptedVersion = lastPromptedVersion,
    )

    private fun prepareScenario() {
        database = Room.inMemoryDatabaseBuilder<AppDatabase>().setDriver(BundledSQLiteDriver()).build()
        dataSource = RoomReviewPromptLocalDataSource(database.reviewPromptDao())
    }
}
