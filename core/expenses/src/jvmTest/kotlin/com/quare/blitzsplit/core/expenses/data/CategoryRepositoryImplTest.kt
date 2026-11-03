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
import kotlin.test.assertIs
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
    fun `GIVEN a category created offline WHEN observing the group THEN it is pending until a push`() = runTest {
        // Given
        prepareScenario()

        // When
        repository.createGroupCategory(
            category(
                id = "cat-1",
                name = "Pet",
            ),
            createdAt = 5L,
        )
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
        val result = repository.createGroupCategory(
            category(
                id = "cat-1",
                groupId = "gone",
            ),
            createdAt = 5L,
        )

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `GIVEN a main category with a subcategory WHEN deleting the main one THEN both become pending tombstones`() =
        runTest {
            // Given
            prepareScenario()
            repository.createGroupCategory(
                category(
                    id = "main",
                    name = "Praia",
                    icon = CategoryIcon.SUN,
                ),
                1L,
            )
            repository.createGroupCategory(
                category(
                    id = "sub",
                    name = "Quiosque",
                    parentId = "main",
                ),
                2L,
            )

            // When
            repository.deleteGroupCategory("main", deletedAt = 9L)

            // Then
            val stored = repository.observeGroupCategories(GROUP_ID).first()
            assertEquals(listOf(true, true), stored.map { it.isDeleted })
            assertTrue(repository.observePendingVersions().first().all { it >= 9L })
        }

    @Test
    fun `GIVEN a category edited here WHEN a newer server row comes THEN the servers wins an older one does not`() =
        runTest {
            // Given
            prepareScenario()
            repository.createGroupCategory(
                category(
                    id = "cat-1",
                    name = "Pet",
                ),
                createdAt = 5L,
            )
            repository.updateGroupCategory("cat-1", "Bichos", null, "paw", updatedAt = 7L)

            // When
            remote.rows = listOf(
                dto(
                    id = "cat-1",
                    name = "Velho",
                    updatedAt = 6L,
                ),
            )
            repository.download(USER_ID)
            val afterOlder = repository
                .observeGroupCategories(GROUP_ID)
                .first()
                .single()
                .name
            remote.rows = listOf(
                dto(
                    id = "cat-1",
                    name = "Novo",
                    updatedAt = 20L,
                ),
            )
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
        remote.rows = listOf(
            dto(
                id = "cat-1",
                name = "Pet",
                updatedAt = 3L,
            ),
        )
        repository.download(USER_ID)

        // When
        remote.rows = emptyList()
        repository.download(USER_ID)

        // Then
        assertTrue(repository.observeGroupCategories(GROUP_ID).first().isEmpty())
    }

    @Test
    fun `GIVEN categories of two groups on the server WHEN downloading one group THEN only its categories come down`() =
        runTest {
            // Given
            prepareScenario()
            remote.rows = listOf(
                dto(
                    id = "cat-1",
                    name = "Pet",
                    updatedAt = 3L,
                ),
                dto(
                    id = "cat-other",
                    name = "Quiosque",
                    updatedAt = 3L,
                    groupId = OTHER_GROUP_ID,
                ),
            )

            // When
            val result = repository.fetchGroupCategories(GROUP_ID)
            result.getOrNull()?.invoke()

            // Then
            assertTrue(result.isSuccess)
            val stored = repository.observeAllGroupCategories().first()
            assertEquals(listOf("cat-1" to false), stored.map { it.id to it.isPending })
        }

    @Test
    fun `GIVEN synced categories the server no longer has WHEN downloading one group THEN none leaves the device`() =
        runTest {
            // Given
            prepareScenario()
            remote.rows = listOf(
                dto(
                    id = "cat-gone",
                    name = "Pet",
                    updatedAt = 3L,
                ),
                dto(
                    id = "cat-other",
                    name = "Quiosque",
                    updatedAt = 3L,
                    groupId = OTHER_GROUP_ID,
                ),
            )
            repository.download(USER_ID)
            remote.rows = listOf(
                dto(
                    id = "cat-1",
                    name = "Mercado",
                    updatedAt = 4L,
                ),
            )

            // When
            repository.fetchGroupCategories(GROUP_ID).getOrThrow()()

            // Then
            assertEquals(
                setOf("cat-gone", "cat-other", "cat-1"),
                repository
                    .observeAllGroupCategories()
                    .first()
                    .map { it.id }
                    .toSet(),
            )
        }

    @Test
    fun `GIVEN a category edited after the server row WHEN downloading its group THEN keeps the edit pending`() =
        runTest {
            // Given
            prepareScenario()
            repository.createGroupCategory(
                category(
                    id = "cat-1",
                    name = "Pet",
                ),
                createdAt = 5L,
            )
            repository.updateGroupCategory("cat-1", "Bichos", null, "paw", updatedAt = 7L)
            remote.rows = listOf(
                dto(
                    id = "cat-1",
                    name = "Velho",
                    updatedAt = 6L,
                ),
            )

            // When
            repository.fetchGroupCategories(GROUP_ID).getOrThrow()()

            // Then
            val stored = repository.observeGroupCategories(GROUP_ID).first().single()
            assertEquals("Bichos" to true, stored.name to stored.isPending)
        }

    @Test
    fun `GIVEN a server row newer than an edit here WHEN downloading its group THEN the server row wins`() = runTest {
        // Given
        prepareScenario()
        repository.createGroupCategory(
            category(
                id = "cat-1",
                name = "Pet",
            ),
            createdAt = 5L,
        )
        repository.updateGroupCategory("cat-1", "Bichos", null, "paw", updatedAt = 7L)
        remote.rows = listOf(
            dto(
                id = "cat-1",
                name = "Novo",
                updatedAt = 20L,
            ),
        )

        // When
        repository.fetchGroupCategories(GROUP_ID).getOrThrow()()

        // Then
        val stored = repository.observeGroupCategories(GROUP_ID).first().single()
        assertEquals("Novo" to false, stored.name to stored.isPending)
    }

    @Test
    fun `GIVEN the server fails WHEN downloading one group THEN fails and keeps the categories on the device`() =
        runTest {
            // Given
            prepareScenario()
            remote.rows = listOf(
                dto(
                    id = "cat-1",
                    name = "Pet",
                    updatedAt = 3L,
                ),
            )
            repository.download(USER_ID)
            remote.rows = listOf(
                dto(
                    id = "cat-1",
                    name = "Bichos",
                    updatedAt = 9L,
                ),
            )
            remote.fetchError = IllegalStateException("server down")

            // When
            val result = repository.fetchGroupCategories(GROUP_ID)
            result.getOrNull()?.invoke()

            // Then
            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertEquals(listOf("Pet"), repository.observeGroupCategories(GROUP_ID).first().map { it.name })
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
        groupId: String = GROUP_ID,
    ): GroupCategoryDto = GroupCategoryDto(
        id = id,
        groupId = groupId,
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
        listOf(GROUP_ID, OTHER_GROUP_ID).forEach { id ->
            database.groupDao().upsertGroup(
                GroupEntity(
                    id = id,
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
        }
        remote = FakeGroupCategoryRemote()
        repository = CategoryRepositoryImpl(
            groupLocal = RoomGroupCategoryLocalDataSource(database.groupCategoryDao()),
            groupRemote = remote,
            groupMapper = GroupCategoryMapperImpl(),
        )
    }

    private companion object {
        const val USER_ID = "user-1"
        const val GROUP_ID = "group-1"
        const val OTHER_GROUP_ID = "group-2"
    }
}

private class FakeGroupCategoryRemote : GroupCategoryRemoteDataSource {
    val upserted = mutableListOf<GroupCategoryDto>()
    var rows = emptyList<GroupCategoryDto>()
    var fetchError: Throwable? = null

    override suspend fun upsertCategories(categories: List<GroupCategoryDto>) {
        upserted += categories
    }

    override suspend fun fetchCategories(): List<GroupCategoryDto> {
        fetchError?.let { throw it }
        return rows
    }

    override suspend fun fetchCategories(groupId: String): List<GroupCategoryDto> {
        fetchError?.let { throw it }
        return rows.filter { it.groupId == groupId }
    }
}
