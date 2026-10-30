package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.quare.blitzsplit.core.provider.room.db.AppDatabase
import com.quare.blitzsplit.core.provider.room.entity.GroupEntity
import com.quare.blitzsplit.core.provider.room.entity.GroupMemberEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

// Why: the invite code and the member removal follow rules in the SQL itself, so it runs against SQLite rather than a fake.
class GroupDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: GroupDao

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `GIVEN a pushed group WHEN the server row has the same updatedAt THEN takes its invite code`() = runTest {
        // Given
        prepareScenario(stored = entity(updatedAt = 5L))

        // When
        dao.applyRemoteGroup(
            entity(
                updatedAt = 5L,
                inviteCode = "7Hk2pQxa",
            ),
        )

        // Then
        assertEquals(
            "7Hk2pQxa",
            dao
                .observeGroups()
                .first()
                .single()
                .inviteCode,
        )
    }

    @Test
    fun `GIVEN a newer edit here WHEN an older server row comes THEN keeps the edit and takes the code`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                name = "Floripa 2027",
                updatedAt = 9L,
                pendingSync = true,
            ),
        )

        // When
        dao.applyRemoteGroup(
            entity(
                name = "Viagem Floripa",
                updatedAt = 5L,
                inviteCode = "7Hk2pQxa",
            ),
        )

        // Then
        assertEquals(
            entity(
                name = "Floripa 2027",
                updatedAt = 9L,
                pendingSync = true,
                inviteCode = "7Hk2pQxa",
            ),
            dao
                .observeGroups()
                .first()
                .single(),
        )
    }

    @Test
    fun `GIVEN joined members and this device user WHEN removing them THEN only the one not joined goes`() = runTest {
        // Given
        prepareScenario(
            stored = entity(updatedAt = 1L),
            members = listOf(
                member(
                    id = "me",
                    isMe = true,
                ),
                member(
                    id = "ana",
                    userId = "user-2",
                ),
                member(id = "bruno"),
            ),
        )

        // When
        listOf("me", "ana", "bruno").forEach {
            dao.removeMember(
                id = it,
                removedAt = 9L,
            )
        }

        // Then
        assertEquals(
            listOf("bruno"),
            dao
                .observeMembers()
                .first()
                .filter { it.isRemoved }
                .map { it.id },
        )
    }

    @Test
    fun `GIVEN a removal not up yet WHEN a newer server row comes THEN the member is back and synced`() = runTest {
        // Given
        prepareScenario(
            stored = entity(updatedAt = 1L),
            members = listOf(member(id = "bruno")),
        )
        dao.removeMember(
            id = "bruno",
            removedAt = 5L,
        )

        // When
        dao.applyRemoteMember(
            member(
                id = "bruno",
                name = "Bruno M",
                updatedAt = 8L,
            ),
        )

        // Then
        assertEquals(
            member(
                id = "bruno",
                name = "Bruno M",
                updatedAt = 8L,
            ),
            dao
                .observeMembers()
                .first()
                .single(),
        )
    }

    @Test
    fun `GIVEN a removed member brought back WHEN deleting the removed ones THEN it stays`() = runTest {
        // Given
        prepareScenario(
            stored = entity(updatedAt = 1L),
            members = listOf(
                member(id = "bruno"),
                member(id = "carla"),
            ),
        )
        listOf("bruno", "carla").forEach {
            dao.removeMember(
                id = it,
                removedAt = 5L,
            )
        }
        dao.applyRemoteMember(
            member(
                id = "carla",
                updatedAt = 8L,
            ),
        )

        // When
        dao.deleteRemovedMembers(listOf("bruno", "carla"))

        // Then
        assertEquals(
            listOf("carla"),
            dao
                .observeMembers()
                .first()
                .map { it.id },
        )
    }

    @Test
    fun `GIVEN a removed member WHEN renaming it THEN keeps the name it had`() = runTest {
        // Given
        prepareScenario(
            stored = entity(updatedAt = 1L),
            members = listOf(member(id = "bruno")),
        )
        dao.removeMember(
            id = "bruno",
            removedAt = 5L,
        )

        // When
        dao.renameMember(
            id = "bruno",
            name = "Bruno M",
            updatedAt = 6L,
        )

        // Then
        assertEquals(
            "Bruno",
            dao
                .observeMembers()
                .first()
                .single()
                .name,
        )
    }

    @Test
    fun `GIVEN a group no longer on the device WHEN adding a member to it THEN fails and leaves nothing pending`() =
        runTest {
            // Given
            prepareScenario(stored = entity(updatedAt = 1L))

            // When
            val result = runCatching {
                dao.insertMemberIntoGroup(
                    member(id = "ana").copy(
                        groupId = "group-gone",
                        pendingSync = true,
                    ),
                )
            }

            // Then
            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertTrue(dao.observeMembers().first().isEmpty())
            assertFalse(dao.observeHasPending().first())
        }

    @Test
    fun `GIVEN a member only the server has WHEN its row comes THEN stores it with its invite code`() = runTest {
        // Given
        prepareScenario(stored = entity(updatedAt = 1L))

        // When
        dao.applyRemoteMember(
            member(
                id = "ana",
                inviteCode = "Xy7kQ2",
            ),
        )

        // Then
        assertEquals(
            "Xy7kQ2",
            dao
                .observeMembers()
                .first()
                .single()
                .inviteCode,
        )
    }

    @Test
    fun `GIVEN a pushed member WHEN the server row has the same updatedAt THEN takes its invite code`() = runTest {
        // Given
        prepareScenario(
            stored = entity(updatedAt = 1L),
            members = listOf(
                member(
                    id = "ana",
                    updatedAt = 5L,
                ),
            ),
        )

        // When
        dao.applyRemoteMember(
            member(
                id = "ana",
                updatedAt = 5L,
                inviteCode = "Xy7kQ2",
            ),
        )

        // Then
        assertEquals(
            "Xy7kQ2",
            dao
                .observeMembers()
                .first()
                .single()
                .inviteCode,
        )
    }

    @Test
    fun `GIVEN a member renamed here WHEN an older server row comes THEN keeps the name and takes the code`() =
        runTest {
            // Given
            prepareScenario(
                stored = entity(updatedAt = 1L),
                members = listOf(
                    member(
                        id = "ana",
                        name = "Ana Paula",
                        updatedAt = 9L,
                        pendingSync = true,
                    ),
                ),
            )

            // When
            dao.applyRemoteMember(
                member(
                    id = "ana",
                    name = "Ana",
                    updatedAt = 5L,
                    inviteCode = "Xy7kQ2",
                ),
            )

            // Then
            assertEquals(
                member(
                    id = "ana",
                    name = "Ana Paula",
                    updatedAt = 9L,
                    pendingSync = true,
                    inviteCode = "Xy7kQ2",
                ),
                dao
                    .observeMembers()
                    .first()
                    .single(),
            )
        }

    @Test
    fun `GIVEN a member with its code WHEN a server row without a code comes THEN keeps the code`() = runTest {
        // Given
        prepareScenario(
            stored = entity(updatedAt = 1L),
            members = listOf(
                member(
                    id = "ana",
                    updatedAt = 5L,
                    inviteCode = "Xy7kQ2",
                ),
            ),
        )

        // When
        dao.applyRemoteMember(
            member(
                id = "ana",
                name = "Ana Paula",
                updatedAt = 9L,
            ),
        )

        // Then
        assertEquals(
            member(
                id = "ana",
                name = "Ana Paula",
                updatedAt = 9L,
                inviteCode = "Xy7kQ2",
            ),
            dao
                .observeMembers()
                .first()
                .single(),
        )
    }

    @Test
    fun `GIVEN a member without a code WHEN setting its invite code THEN only that member has it`() = runTest {
        // Given
        prepareScenario(
            stored = entity(updatedAt = 1L),
            members = listOf(
                member(id = "ana"),
                member(id = "bruno"),
            ),
        )

        // When
        dao.updateMemberInviteCode(
            id = "ana",
            inviteCode = "Xy7kQ2",
        )

        // Then
        assertEquals(
            listOf("Xy7kQ2", null),
            dao
                .observeMembers()
                .first()
                .map { it.inviteCode },
        )
    }

    @Test
    fun `GIVEN members without codes WHEN setting the codes an upload brought THEN each member has its own`() =
        runTest {
            // Given
            prepareScenario(
                stored = entity(updatedAt = 1L),
                members = listOf(
                    member(id = "ana"),
                    member(id = "bruno"),
                    member(id = "carla"),
                ),
            )

            // When
            dao.updateMemberInviteCodes(
                mapOf(
                    "ana" to "Xy7kQ2",
                    "carla" to "c3Rt9z",
                ),
            )

            // Then
            assertEquals(
                listOf("Xy7kQ2", null, "c3Rt9z"),
                dao
                    .observeMembers()
                    .first()
                    .map { it.inviteCode },
            )
        }

    @Test
    fun `GIVEN a member with an invite code WHEN renaming it THEN keeps the code`() = runTest {
        // Given
        prepareScenario(
            stored = entity(updatedAt = 1L),
            members = listOf(
                member(
                    id = "ana",
                    inviteCode = "Xy7kQ2",
                ),
            ),
        )

        // When
        dao.renameMember(
            id = "ana",
            name = "Ana Paula",
            updatedAt = 6L,
        )

        // Then
        val stored = dao
            .observeMembers()
            .first()
            .single()
        assertEquals("Ana Paula", stored.name)
        assertEquals("Xy7kQ2", stored.inviteCode)
    }

    @Test
    fun `GIVEN a synced group with its code WHEN editing it THEN changes its fields and keeps who made it`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                updatedAt = 1L,
                inviteCode = "7Hk2pQxa",
            ),
        )

        // When
        dao.updateGroup(
            id = "group-1",
            name = "Casa da praia",
            type = "home",
            color = "peach",
            currency = "USD",
            updatedAt = 6L,
        )

        // Then
        assertEquals(
            entity(
                updatedAt = 6L,
                pendingSync = true,
                inviteCode = "7Hk2pQxa",
            ).copy(
                name = "Casa da praia",
                type = "home",
                color = "peach",
                currency = "USD",
            ),
            dao
                .observeGroups()
                .first()
                .single(),
        )
    }

    @Test
    fun `GIVEN a group stamped by a clock ahead WHEN editing it THEN the edit is stamped after it`() = runTest {
        // Given
        prepareScenario(stored = entity(updatedAt = 9L))

        // When
        dao.updateGroup(
            id = "group-1",
            name = "Casa da praia",
            type = "trip",
            color = "blue",
            currency = "BRL",
            updatedAt = 6L,
        )

        // Then
        assertEquals(
            10L,
            dao
                .observeGroups()
                .first()
                .single()
                .updatedAt,
        )
    }

    private fun entity(
        name: String = "Viagem Floripa",
        updatedAt: Long,
        pendingSync: Boolean = false,
        inviteCode: String? = null,
    ): GroupEntity = GroupEntity(
        id = "group-1",
        name = name,
        type = "trip",
        color = "blue",
        currency = "BRL",
        createdBy = "user-1",
        addedAt = 1L,
        updatedAt = updatedAt,
        pendingSync = pendingSync,
        ownerUserId = "user-1",
        inviteCode = inviteCode,
    )

    private fun member(
        id: String,
        name: String = id.replaceFirstChar { it.uppercase() },
        userId: String? = null,
        isMe: Boolean = false,
        updatedAt: Long = 1L,
        pendingSync: Boolean = false,
        inviteCode: String? = null,
    ): GroupMemberEntity = GroupMemberEntity(
        id = id,
        groupId = "group-1",
        name = name,
        avatarColor = "green",
        userId = userId,
        isMe = isMe,
        addedAt = 1L,
        updatedAt = updatedAt,
        pendingSync = pendingSync,
        isRemoved = false,
        inviteCode = inviteCode,
    )

    private suspend fun prepareScenario(
        stored: GroupEntity,
        members: List<GroupMemberEntity> = emptyList(),
    ) {
        database = Room.inMemoryDatabaseBuilder<AppDatabase>().setDriver(BundledSQLiteDriver()).build()
        dao = database.groupDao()
        dao.upsertGroup(stored)
        dao.upsertMembers(members)
    }
}
