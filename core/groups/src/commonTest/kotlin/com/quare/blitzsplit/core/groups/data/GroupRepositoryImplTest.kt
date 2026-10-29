package com.quare.blitzsplit.core.groups.data

import com.quare.blitzsplit.core.groups.data.datasource.GroupLocalDataSource
import com.quare.blitzsplit.core.groups.data.datasource.GroupRemoteDataSource
import com.quare.blitzsplit.core.groups.data.dto.GroupDto
import com.quare.blitzsplit.core.groups.data.dto.GroupMemberDto
import com.quare.blitzsplit.core.groups.data.mapper.GroupMapperImpl
import com.quare.blitzsplit.core.groups.data.repository.GroupRepositoryImpl
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.profile.data.datasource.ProfileLocalDataSource
import com.quare.blitzsplit.core.provider.room.entity.GroupEntity
import com.quare.blitzsplit.core.provider.room.entity.GroupMemberEntity
import com.quare.blitzsplit.core.provider.room.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupRepositoryImplTest {
    private lateinit var local: FakeGroupLocalDataSource
    private lateinit var remote: FakeGroupRemoteDataSource
    private lateinit var repository: GroupRepositoryImpl

    @Test
    fun `GIVEN two groups with members WHEN observing the groups THEN each has its own in the stored order`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(group(id = "group-1"), group(id = "group-2")),
                storedMembers = listOf(
                    member(
                        id = "m-2",
                        groupId = "group-1",
                        name = "Ana",
                    ),
                    member(
                        id = "m-3",
                        groupId = "group-2",
                        name = "Bruno",
                    ),
                    member(
                        id = "m-1",
                        groupId = "group-1",
                        name = "Pierre",
                    ),
                ),
            )

            // When
            val groups = repository.observeGroups().first()

            // Then
            assertEquals(listOf("group-1", "group-2"), groups.map { it.id })
            assertEquals(listOf("Ana", "Pierre"), groups[0].members.map { it.name })
            assertEquals(listOf("Bruno"), groups[1].members.map { it.name })
        }

    @Test
    fun `GIVEN a member added to a stored group WHEN observing the groups THEN the group shows it`() = runTest {
        // Given
        prepareScenario(storedGroups = listOf(group(id = "group-1")))
        local.members.value = listOf(
            member(
                id = "m-1",
                groupId = "group-1",
                name = "Ana",
            ),
        )

        // When
        val groups = repository.observeGroups().first()

        // Then
        assertEquals(listOf("Ana"), groups.single().members.map { it.name })
    }

    @Test
    fun `GIVEN a pending group WHEN observing pending changes THEN reports them`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    pendingSync = true,
                ),
            ),
        )

        // When
        val hasPendingChanges = repository.observeHasPendingChanges().first()

        // Then
        assertTrue(hasPendingChanges)
    }

    @Test
    fun `GIVEN only a pending member WHEN observing pending changes THEN reports them`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            storedMembers = listOf(
                member(
                    id = "m-1",
                    groupId = "group-1",
                    pendingSync = true,
                ),
            ),
        )

        // When
        val hasPendingChanges = repository.observeHasPendingChanges().first()

        // Then
        assertTrue(hasPendingChanges)
    }

    @Test
    fun `GIVEN only synced groups and members WHEN observing pending changes THEN reports none`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            storedMembers = listOf(
                member(
                    id = "m-1",
                    groupId = "group-1",
                ),
            ),
        )

        // When
        val hasPendingChanges = repository.observeHasPendingChanges().first()

        // Then
        assertFalse(hasPendingChanges)
    }

    @Test
    fun `GIVEN a new group WHEN creating it THEN stores the group pending and unowned`() = runTest {
        // Given
        prepareScenario()

        // When
        repository.createGroup(newGroup, createdAt = 1_000L)

        // Then
        val stored = local.group("group-new")
        assertEquals("Viagem Floripa", stored?.name)
        assertEquals("trip", stored?.type)
        assertEquals("peach", stored?.color)
        assertEquals("BRL", stored?.currency)
        assertEquals(1_000L, stored?.updatedAt)
        assertEquals(true, stored?.pendingSync)
        assertNull(stored?.ownerUserId)
        assertNull(stored?.createdBy)
    }

    @Test
    fun `GIVEN a new group WHEN creating it THEN stores its members pending with no account in the same write`() =
        runTest {
            // Given
            prepareScenario()

            // When
            repository.createGroup(newGroup, createdAt = 1_000L)

            // Then
            val write = local.groupWrites.single()
            assertEquals("group-new", write.first.id)
            assertEquals(listOf("new-1", "new-2"), write.second.map { it.id })
            assertTrue(write.second.all { it.groupId == "group-new" && it.pendingSync && it.userId == null })
            assertEquals(listOf(true, false), write.second.map { it.isMe })
        }

    @Test
    fun `GIVEN a group just created WHEN observing THEN shows it pending with its members in order`() = runTest {
        // Given
        prepareScenario()
        repository.createGroup(newGroup, createdAt = 1_000L)

        // When
        val groups = repository.observeGroups().first()

        // Then
        val created = groups.single()
        assertTrue(created.isPending)
        assertEquals(listOf("Pierre", "Ana"), created.members.map { it.name })
        assertEquals(listOf(true, false), created.members.map { it.hasJoined })
    }

    @Test
    fun `GIVEN an owned pending group WHEN uploading THEN pushes it with the user as creator and marks it synced`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        createdBy = null,
                        pendingSync = true,
                        updatedAt = 5L,
                    ),
                ),
            )

            // When
            val result = repository.uploadPendingGroups("user-1")

            // Then
            assertTrue(result.isSuccess)
            val pushed = remote.upsertedGroups.single().single()
            assertEquals("group-1", pushed.id)
            assertEquals("user-1", pushed.createdBy)
            assertEquals(5L, pushed.updatedAt)
            assertEquals(false, local.group("group-1")?.pendingSync)
        }

    @Test
    fun `GIVEN a pending group nobody owns yet WHEN uploading THEN leaves it on the device pending`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    ownerUserId = null,
                    pendingSync = true,
                ),
            ),
        )

        // When
        val result = repository.uploadPendingGroups("user-1")

        // Then
        assertTrue(result.isSuccess)
        assertTrue(remote.upsertedGroups.isEmpty())
        assertEquals(true, local.group("group-1")?.pendingSync)
    }

    @Test
    fun `GIVEN a pending group of another user WHEN uploading THEN does not push it`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    ownerUserId = "user-2",
                    pendingSync = true,
                ),
            ),
        )

        // When
        repository.uploadPendingGroups("user-1")

        // Then
        assertTrue(remote.upsertedGroups.isEmpty())
        assertEquals(true, local.group("group-1")?.pendingSync)
    }

    @Test
    fun `GIVEN only synced groups and members WHEN uploading THEN pushes nothing`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            storedMembers = listOf(
                member(
                    id = "m-1",
                    groupId = "group-1",
                ),
            ),
        )

        // When
        val result = repository.uploadPendingGroups("user-1")

        // Then
        assertTrue(result.isSuccess)
        assertTrue(remote.calls.isEmpty())
    }

    @Test
    fun `GIVEN a pending group with pending members WHEN uploading THEN pushes the group before its members`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        pendingSync = true,
                    ),
                ),
                storedMembers = listOf(
                    member(
                        id = "m-1",
                        groupId = "group-1",
                        pendingSync = true,
                    ),
                    member(
                        id = "m-2",
                        groupId = "group-1",
                        pendingSync = true,
                    ),
                ),
            )

            // When
            repository.uploadPendingGroups("user-1")

            // Then
            assertEquals(listOf("groups", "members"), remote.calls)
            assertEquals(listOf("m-1", "m-2"), remote.upsertedMembers.single().map { it.id })
        }

    @Test
    fun `GIVEN pending members of an owned group WHEN uploading THEN marks them synced`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            storedMembers = listOf(
                member(
                    id = "m-1",
                    groupId = "group-1",
                    pendingSync = true,
                ),
            ),
        )

        // When
        val result = repository.uploadPendingGroups("user-1")

        // Then
        assertTrue(result.isSuccess)
        assertTrue(remote.upsertedGroups.isEmpty())
        assertEquals(listOf("m-1"), remote.upsertedMembers.single().map { it.id })
        assertEquals(false, local.member("m-1")?.pendingSync)
    }

    @Test
    fun `GIVEN pending members of a group nobody owns yet WHEN uploading THEN does not push them`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    ownerUserId = null,
                    pendingSync = true,
                ),
            ),
            storedMembers = listOf(
                member(
                    id = "m-1",
                    groupId = "group-1",
                    pendingSync = true,
                ),
            ),
        )

        // When
        repository.uploadPendingGroups("user-1")

        // Then
        assertTrue(remote.upsertedMembers.isEmpty())
        assertEquals(true, local.member("m-1")?.pendingSync)
    }

    @Test
    fun `GIVEN pending members of another user group WHEN uploading THEN does not push them`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    ownerUserId = "user-2",
                ),
            ),
            storedMembers = listOf(
                member(
                    id = "m-1",
                    groupId = "group-1",
                    pendingSync = true,
                ),
            ),
        )

        // When
        repository.uploadPendingGroups("user-1")

        // Then
        assertTrue(remote.upsertedMembers.isEmpty())
        assertEquals(true, local.member("m-1")?.pendingSync)
    }

    @Test
    fun `GIVEN the server fails the groups WHEN uploading THEN returns the error and leaves everything pending`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        pendingSync = true,
                    ),
                ),
                storedMembers = listOf(
                    member(
                        id = "m-1",
                        groupId = "group-1",
                        pendingSync = true,
                    ),
                ),
                upsertGroupsError = IllegalStateException("server down"),
            )

            // When
            val result = repository.uploadPendingGroups("user-1")

            // Then
            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertTrue(remote.upsertedMembers.isEmpty())
            assertEquals(true, local.group("group-1")?.pendingSync)
            assertEquals(true, local.member("m-1")?.pendingSync)
        }

    @Test
    fun `GIVEN the server fails the members WHEN uploading THEN returns the error and leaves the members pending`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(group(id = "group-1")),
                storedMembers = listOf(
                    member(
                        id = "m-1",
                        groupId = "group-1",
                        pendingSync = true,
                    ),
                ),
                upsertMembersError = IllegalStateException("server down"),
            )

            // When
            val result = repository.uploadPendingGroups("user-1")

            // Then
            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertEquals(true, local.member("m-1")?.pendingSync)
        }

    @Test
    fun `GIVEN a group uploaded once WHEN uploading again THEN pushes nothing`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    pendingSync = true,
                ),
            ),
            storedMembers = listOf(
                member(
                    id = "m-1",
                    groupId = "group-1",
                    pendingSync = true,
                ),
            ),
        )
        repository.uploadPendingGroups("user-1")
        remote.calls.clear()

        // When
        val result = repository.uploadPendingGroups("user-1")

        // Then
        assertTrue(result.isSuccess)
        assertTrue(remote.calls.isEmpty())
    }

    @Test
    fun `GIVEN the group changes while its push is in flight WHEN uploading THEN keeps the change pending`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    pendingSync = true,
                    updatedAt = 5L,
                ),
            ),
        )
        remote.onUpsertGroups = {
            local.put(
                local.group("group-1")!!.copy(
                    name = "Viagem Floripa 2026",
                    updatedAt = 6L,
                ),
            )
        }

        // When
        repository.uploadPendingGroups("user-1")

        // Then
        assertEquals(true, local.group("group-1")?.pendingSync)
    }

    @Test
    fun `GIVEN groups and members on the server WHEN downloading THEN stores them synced and owned by the user`() =
        runTest {
            // Given
            prepareScenario(
                remoteGroups = listOf(
                    groupDto(
                        id = "group-1",
                        createdBy = "user-2",
                    ),
                ),
                remoteMembers = listOf(
                    memberDto(
                        id = "m-1",
                        groupId = "group-1",
                        userId = "user-2",
                        name = "Ana",
                    ),
                    memberDto(
                        id = "m-2",
                        groupId = "group-1",
                        userId = "user-1",
                        name = "Pierre",
                    ),
                    memberDto(
                        id = "m-3",
                        groupId = "group-1",
                        userId = null,
                        name = "Bruno",
                    ),
                ),
            )

            // When
            val result = repository.downloadGroups("user-1")

            // Then
            assertTrue(result.isSuccess)
            val stored = local.group("group-1")
            assertEquals("user-1", stored?.ownerUserId)
            assertEquals("user-2", stored?.createdBy)
            assertEquals(false, stored?.pendingSync)
            assertEquals(listOf(false, true, false), listOf("m-1", "m-2", "m-3").map { local.member(it)?.isMe })
            assertFalse(repository.observeHasPendingChanges().first())
        }

    @Test
    fun `GIVEN members on the server WHEN downloading THEN only those with an account have joined`() = runTest {
        // Given
        prepareScenario(
            remoteGroups = listOf(groupDto(id = "group-1")),
            remoteMembers = listOf(
                memberDto(
                    id = "m-1",
                    groupId = "group-1",
                    userId = "user-2",
                    name = "Ana",
                ),
                memberDto(
                    id = "m-2",
                    groupId = "group-1",
                    userId = null,
                    name = "Bruno",
                ),
            ),
        )

        // When
        repository.downloadGroups("user-1")

        // Then
        val members = repository
            .observeGroups()
            .first()
            .single()
            .members
        assertEquals(listOf(true, false), members.map { it.hasJoined })
        assertEquals(listOf(false, false), members.map { it.isMe })
    }

    @Test
    fun `GIVEN the server fails WHEN downloading THEN returns the error and keeps the device groups`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    ownerUserId = "user-2",
                ),
            ),
            fetchError = IllegalStateException("server down"),
        )

        // When
        val result = repository.downloadGroups("user-1")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals("user-2", local.group("group-1")?.ownerUserId)
    }

    @Test
    fun `GIVEN groups of another user on the device WHEN downloading THEN deletes them and their members`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-old",
                    ownerUserId = "user-2",
                    pendingSync = true,
                ),
                group(
                    id = "group-mine",
                    ownerUserId = "user-1",
                ),
            ),
            storedMembers = listOf(
                member(
                    id = "m-old",
                    groupId = "group-old",
                    pendingSync = true,
                ),
                member(
                    id = "m-mine",
                    groupId = "group-mine",
                ),
            ),
            remoteGroups = listOf(groupDto(id = "group-mine")),
            remoteMembers = listOf(
                memberDto(
                    id = "m-mine",
                    groupId = "group-mine",
                ),
            ),
        )

        // When
        repository.downloadGroups("user-1")

        // Then
        assertEquals(listOf("group-mine"), local.groups.value.map { it.id })
        assertEquals(listOf("m-mine"), local.members.value.map { it.id })
    }

    @Test
    fun `GIVEN a group created before any account WHEN downloading THEN the user claims it`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    ownerUserId = null,
                    createdBy = null,
                    pendingSync = true,
                ),
            ),
            storedMembers = listOf(
                member(
                    id = "m-me",
                    groupId = "group-1",
                    isMe = true,
                    userId = null,
                    pendingSync = true,
                ),
                member(
                    id = "m-ana",
                    groupId = "group-1",
                    isMe = false,
                    userId = null,
                    pendingSync = true,
                ),
            ),
        )

        // When
        repository.downloadGroups("user-1")

        // Then
        val stored = local.group("group-1")
        assertEquals("user-1", stored?.ownerUserId)
        assertEquals("user-1", stored?.createdBy)
        assertEquals(true, stored?.pendingSync)
        assertEquals("user-1", local.member("m-me")?.userId)
        assertNull(local.member("m-ana")?.userId)
    }

    @Test
    fun `GIVEN a group created offline and claimed on download WHEN uploading THEN pushes it with its members`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        ownerUserId = null,
                        createdBy = null,
                        pendingSync = true,
                    ),
                ),
                storedMembers = listOf(
                    member(
                        id = "m-me",
                        groupId = "group-1",
                        isMe = true,
                        userId = null,
                        pendingSync = true,
                    ),
                ),
            )
            repository.downloadGroups("user-1")

            // When
            repository.uploadPendingGroups("user-1")

            // Then
            assertEquals(
                "user-1",
                remote.upsertedGroups
                    .single()
                    .single()
                    .createdBy,
            )
            assertEquals(
                "user-1",
                remote.upsertedMembers
                    .single()
                    .single()
                    .userId,
            )
            assertFalse(repository.observeHasPendingChanges().first())
        }

    @Test
    fun `GIVEN a newer server copy of a stored group WHEN downloading THEN the server copy wins`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    name = "Viagem",
                    updatedAt = 5L,
                    pendingSync = true,
                ),
            ),
            remoteGroups = listOf(
                groupDto(
                    id = "group-1",
                    name = "Viagem Floripa",
                    updatedAt = 9L,
                ),
            ),
        )

        // When
        repository.downloadGroups("user-1")

        // Then
        val stored = local.group("group-1")
        assertEquals("Viagem Floripa", stored?.name)
        assertEquals(9L, stored?.updatedAt)
        assertEquals(false, stored?.pendingSync)
    }

    @Test
    fun `GIVEN an older server copy of a stored group WHEN downloading THEN keeps the device change pending`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        name = "Viagem Floripa",
                        updatedAt = 9L,
                        pendingSync = true,
                    ),
                ),
                remoteGroups = listOf(
                    groupDto(
                        id = "group-1",
                        name = "Viagem",
                        updatedAt = 5L,
                    ),
                ),
            )

            // When
            repository.downloadGroups("user-1")

            // Then
            val stored = local.group("group-1")
            assertEquals("Viagem Floripa", stored?.name)
            assertEquals(9L, stored?.updatedAt)
            assertEquals(true, stored?.pendingSync)
        }

    @Test
    fun `GIVEN a group created offline that the server has newer WHEN downloading THEN the server copy wins`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        name = "Viagem",
                        ownerUserId = null,
                        updatedAt = 5L,
                        pendingSync = true,
                    ),
                ),
                remoteGroups = listOf(
                    groupDto(
                        id = "group-1",
                        name = "Viagem Floripa",
                        updatedAt = 9L,
                    ),
                ),
            )

            // When
            repository.downloadGroups("user-1")

            // Then
            assertEquals("Viagem Floripa", local.group("group-1")?.name)
            assertEquals("user-1", local.group("group-1")?.ownerUserId)
        }

    @Test
    fun `GIVEN a newer server copy of a stored member WHEN downloading THEN the server copy wins`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            storedMembers = listOf(
                member(
                    id = "m-1",
                    groupId = "group-1",
                    name = "Ana",
                    updatedAt = 5L,
                ),
            ),
            remoteGroups = listOf(groupDto(id = "group-1")),
            remoteMembers = listOf(
                memberDto(
                    id = "m-1",
                    groupId = "group-1",
                    userId = "user-2",
                    name = "Ana Paula",
                    updatedAt = 9L,
                ),
            ),
        )

        // When
        repository.downloadGroups("user-1")

        // Then
        assertEquals("Ana Paula", local.member("m-1")?.name)
        assertEquals("user-2", local.member("m-1")?.userId)
    }

    @Test
    fun `GIVEN an older server copy of a stored member WHEN downloading THEN keeps the device change`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            storedMembers = listOf(
                member(
                    id = "m-1",
                    groupId = "group-1",
                    name = "Ana Paula",
                    updatedAt = 9L,
                    pendingSync = true,
                ),
            ),
            remoteGroups = listOf(groupDto(id = "group-1")),
            remoteMembers = listOf(
                memberDto(
                    id = "m-1",
                    groupId = "group-1",
                    name = "Ana",
                    updatedAt = 5L,
                ),
            ),
        )

        // When
        repository.downloadGroups("user-1")

        // Then
        assertEquals("Ana Paula", local.member("m-1")?.name)
        assertEquals(true, local.member("m-1")?.pendingSync)
    }

    @Test
    fun `GIVEN a group of the user only on the device WHEN downloading THEN keeps it`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    pendingSync = true,
                ),
            ),
            remoteGroups = listOf(groupDto(id = "group-2")),
        )

        // When
        repository.downloadGroups("user-1")

        // Then
        assertEquals(
            setOf("group-1", "group-2"),
            local.groups.value
                .map { it.id }
                .toSet(),
        )
    }

    @Test
    fun `GIVEN stored groups and members WHEN clearing THEN the device has none`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    pendingSync = true,
                ),
            ),
            storedMembers = listOf(
                member(
                    id = "m-1",
                    groupId = "group-1",
                    pendingSync = true,
                ),
            ),
        )

        // When
        repository.clear()

        // Then
        assertTrue(repository.observeGroups().first().isEmpty())
        assertTrue(local.members.value.isEmpty())
        assertFalse(repository.observeHasPendingChanges().first())
    }

    @Test
    fun `GIVEN a profile of an account WHEN creating a group THEN that account owns and created it`() = runTest {
        // Given
        prepareScenario(profileOwnerUserId = "user-1")

        // When
        val result = repository.createGroup(newGroup, createdAt = 1_000L)

        // Then
        assertTrue(result.isSuccess)
        val stored = local.group("group-new")
        assertEquals("user-1", stored?.ownerUserId)
        assertEquals("user-1", stored?.createdBy)
        assertEquals(true, stored?.pendingSync)
    }

    @Test
    fun `GIVEN a profile of an account WHEN creating a group THEN only the creator member carries that account`() =
        runTest {
            // Given
            prepareScenario(profileOwnerUserId = "user-1")

            // When
            repository.createGroup(newGroup, createdAt = 1_000L)

            // Then
            assertEquals("user-1", local.member("new-1")?.userId)
            assertNull(local.member("new-2")?.userId)
            assertTrue(listOf("new-1", "new-2").all { local.member(it)?.pendingSync == true })
        }

    @Test
    fun `GIVEN a profile of no account WHEN creating a group THEN succeeds with the group owned by nobody`() = runTest {
        // Given
        prepareScenario(profileOwnerUserId = null)

        // When
        val result = repository.createGroup(newGroup, createdAt = 1_000L)

        // Then
        assertTrue(result.isSuccess)
        assertNull(local.group("group-new")?.ownerUserId)
        assertNull(local.member("new-1")?.userId)
    }

    @Test
    fun `GIVEN the device fails to write WHEN creating a group THEN returns the error instead of throwing`() = runTest {
        // Given
        prepareScenario(
            profileOwnerUserId = "user-1",
            insertError = IllegalStateException("disk full"),
        )

        // When
        val result = repository.createGroup(newGroup, createdAt = 1_000L)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(local.groups.value.isEmpty())
        assertTrue(local.members.value.isEmpty())
    }

    @Test
    fun `GIVEN a group created under an account WHEN that account uploads THEN pushes it as its own without a claim`() =
        runTest {
            // Given
            prepareScenario(profileOwnerUserId = "user-1")
            repository.createGroup(newGroup, createdAt = 1_000L)

            // When
            val result = repository.uploadPendingGroups("user-1")

            // Then
            assertTrue(result.isSuccess)
            val pushedGroup = remote.upsertedGroups.single().single()
            assertEquals("group-new", pushedGroup.id)
            assertEquals("user-1", pushedGroup.createdBy)
            assertEquals(
                listOf("user-1", null),
                remote.upsertedMembers.single().map { it.userId },
            )
            assertFalse(repository.observeHasPendingChanges().first())
        }

    @Test
    fun `GIVEN a group created under one account WHEN another account downloads THEN the device drops it`() = runTest {
        // Given
        prepareScenario(profileOwnerUserId = "user-1")
        repository.createGroup(newGroup, createdAt = 1_000L)

        // When
        val result = repository.downloadGroups("user-2")

        // Then
        assertTrue(result.isSuccess)
        assertNull(local.group("group-new"))
        assertTrue(local.members.value.isEmpty())
    }

    @Test
    fun `GIVEN a group created and not pushed yet WHEN its account downloads THEN keeps it pending`() = runTest {
        // Given
        prepareScenario(profileOwnerUserId = "user-1")
        repository.createGroup(newGroup, createdAt = 1_000L)

        // When
        repository.downloadGroups("user-1")

        // Then
        assertEquals(true, local.group("group-new")?.pendingSync)
        assertEquals(listOf("new-1", "new-2"), local.members.value.map { it.id })
    }

    @Test
    fun `GIVEN a synced group gone from the server WHEN downloading THEN it leaves with all its members`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(id = "group-gone"),
                group(id = "group-kept"),
            ),
            storedMembers = listOf(
                member(
                    id = "m-gone-synced",
                    groupId = "group-gone",
                ),
                member(
                    id = "m-gone-pending",
                    groupId = "group-gone",
                    pendingSync = true,
                ),
                member(
                    id = "m-kept",
                    groupId = "group-kept",
                ),
            ),
            remoteGroups = listOf(groupDto(id = "group-kept")),
            remoteMembers = listOf(
                memberDto(
                    id = "m-kept",
                    groupId = "group-kept",
                ),
            ),
        )

        // When
        val result = repository.downloadGroups("user-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("group-kept"), local.groups.value.map { it.id })
        assertEquals(listOf("m-kept"), local.members.value.map { it.id })
    }

    @Test
    fun `GIVEN a synced member the server no longer has WHEN downloading THEN it leaves and the others stay`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(group(id = "group-1")),
                storedMembers = listOf(
                    member(
                        id = "m-kept",
                        groupId = "group-1",
                    ),
                    member(
                        id = "m-removed",
                        groupId = "group-1",
                    ),
                ),
                remoteGroups = listOf(groupDto(id = "group-1")),
                remoteMembers = listOf(
                    memberDto(
                        id = "m-kept",
                        groupId = "group-1",
                    ),
                ),
            )

            // When
            repository.downloadGroups("user-1")

            // Then
            assertEquals(listOf("group-1"), local.groups.value.map { it.id })
            assertEquals(listOf("m-kept"), local.members.value.map { it.id })
        }

    @Test
    fun `GIVEN a member added on the device and not pushed yet WHEN downloading THEN keeps it pending`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            storedMembers = listOf(
                member(
                    id = "m-synced",
                    groupId = "group-1",
                ),
                member(
                    id = "m-new",
                    groupId = "group-1",
                    pendingSync = true,
                ),
            ),
            remoteGroups = listOf(groupDto(id = "group-1")),
            remoteMembers = listOf(
                memberDto(
                    id = "m-synced",
                    groupId = "group-1",
                ),
            ),
        )

        // When
        repository.downloadGroups("user-1")

        // Then
        assertEquals(listOf("m-synced", "m-new"), local.members.value.map { it.id })
        assertEquals(true, local.member("m-new")?.pendingSync)
    }

    @Test
    fun `GIVEN a pending group with pending members the server does not have WHEN downloading THEN keeps them all`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        pendingSync = true,
                    ),
                ),
                storedMembers = listOf(
                    member(
                        id = "m-1",
                        groupId = "group-1",
                        pendingSync = true,
                    ),
                    member(
                        id = "m-2",
                        groupId = "group-1",
                        pendingSync = true,
                    ),
                ),
            )

            // When
            repository.downloadGroups("user-1")

            // Then
            assertEquals(listOf("group-1"), local.groups.value.map { it.id })
            assertEquals(listOf("m-1", "m-2"), local.members.value.map { it.id })
        }

    @Test
    fun `GIVEN a group edited here that the server no longer has WHEN downloading THEN keeps its synced members`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        pendingSync = true,
                    ),
                ),
                storedMembers = listOf(
                    member(
                        id = "m-1",
                        groupId = "group-1",
                    ),
                ),
            )

            // When
            repository.downloadGroups("user-1")

            // Then
            assertEquals(listOf("group-1"), local.groups.value.map { it.id })
            assertEquals(listOf("m-1"), local.members.value.map { it.id })
        }

    @Test
    fun `GIVEN the server fails WHEN downloading THEN deletes none of the synced groups and members`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            storedMembers = listOf(
                member(
                    id = "m-1",
                    groupId = "group-1",
                ),
            ),
            fetchError = IllegalStateException("server down"),
        )

        // When
        val result = repository.downloadGroups("user-1")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(listOf("group-1"), local.groups.value.map { it.id })
        assertEquals(listOf("m-1"), local.members.value.map { it.id })
    }

    @Test
    fun `GIVEN a group uploaded and still on the server WHEN downloading THEN keeps it with its members`() = runTest {
        // Given
        prepareScenario(
            profileOwnerUserId = "user-1",
            remoteGroups = listOf(
                groupDto(
                    id = "group-new",
                    updatedAt = 1_000L,
                ),
            ),
            remoteMembers = listOf(
                memberDto(
                    id = "new-1",
                    groupId = "group-new",
                    userId = "user-1",
                    name = "Pierre",
                    updatedAt = 1_000L,
                ),
                memberDto(
                    id = "new-2",
                    groupId = "group-new",
                    updatedAt = 1_000L,
                ),
            ),
        )
        repository.createGroup(newGroup, createdAt = 1_000L)
        repository.uploadPendingGroups("user-1")

        // When
        repository.downloadGroups("user-1")

        // Then
        assertEquals(false, local.group("group-new")?.pendingSync)
        assertEquals(listOf("new-1", "new-2"), local.members.value.map { it.id })
    }

    private val newGroup = Group(
        id = "group-new",
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.PEACH,
        currency = GroupCurrency.BRL,
        members = listOf(
            GroupMember(
                id = "new-1",
                name = "Pierre",
                avatarColor = AvatarColor.LILAC,
                isMe = true,
                hasJoined = true,
            ),
            GroupMember(
                id = "new-2",
                name = "Ana",
                avatarColor = AvatarColor.GREEN,
                isMe = false,
                hasJoined = false,
            ),
        ),
        isPending = true,
    )

    private fun group(
        id: String,
        name: String = "Viagem Floripa",
        createdBy: String? = "user-1",
        updatedAt: Long = 1L,
        pendingSync: Boolean = false,
        ownerUserId: String? = "user-1",
    ): GroupEntity = GroupEntity(
        id = id,
        name = name,
        type = "trip",
        color = "blue",
        currency = "BRL",
        createdBy = createdBy,
        addedAt = 1L,
        updatedAt = updatedAt,
        pendingSync = pendingSync,
        ownerUserId = ownerUserId,
    )

    private fun member(
        id: String,
        groupId: String,
        name: String = "Ana",
        userId: String? = null,
        isMe: Boolean = false,
        updatedAt: Long = 1L,
        pendingSync: Boolean = false,
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
    )

    private fun groupDto(
        id: String,
        name: String = "Viagem Floripa",
        createdBy: String? = "user-1",
        updatedAt: Long = 1L,
    ): GroupDto = GroupDto(
        id = id,
        createdBy = createdBy,
        name = name,
        type = "trip",
        color = "blue",
        currency = "BRL",
        addedAt = 1L,
        updatedAt = updatedAt,
    )

    private fun memberDto(
        id: String,
        groupId: String,
        name: String = "Ana",
        userId: String? = null,
        updatedAt: Long = 1L,
    ): GroupMemberDto = GroupMemberDto(
        id = id,
        groupId = groupId,
        userId = userId,
        name = name,
        avatarColor = "green",
        addedAt = 1L,
        updatedAt = updatedAt,
    )

    private fun prepareScenario(
        storedGroups: List<GroupEntity> = emptyList(),
        storedMembers: List<GroupMemberEntity> = emptyList(),
        remoteGroups: List<GroupDto> = emptyList(),
        remoteMembers: List<GroupMemberDto> = emptyList(),
        upsertGroupsError: Throwable? = null,
        upsertMembersError: Throwable? = null,
        fetchError: Throwable? = null,
        profileOwnerUserId: String? = null,
        insertError: Throwable? = null,
    ) {
        local = FakeGroupLocalDataSource(storedGroups, storedMembers).also { it.insertError = insertError }
        remote = FakeGroupRemoteDataSource(
            groups = remoteGroups,
            members = remoteMembers,
            upsertGroupsError = upsertGroupsError,
            upsertMembersError = upsertMembersError,
            fetchError = fetchError,
        )
        repository = GroupRepositoryImpl(
            local = local,
            remote = remote,
            profileLocal = FakeGroupOwnerProfileLocalDataSource(profileOwnerUserId),
            mapper = GroupMapperImpl(),
        )
    }
}

// Why: the guards mirror GroupDao's queries, so the repository is checked against the same rules.
private class FakeGroupLocalDataSource(
    storedGroups: List<GroupEntity>,
    storedMembers: List<GroupMemberEntity>,
) : GroupLocalDataSource {
    val groups = MutableStateFlow(storedGroups)
    val members = MutableStateFlow(storedMembers)
    val groupWrites = mutableListOf<Pair<GroupEntity, List<GroupMemberEntity>>>()
    var insertError: Throwable? = null

    fun group(id: String): GroupEntity? = groups.value.find { it.id == id }

    fun member(id: String): GroupMemberEntity? = members.value.find { it.id == id }

    fun put(entity: GroupEntity) {
        groups.value = groups.value
            .map { if (it.id == entity.id) entity else it }
            .let { if (it.any { row -> row.id == entity.id }) it else it + entity }
    }

    private fun put(entity: GroupMemberEntity) {
        members.value = members.value
            .map { if (it.id == entity.id) entity else it }
            .let { if (it.any { row -> row.id == entity.id }) it else it + entity }
    }

    override fun observeGroups(): Flow<List<GroupEntity>> = groups

    override fun observeMembers(): Flow<List<GroupMemberEntity>> = members

    override fun observeHasPending(): Flow<Boolean> = combine(groups, members) { groupRows, memberRows ->
        groupRows.any { it.pendingSync } || memberRows.any { it.pendingSync }
    }

    override suspend fun getPendingGroups(ownerUserId: String): List<GroupEntity> =
        groups.value.filter { it.ownerUserId == ownerUserId && it.pendingSync }

    override suspend fun getPendingMembers(ownerUserId: String): List<GroupMemberEntity> {
        val owned = groups.value
            .filter { it.ownerUserId == ownerUserId }
            .map { it.id }
            .toSet()
        return members.value.filter { it.pendingSync && it.groupId in owned }
    }

    override suspend fun insertGroupWithMembers(
        group: GroupEntity,
        members: List<GroupMemberEntity>,
    ) {
        insertError?.let { throw it }
        groupWrites += group to members
        put(group)
        members.forEach(::put)
    }

    override suspend fun markGroupSynced(
        id: String,
        syncedUpdatedAt: Long,
    ) {
        groups.value = groups.value.map {
            if (it.id == id && it.updatedAt == syncedUpdatedAt) it.copy(pendingSync = false) else it
        }
    }

    override suspend fun markMemberSynced(
        id: String,
        syncedUpdatedAt: Long,
    ) {
        members.value = members.value.map {
            if (it.id == id && it.updatedAt == syncedUpdatedAt) it.copy(pendingSync = false) else it
        }
    }

    override suspend fun applyRemoteGroup(entity: GroupEntity) {
        val current = group(entity.id)
        when {
            current == null -> put(entity)

            current.ownerUserId == entity.ownerUserId && current.updatedAt < entity.updatedAt ->
                put(entity.copy(pendingSync = false))
        }
    }

    override suspend fun applyRemoteMember(entity: GroupMemberEntity) {
        val current = member(entity.id)
        when {
            current == null -> put(entity)
            current.updatedAt < entity.updatedAt -> put(entity.copy(pendingSync = false))
        }
    }

    override suspend fun claimUnowned(ownerUserId: String) {
        groups.value = groups.value.map {
            if (it.ownerUserId ==
                null
            ) {
                it.copy(
                    ownerUserId = ownerUserId,
                    createdBy = it.createdBy ?: ownerUserId,
                )
            } else {
                it
            }
        }
        members.value = members.value.map {
            if (it.isMe && it.userId == null) it.copy(userId = ownerUserId) else it
        }
    }

    override suspend fun deleteOtherUsersGroups(ownerUserId: String) {
        val others = groups.value.filter { it.ownerUserId != null && it.ownerUserId != ownerUserId }.map { it.id }
        members.value = members.value.filterNot { it.groupId in others }
        groups.value = groups.value.filterNot { it.id in others }
    }

    override suspend fun deleteGoneFromServer(
        ownerUserId: String,
        serverGroupIds: Set<String>,
        serverMemberIds: Set<String>,
    ) {
        val owned = groups.value.filter { it.ownerUserId == ownerUserId }
        val goneGroups = owned
            .filter { !it.pendingSync && it.id !in serverGroupIds }
            .map { it.id }
            .toSet()
        val ownedIds = owned.map { it.id }.toSet()
        members.value = members.value.filterNot {
            it.groupId in goneGroups ||
                (it.groupId in ownedIds && it.groupId in serverGroupIds && !it.pendingSync && it.id !in serverMemberIds)
        }
        groups.value = groups.value.filterNot { it.id in goneGroups }
    }

    override suspend fun clear() {
        members.value = emptyList()
        groups.value = emptyList()
    }
}

private class FakeGroupOwnerProfileLocalDataSource(
    private val ownerUserId: String?,
) : ProfileLocalDataSource {
    override fun observe(): Flow<ProfileEntity?> = error("unused")

    override suspend fun get(): ProfileEntity = ProfileEntity(
        id = "profile-1",
        displayName = "Pierre",
        avatarColor = "lilac",
        photoUrl = null,
        updatedAt = 1L,
        pendingSync = false,
        pendingPhoto = null,
        ownerUserId = ownerUserId,
    )

    override suspend fun upsert(entity: ProfileEntity) = error("unused")

    override suspend fun markSynced(
        syncedUpdatedAt: Long,
        photoUrl: String?,
    ) = error("unused")

    override suspend fun applyRemote(entity: ProfileEntity) = error("unused")

    override suspend fun claimUnowned(ownerUserId: String) = error("unused")

    override suspend fun replaceOtherUsersCopy(entity: ProfileEntity) = error("unused")

    override suspend fun reclaimOtherUsersCopy(
        ownerUserId: String,
        updatedAt: Long,
        photoUrl: String?,
    ) = error("unused")

    override suspend fun clear() = error("unused")
}

private class FakeGroupRemoteDataSource(
    private val groups: List<GroupDto>,
    private val members: List<GroupMemberDto>,
    private val upsertGroupsError: Throwable?,
    private val upsertMembersError: Throwable?,
    private val fetchError: Throwable?,
) : GroupRemoteDataSource {
    val calls = mutableListOf<String>()
    val upsertedGroups = mutableListOf<List<GroupDto>>()
    val upsertedMembers = mutableListOf<List<GroupMemberDto>>()
    var onUpsertGroups: () -> Unit = {}

    override suspend fun upsertGroups(groups: List<GroupDto>) {
        calls += "groups"
        onUpsertGroups()
        upsertGroupsError?.let { throw it }
        upsertedGroups += groups
    }

    override suspend fun upsertMembers(members: List<GroupMemberDto>) {
        calls += "members"
        upsertMembersError?.let { throw it }
        upsertedMembers += members
    }

    override suspend fun fetchGroups(): List<GroupDto> {
        fetchError?.let { throw it }
        return groups
    }

    override suspend fun fetchMembers(): List<GroupMemberDto> {
        fetchError?.let { throw it }
        return members
    }
}
