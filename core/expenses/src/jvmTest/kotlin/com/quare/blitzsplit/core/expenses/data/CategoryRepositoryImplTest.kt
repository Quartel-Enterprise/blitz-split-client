package com.quare.blitzsplit.core.expenses.data

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.quare.blitzsplit.core.expenses.data.datasource.GroupCategoryRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.RoomGroupCategoryLocalDataSource
import com.quare.blitzsplit.core.expenses.data.dto.GroupCategoryDto
import com.quare.blitzsplit.core.expenses.data.mapper.GroupCategoryMapperImpl
import com.quare.blitzsplit.core.expenses.data.repository.CategoryRepositoryImpl
import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import com.quare.blitzsplit.core.provider.room.db.AppDatabase
import com.quare.blitzsplit.core.provider.room.entity.GroupEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Why: the repository over a real in-memory Room, since the tombstones, the group check and Last-Write-Wins are SQL.
class CategoryRepositoryImplTest {
    private lateinit var database: AppDatabase
    private lateinit var remote: FakeGroupCategoryRemote
    private lateinit var repository: CategoryRepositoryImpl

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `GIVEN a category created offline WHEN observing the group THEN it is there pending until a push takes it up`() =
        runTest {
            // Given
            prepareScenario()

            // When
            repository.createGroupCategory(category(id = "cat-1", name = "Pet"), createdAt = 5L)
            val pending = repository.observeGroupCategories(GROUP_ID).first()
            repository.uploadPending(USER_ID)

            // Then
            assertEquals(listOf("Pet" to true), pending.map { it.name to it.isPending })
            assertEquals(listOf("cat-1" to USER_ID), remote.upserted.map { it.id to it.createdBy })
            assertFalse(repository.observeHasPendingChanges().first())
        }

    @Test
    fun `GIVEN a group that is gone WHEN creating a category in it THEN fails`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = repository.createGroupCategory(category(id = "cat-1", groupId = "gone"), createdAt = 5L)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `GIVEN a main category with a subcategory WHEN deleting the main one THEN both become pending tombstones`() =
        runTest {
            // Given
            prepareScenario()
            repository.createGroupCategory(category(id = "main", name = "Praia", icon = CategoryIcon.SUN), 1L)
            repository.createGroupCategory(category(id = "sub", name = "Quiosque", parentId = "main"), 2L)

            // When
            repository.deleteGroupCategory("main", deletedAt = 9L)

            // Then
            val stored = repository.observeGroupCategories(GROUP_ID).first()
            assertEquals(listOf(true, true), stored.map { it.isDeleted })
            assertTrue(repository.observePendingVersions().first().all { it >= 9L })
        }

    @Test
    fun `GIVEN a category edited here WHEN a newer server row comes THEN the server's wins, an older one doesn't`() =
        runTest {
            // Given
            prepareScenario()
            repository.createGroupCategory(category(id = "cat-1", name = "Pet"), createdAt = 5L)
            repository.updateGroupCategory("cat-1", "Bichos", null, "paw", updatedAt = 7L)

            // When
            remote.rows = listOf(dto(id = "cat-1", name = "Velho", updatedAt = 6L))
            repository.download(USER_ID)
            val afterOlder = repository.observeGroupCategories(GROUP_ID).first().single().name
            remote.rows = listOf(dto(id = "cat-1", name = "Novo", updatedAt = 20L))
            repository.download(USER_ID)

            // Then
            assertEquals("Bichos", afterOlder)
            val stored = repository.observeGroupCategories(GROUP_ID).first().single()
            assertEquals("Novo" to false, stored.name to stored.isPending)
        }

    @Test
    fun `GIVEN a synced category the server no longer has WHEN downloading THEN it leaves the device`() = runTest {
        // Given
        prepareScenario()
        remote.rows = listOf(dto(id = "cat-1", name = "Pet", updatedAt = 3L))
        repository.download(USER_ID)

        // When
        remote.rows = emptyList()
        repository.download(USER_ID)

        // Then
        assertTrue(repository.observeGroupCategories(GROUP_ID).first().isEmpty())
    }

    private fun category(
        id: String,
        name: String = "Pet",
        groupId: String = GROUP_ID,
        parentId: String? = null,
        icon: CategoryIcon? = CategoryIcon.PAW,
    ): GroupCategory = GroupCategory(
        id = id,
        groupId = groupId,
        parentId = parentId,
        name = name,
        icon = icon,
        createdByMemberId = "member-1",
        isDeleted = false,
        isPending = true,
    )

    private fun dto(
        id: String,
        name: String,
        updatedAt: Long,
    ): GroupCategoryDto = GroupCategoryDto(
        id = id,
        groupId = GROUP_ID,
        createdBy = "user-2",
        createdByMemberId = "member-2",
        parentId = null,
        name = name,
        icon = "paw",
        isDeleted = false,
        addedAt = 1L,
        updatedAt = updatedAt,
    )

    private suspend fun prepareScenario() {
        database = Room.inMemoryDatabaseBuilder<AppDatabase>().setDriver(BundledSQLiteDriver()).build()
        database.groupDao().upsertGroup(
            GroupEntity(
                id = GROUP_ID,
                name = "Viagem Floripa",
                type = "trip",
                color = "blue",
                currency = "BRL",
                createdBy = USER_ID,
                addedAt = 1L,
                updatedAt = 1L,
                pendingSync = false,
                ownerUserId = USER_ID,
                inviteCode = null,
                defaultSplit = null,
                splitIncomes = null,
            ),
        )
        remote = FakeGroupCategoryRemote()
        repository = CategoryRepositoryImpl(
            groupLocal = RoomGroupCategoryLocalDataSource(database.groupCategoryDao()),
            groupRemote = remote,
            groupMapper = GroupCategoryMapperImpl(),
        )
    }

    private class FakeGroupCategoryRemote : GroupCategoryRemoteDataSource {
        val upserted = mutableListOf<GroupCategoryDto>()
        var rows = emptyList<GroupCategoryDto>()

        override suspend fun upsertCategories(categories: List<GroupCategoryDto>) {
            upserted += categories
        }

        override suspend fun fetchCategories(): List<GroupCategoryDto> = rows
    }

    private companion object {
        const val USER_ID = "user-1"
        const val GROUP_ID = "group-1"
    }
}
