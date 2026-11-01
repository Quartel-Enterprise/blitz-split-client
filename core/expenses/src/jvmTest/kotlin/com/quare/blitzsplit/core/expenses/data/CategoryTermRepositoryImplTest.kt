package com.quare.blitzsplit.core.expenses.data

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.quare.blitzsplit.core.expenses.data.datasource.CategoryTermRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.RoomCategoryTermLocalDataSource
import com.quare.blitzsplit.core.expenses.data.dto.CategoryTermDto
import com.quare.blitzsplit.core.expenses.data.mapper.CategoryTermMapperImpl
import com.quare.blitzsplit.core.expenses.data.repository.CategoryTermRepositoryImpl
import com.quare.blitzsplit.core.provider.room.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Why: the map's repository over a real in-memory Room: one row per word, Last-Write-Wins and the owner are SQL.
class CategoryTermRepositoryImplTest {
    private lateinit var database: AppDatabase
    private lateinit var remote: FakeCategoryTermRemote
    private lateinit var repository: CategoryTermRepositoryImpl

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `GIVEN a word saved twice WHEN observing THEN it is one change stamped past the first`() = runTest {
        // Given
        prepareScenario()

        // When
        repository.saveTerm(term = " Farmácia ", categoryId = "health.pharmacy", updatedAt = 10L)
        repository.saveTerm(term = "farmacia", categoryId = null, updatedAt = 5L)

        // Then
        val change = repository.observeChanges().first().single()
        assertEquals("farmacia", change.key)
        assertNull(change.categoryId)
        assertTrue(change.isPending)
        assertEquals(11L, database.categoryTermDao().getAll().single().updatedAt)
    }

    @Test
    fun `GIVEN changes to app and own terms WHEN restoring THEN only the app ones are restored`() = runTest {
        // Given
        prepareScenario()
        repository.saveTerm(term = "uber", categoryId = "leisure.parties", updatedAt = 1L)
        repository.saveTerm(term = "picanha", categoryId = "food", updatedAt = 1L)

        // When
        repository.restoreAppTerms(appKeys = setOf("uber"), updatedAt = 2L)

        // Then
        val byKey = repository.observeChanges().first().associateBy { it.key }
        assertTrue(byKey.getValue("uber").isRestored)
        assertFalse(byKey.getValue("picanha").isRestored)
    }

    @Test
    fun `GIVEN a pending change WHEN pushing THEN it goes up as the user and stops pending`() = runTest {
        // Given
        prepareScenario()
        repository.saveTerm(term = "pizzaria", categoryId = "food.restaurant", updatedAt = 3L)

        // When
        repository.uploadPending(USER_ID)

        // Then
        assertEquals(listOf("pizzaria" to USER_ID), remote.upserted.map { it.termKey to it.userId })
        assertFalse(repository.observeHasPendingChanges().first())
        assertEquals(USER_ID, database.categoryTermDao().getAll().single().ownerUserId)
    }

    @Test
    fun `GIVEN rows on the server WHEN pulling THEN a newer one wins and an older one does not`() = runTest {
        // Given
        prepareScenario()
        repository.saveTerm(term = "uber", categoryId = "leisure.parties", updatedAt = 10L)
        repository.saveTerm(term = "bar", categoryId = "leisure", updatedAt = 10L)
        remote.rows = listOf(dto("uber", "transport.ride", updatedAt = 20L), dto("bar", null, updatedAt = 5L), dto("voo", "travel.flight", updatedAt = 1L))

        // When
        repository.download(USER_ID)

        // Then
        val byKey = repository.observeChanges().first().associateBy { it.key }
        assertEquals("transport.ride", byKey.getValue("uber").categoryId)
        assertFalse(byKey.getValue("uber").isPending)
        assertEquals("leisure", byKey.getValue("bar").categoryId)
        assertEquals("travel.flight", byKey.getValue("voo").categoryId)
    }

    @Test
    fun `GIVEN another user's map WHEN pulling THEN it is dropped and clearing empties the map`() = runTest {
        // Given
        prepareScenario()
        repository.saveTerm(term = "uber", categoryId = "leisure.parties", updatedAt = 1L)
        repository.uploadPending("someone-else")

        // When
        repository.download(USER_ID)
        val afterSwitch = repository.observeChanges().first()
        repository.saveTerm(term = "voo", categoryId = "travel.flight", updatedAt = 2L)
        repository.clear()

        // Then
        assertTrue(afterSwitch.isEmpty())
        assertTrue(repository.observeChanges().first().isEmpty())
    }

    private fun dto(
        term: String,
        categoryId: String?,
        updatedAt: Long,
    ) = CategoryTermDto(
        userId = USER_ID,
        termKey = term,
        term = term,
        categoryId = categoryId,
        isDeleted = false,
        addedAt = 1L,
        updatedAt = updatedAt,
    )

    private fun prepareScenario() {
        database = Room.inMemoryDatabaseBuilder<AppDatabase>().setDriver(BundledSQLiteDriver()).build()
        remote = FakeCategoryTermRemote()
        repository = CategoryTermRepositoryImpl(
            local = RoomCategoryTermLocalDataSource(database.categoryTermDao()),
            remote = remote,
            mapper = CategoryTermMapperImpl(),
        )
    }

    private companion object {
        const val USER_ID = "user-1"
    }
}

private class FakeCategoryTermRemote : CategoryTermRemoteDataSource {
    val upserted = mutableListOf<CategoryTermDto>()
    var rows = emptyList<CategoryTermDto>()

    override suspend fun upsert(terms: List<CategoryTermDto>) {
        upserted += terms
    }

    override suspend fun fetch(userId: String): List<CategoryTermDto> = rows
}
