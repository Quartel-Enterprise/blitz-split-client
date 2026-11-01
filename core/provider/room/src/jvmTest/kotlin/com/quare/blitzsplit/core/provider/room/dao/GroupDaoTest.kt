package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.quare.blitzsplit.core.provider.room.db.AppDatabase
import com.quare.blitzsplit.core.provider.room.entity.ExpenseEntity
import com.quare.blitzsplit.core.provider.room.entity.GroupEntity
import com.quare.blitzsplit.core.provider.room.entity.GroupMemberEntity
import com.quare.blitzsplit.core.provider.room.entity.SettlementEntity
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
    fun `GIVEN a joined member renamed here WHEN an older row released it THEN drops only the account`() = runTest {
        // Given
        prepareScenario(
            stored = entity(updatedAt = 1L),
            members = listOf(
                member(
                    id = "carla",
                    name = "Carla S.",
                    userId = "user-2",
                    updatedAt = 9L,
                    pendingSync = true,
                    inviteCode = "Ab3dE5",
                ),
            ),
        )

        // When
        dao.applyRemoteMember(
            member(
                id = "carla",
                userId = null,
                updatedAt = 5L,
                inviteCode = "Zz9yX8",
            ),
        )

        // Then
        assertEquals(
            member(
                id = "carla",
                name = "Carla S.",
                userId = null,
                updatedAt = 9L,
                pendingSync = true,
                inviteCode = "Zz9yX8",
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

    @Test
    fun `GIVEN a group edited here that went up WHEN the server no longer gives it THEN it goes with its members`() =
        runTest {
            // Given
            prepareScenario(
                stored = entity(
                    updatedAt = 9L,
                    pendingSync = true,
                    inviteCode = "7Hk2pQxa",
                ),
                members = listOf(
                    member(
                        id = "me",
                        isMe = true,
                        userId = "user-1",
                    ),
                    member(
                        id = "ana",
                        name = "Ana Paula",
                        updatedAt = 9L,
                        pendingSync = true,
                    ),
                ),
            )

            // When
            dao.deleteGoneFromServer(
                ownerUserId = "user-1",
                serverGroupIds = emptySet(),
                serverMemberIds = emptySet(),
            )

            // Then
            assertTrue(dao.observeGroups().first().isEmpty())
            assertTrue(dao.observeMembers().first().isEmpty())
            assertFalse(dao.observeHasPending().first())
        }

    @Test
    fun `GIVEN a released group with pending bills and payments WHEN the server no longer gives it THEN they go too`() =
        runTest {
            // Given
            prepareScenario(
                stored = entity(
                    updatedAt = 9L,
                    pendingSync = true,
                    inviteCode = "7Hk2pQxa",
                ),
                expenses = listOf(
                    expense(id = "bill-synced"),
                    expense(
                        id = "bill-pending",
                        pendingSync = true,
                    ),
                ),
                settlements = listOf(
                    settlement(id = "payment-synced"),
                    settlement(
                        id = "payment-pending",
                        pendingSync = true,
                    ),
                ),
            )

            // When
            dao.deleteGoneFromServer(
                ownerUserId = "user-1",
                serverGroupIds = emptySet(),
                serverMemberIds = emptySet(),
            )

            // Then
            assertTrue(
                database
                    .expenseDao()
                    .observeExpenses("group-1")
                    .first()
                    .isEmpty(),
            )
            assertTrue(
                database
                    .settlementDao()
                    .observeSettlements("group-1")
                    .first()
                    .isEmpty(),
            )
            assertFalse(database.expenseDao().observeHasPending().first())
            assertFalse(database.settlementDao().observeHasPending().first())
        }

    @Test
    fun `GIVEN a group created here and not up yet WHEN the server does not give it THEN it stays with its members`() =
        runTest {
            // Given
            prepareScenario(
                stored = entity(
                    updatedAt = 9L,
                    pendingSync = true,
                ),
                members = listOf(
                    member(
                        id = "me",
                        isMe = true,
                        pendingSync = true,
                    ),
                    member(
                        id = "ana",
                        pendingSync = true,
                    ),
                ),
            )

            // When
            dao.deleteGoneFromServer(
                ownerUserId = "user-1",
                serverGroupIds = emptySet(),
                serverMemberIds = emptySet(),
            )

            // Then
            assertEquals(listOf("group-1"), dao.observeGroups().first().map { it.id })
            assertEquals(listOf("ana", "me"), dao.observeMembers().first().map { it.id })
        }

    @Test
    fun `GIVEN a synced group WHEN the server no longer gives it THEN it goes with its members`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                updatedAt = 1L,
                inviteCode = "7Hk2pQxa",
            ),
            members = listOf(member(id = "ana")),
        )

        // When
        dao.deleteGoneFromServer(
            ownerUserId = "user-1",
            serverGroupIds = emptySet(),
            serverMemberIds = emptySet(),
        )

        // Then
        assertTrue(dao.observeGroups().first().isEmpty())
        assertTrue(dao.observeMembers().first().isEmpty())
    }

    @Test
    fun `GIVEN a synced group and one edited here WHEN the server still gives both THEN both stay`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                updatedAt = 1L,
                inviteCode = "7Hk2pQxa",
            ),
            otherGroups = listOf(
                entity(
                    id = "group-2",
                    updatedAt = 9L,
                    pendingSync = true,
                    inviteCode = "Qp4mZt8w",
                ),
            ),
            members = listOf(
                member(id = "ana"),
                member(
                    id = "bruno",
                    groupId = "group-2",
                ),
            ),
        )

        // When
        dao.deleteGoneFromServer(
            ownerUserId = "user-1",
            serverGroupIds = setOf("group-1", "group-2"),
            serverMemberIds = setOf("ana", "bruno"),
        )

        // Then
        assertEquals(listOf("group-2", "group-1"), dao.observeGroups().first().map { it.id })
        assertEquals(listOf("ana", "bruno"), dao.observeMembers().first().map { it.id })
    }

    @Test
    fun `GIVEN synced and pending members WHEN the server group has neither THEN only the pending one stays`() =
        runTest {
            // Given
            prepareScenario(
                stored = entity(
                    updatedAt = 1L,
                    inviteCode = "7Hk2pQxa",
                ),
                members = listOf(
                    member(id = "ana"),
                    member(
                        id = "bruno",
                        pendingSync = true,
                    ),
                    member(id = "carla"),
                ),
            )

            // When
            dao.deleteGoneFromServer(
                ownerUserId = "user-1",
                serverGroupIds = setOf("group-1"),
                serverMemberIds = setOf("carla"),
            )

            // Then
            assertEquals(listOf("group-1"), dao.observeGroups().first().map { it.id })
            assertEquals(listOf("bruno", "carla"), dao.observeMembers().first().map { it.id })
        }

    @Test
    fun `GIVEN another users synced group WHEN the server groups of this user lack it THEN it stays`() = runTest {
        // Given
        prepareScenario(
            stored = entity(
                updatedAt = 1L,
                inviteCode = "7Hk2pQxa",
                ownerUserId = "user-2",
            ),
            members = listOf(member(id = "ana")),
        )

        // When
        dao.deleteGoneFromServer(
            ownerUserId = "user-1",
            serverGroupIds = emptySet(),
            serverMemberIds = emptySet(),
        )

        // Then
        assertEquals(listOf("group-1"), dao.observeGroups().first().map { it.id })
        assertEquals(listOf("ana"), dao.observeMembers().first().map { it.id })
    }

    @Test
    fun `GIVEN a group WHEN saving its default split THEN only the split and the incomes change`() = runTest {
        // Given
        prepareScenario(stored = entity(updatedAt = 5L))

        // When
        dao.updateDefaultSplit(
            id = "group-1",
            defaultSplit = SHARES_JSON,
            splitIncomes = INCOMES_JSON,
            updatedAt = 3L,
        )

        // Then
        val group = dao.observeGroups().first().single()
        assertEquals(SHARES_JSON, group.defaultSplit)
        assertEquals(INCOMES_JSON, dao.getSplitIncomes("group-1"))
        assertEquals("Viagem Floripa", group.name)
        assertEquals(6L, group.updatedAt)
        assertTrue(group.pendingSync)
    }

    @Test
    fun `GIVEN incomes typed here WHEN a newer server row comes THEN takes its split and keeps the incomes`() =
        runTest {
            // Given
            prepareScenario(stored = entity(updatedAt = 5L).copy(splitIncomes = INCOMES_JSON))

            // When
            dao.applyRemoteGroup(entity(updatedAt = 9L).copy(defaultSplit = SHARES_JSON))

            // Then
            val group = dao.observeGroups().first().single()
            assertEquals(SHARES_JSON, group.defaultSplit)
            assertEquals(INCOMES_JSON, group.splitIncomes)
        }

    private fun entity(
        id: String = "group-1",
        name: String = "Viagem Floripa",
        updatedAt: Long,
        pendingSync: Boolean = false,
        inviteCode: String? = null,
        ownerUserId: String = "user-1",
    ): GroupEntity = GroupEntity(
        id = id,
        name = name,
        type = "trip",
        color = "blue",
        currency = "BRL",
        createdBy = "user-1",
        addedAt = 1L,
        updatedAt = updatedAt,
        pendingSync = pendingSync,
        ownerUserId = ownerUserId,
        inviteCode = inviteCode,
        defaultSplit = null,
        splitIncomes = null,
    )

    private fun member(
        id: String,
        groupId: String = "group-1",
        name: String = id.replaceFirstChar { it.uppercase() },
        userId: String? = null,
        isMe: Boolean = false,
        updatedAt: Long = 1L,
        pendingSync: Boolean = false,
        inviteCode: String? = null,
    ): GroupMemberEntity = GroupMemberEntity(
        id = id,
        groupId = groupId,
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

    private fun expense(
        id: String,
        pendingSync: Boolean = false,
    ): ExpenseEntity = ExpenseEntity(
        id = id,
        groupId = "group-1",
        createdBy = "user-1",
        description = "Mercado",
        amountMinor = 1_000L,
        splitMode = "equal",
        payersJson = "[]",
        sharesJson = "[]",
        spentAt = 1_000L,
        isDeleted = false,
        addedAt = 1L,
        updatedAt = 1L,
        updatedBy = null,
        pendingSync = pendingSync,
    )

    private fun settlement(
        id: String,
        pendingSync: Boolean = false,
    ): SettlementEntity = SettlementEntity(
        id = id,
        groupId = "group-1",
        createdBy = "user-1",
        fromMemberId = "ana",
        toMemberId = "me",
        amountMinor = 500L,
        method = "pix",
        paidAt = 1_000L,
        isDeleted = false,
        addedAt = 1L,
        updatedAt = 1L,
        pendingSync = pendingSync,
    )

    private suspend fun prepareScenario(
        stored: GroupEntity,
        otherGroups: List<GroupEntity> = emptyList(),
        members: List<GroupMemberEntity> = emptyList(),
        expenses: List<ExpenseEntity> = emptyList(),
        settlements: List<SettlementEntity> = emptyList(),
    ) {
        database = Room.inMemoryDatabaseBuilder<AppDatabase>().setDriver(BundledSQLiteDriver()).build()
        dao = database.groupDao()
        dao.upsertGroup(stored)
        otherGroups.forEach { dao.upsertGroup(it) }
        dao.upsertMembers(members)
        expenses.forEach { database.expenseDao().insertExpenseIntoGroup(it) }
        database.settlementDao().insertSettlementsIntoGroups(settlements)
    }

    private companion object {
        const val SHARES_JSON = """{"mode":"shares","weights":[{"member_id":"ana","weight":3}]}"""
        const val INCOMES_JSON = """{"ana":900000}"""
    }
}
