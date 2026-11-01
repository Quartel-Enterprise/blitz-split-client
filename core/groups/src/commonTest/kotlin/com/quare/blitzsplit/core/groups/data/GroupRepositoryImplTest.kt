package com.quare.blitzsplit.core.groups.data

import com.quare.blitzsplit.core.groups.data.datasource.GroupLocalDataSource
import com.quare.blitzsplit.core.groups.data.datasource.GroupRemoteDataSource
import com.quare.blitzsplit.core.groups.data.dto.GroupDto
import com.quare.blitzsplit.core.groups.data.dto.GroupExitResultDto
import com.quare.blitzsplit.core.groups.data.dto.GroupMemberDto
import com.quare.blitzsplit.core.groups.data.dto.InviteDto
import com.quare.blitzsplit.core.groups.data.dto.JoinGroupParamsDto
import com.quare.blitzsplit.core.groups.data.dto.JoinGroupResultDto
import com.quare.blitzsplit.core.groups.data.dto.PersonalInviteParamsDto
import com.quare.blitzsplit.core.groups.data.dto.ReleaseMemberParamsDto
import com.quare.blitzsplit.core.groups.data.dto.RemovedGroupDto
import com.quare.blitzsplit.core.groups.data.mapper.GroupMapperImpl
import com.quare.blitzsplit.core.groups.data.repository.GroupRepositoryImpl
import com.quare.blitzsplit.core.groups.data.repository.InMemoryGroupDeletionRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupDefaultSplit
import com.quare.blitzsplit.core.groups.domain.model.GroupDeletion
import com.quare.blitzsplit.core.groups.domain.model.GroupExitOutcome
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.DefaultSplitMode
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
    private lateinit var deletions: InMemoryGroupDeletionRepository
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
    fun `GIVEN a group someone else deleted WHEN downloading THEN it leaves even pending and its deletion is posted`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-deleted",
                        pendingSync = true,
                        inviteCode = "7Hk2pQxZ",
                    ),
                ),
                storedMembers = listOf(
                    member(
                        id = "m-1",
                        groupId = "group-deleted",
                        pendingSync = true,
                    ),
                ),
                removedGroups = listOf(
                    removedGroup(
                        groupId = "group-deleted",
                        reason = "deleted",
                        removedBy = "user-2",
                        removedByName = "Ana",
                    ),
                ),
            )

            // When
            val result = repository.downloadGroups("user-1")

            // Then
            assertTrue(result.isSuccess)
            assertTrue(local.groups.value.isEmpty())
            assertTrue(local.members.value.isEmpty())
            assertEquals(
                listOf(
                    GroupDeletion(
                        groupId = "group-deleted",
                        groupName = "Viagem Floripa",
                        deletedByName = "Ana",
                    ),
                ),
                deletions.observeDeletions().first(),
            )
        }

    @Test
    fun `GIVEN a group this user left or deleted elsewhere WHEN downloading THEN it leaves with no deletion posted`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-left",
                        pendingSync = true,
                        inviteCode = "7Hk2pQxZ",
                    ),
                    group(id = "group-deleted-by-me"),
                ),
                removedGroups = listOf(
                    removedGroup(
                        groupId = "group-left",
                        reason = "left",
                        removedBy = "user-1",
                        removedByName = "Pierre",
                    ),
                    removedGroup(
                        groupId = "group-deleted-by-me",
                        reason = "deleted",
                        removedBy = "user-1",
                        removedByName = "Pierre",
                    ),
                ),
            )

            // When
            val result = repository.downloadGroups("user-1")

            // Then
            assertTrue(result.isSuccess)
            assertTrue(local.groups.value.isEmpty())
            assertTrue(deletions.observeDeletions().first().isEmpty())
        }

    @Test
    fun `GIVEN removed groups that fail to load WHEN downloading THEN it succeeds and the gone groups still leave`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(id = "group-gone"),
                    group(
                        id = "group-pending",
                        pendingSync = true,
                        inviteCode = "7Hk2pQxZ",
                    ),
                ),
                removedGroupsError = IllegalStateException("no removed_groups yet"),
            )

            // When
            val result = repository.downloadGroups("user-1")

            // Then
            assertTrue(result.isSuccess)
            assertTrue(local.groups.value.isEmpty())
            assertTrue(deletions.observeDeletions().first().isEmpty())
        }

    @Test
    fun `GIVEN every stored group still on the server WHEN downloading THEN asks for no removed group`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            remoteGroups = listOf(groupDto(id = "group-1")),
        )

        // When
        val result = repository.downloadGroups("user-1")

        // Then
        assertTrue(result.isSuccess)
        assertTrue(remote.askedRemovedGroupIds.isEmpty())
    }

    @Test
    fun `GIVEN a group created offline and not pushed yet WHEN downloading THEN asks nothing and keeps it`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-new",
                    pendingSync = true,
                ),
            ),
        )

        // When
        val result = repository.downloadGroups("user-1")

        // Then
        assertTrue(result.isSuccess)
        assertTrue(remote.askedRemovedGroupIds.isEmpty())
        assertEquals(listOf("group-new"), local.groups.value.map { it.id })
    }

    @Test
    fun `GIVEN the server lets this person leave WHEN leaving THEN the group leaves the device with its members`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(group(id = "group-1"), group(id = "group-2")),
                storedMembers = listOf(
                    member(
                        id = "m-1",
                        groupId = "group-1",
                    ),
                    member(
                        id = "m-2",
                        groupId = "group-2",
                    ),
                ),
                exitStatus = "left",
            )

            // When
            val result = repository.requestLeaveGroup("group-1")

            // Then
            assertEquals(GroupExitOutcome.DONE, result.getOrNull())
            assertEquals(listOf("group-1"), remote.leftGroupIds)
            assertEquals(listOf("group-2"), local.groups.value.map { it.id })
            assertEquals(listOf("m-2"), local.members.value.map { it.id })
        }

    @Test
    fun `GIVEN this person still has a balance WHEN leaving THEN answers it and keeps the group`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            exitStatus = "has_balance",
        )

        // When
        val result = repository.requestLeaveGroup("group-1")

        // Then
        assertEquals(GroupExitOutcome.HAS_BALANCE, result.getOrNull())
        assertEquals(listOf("group-1"), local.groups.value.map { it.id })
    }

    @Test
    fun `GIVEN another device already took this person out WHEN leaving THEN the group leaves this one too`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(group(id = "group-1")),
                exitStatus = "not_member",
            )

            // When
            val result = repository.requestLeaveGroup("group-1")

            // Then
            assertEquals(GroupExitOutcome.DONE, result.getOrNull())
            assertTrue(local.groups.value.isEmpty())
        }

    @Test
    fun `GIVEN the server deletes the group WHEN deleting THEN the group leaves the device`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            exitStatus = "deleted",
        )

        // When
        val result = repository.deleteGroup("group-1")

        // Then
        assertEquals(GroupExitOutcome.DONE, result.getOrNull())
        assertEquals(listOf("group-1"), remote.deletedGroupIds)
        assertTrue(local.groups.value.isEmpty())
    }

    @Test
    fun `GIVEN someone still owes WHEN deleting THEN answers it and keeps the group`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            exitStatus = "has_balance",
        )

        // When
        val result = repository.deleteGroup("group-1")

        // Then
        assertEquals(GroupExitOutcome.HAS_BALANCE, result.getOrNull())
        assertEquals(listOf("group-1"), local.groups.value.map { it.id })
    }

    @Test
    fun `GIVEN the server unreachable WHEN deleting THEN fails and keeps the group`() = runTest {
        // Given
        val error = IllegalStateException("offline")
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            exitError = error,
        )

        // When
        val result = repository.deleteGroup("group-1")

        // Then
        assertEquals(error, result.exceptionOrNull())
        assertEquals(listOf("group-1"), local.groups.value.map { it.id })
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
    fun `GIVEN a member whose group the server did not return WHEN downloading THEN does not store it`() = runTest {
        // Given
        prepareScenario(
            remoteGroups = listOf(groupDto(id = "group-1")),
            remoteMembers = listOf(
                memberDto(
                    id = "m-1",
                    groupId = "group-1",
                ),
                memberDto(
                    id = "m-2",
                    groupId = "group-2",
                ),
            ),
        )

        // When
        repository.downloadGroups("user-1")

        // Then
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

    @Test
    fun `GIVEN a group just created WHEN observing THEN it has no invite code yet`() = runTest {
        // Given
        prepareScenario(profileOwnerUserId = "user-1")
        repository.createGroup(newGroup, createdAt = 1_000L)

        // When
        val groups = repository.observeGroups().first()

        // Then
        assertNull(groups.single().inviteCode)
    }

    @Test
    fun `GIVEN a group going up for the first time WHEN uploading THEN stores the invite code the server made`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        pendingSync = true,
                    ),
                ),
                serverInviteCodes = mapOf("group-1" to "aB3dE5gH"),
            )

            // When
            val result = repository.uploadPendingGroups("user-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals("aB3dE5gH", local.group("group-1")?.inviteCode)
            assertEquals(false, local.group("group-1")?.pendingSync)
        }

    @Test
    fun `GIVEN a group created on the device WHEN its first upload succeeds THEN the group shows the invite code`() =
        runTest {
            // Given
            prepareScenario(
                profileOwnerUserId = "user-1",
                serverInviteCodes = mapOf("group-new" to "aB3dE5gH"),
            )
            repository.createGroup(newGroup, createdAt = 1_000L)

            // When
            repository.uploadPendingGroups("user-1")

            // Then
            val group = repository.observeGroups().first().single()
            assertEquals("aB3dE5gH", group.inviteCode)
            assertFalse(group.isPending)
        }

    @Test
    fun `GIVEN two groups going up WHEN uploading THEN each stores its own invite code`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    pendingSync = true,
                ),
                group(
                    id = "group-2",
                    pendingSync = true,
                ),
            ),
            serverInviteCodes = mapOf(
                "group-1" to "aB3dE5gH",
                "group-2" to "Zx9Yw8Vu",
            ),
        )

        // When
        repository.uploadPendingGroups("user-1")

        // Then
        assertEquals("aB3dE5gH", local.group("group-1")?.inviteCode)
        assertEquals("Zx9Yw8Vu", local.group("group-2")?.inviteCode)
    }

    @Test
    fun `GIVEN a group with an invite code edited here WHEN uploading THEN pushes it without the code`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    pendingSync = true,
                    inviteCode = "aB3dE5gH",
                ),
            ),
        )

        // When
        repository.uploadPendingGroups("user-1")

        // Then
        assertNull(
            remote.upsertedGroups
                .single()
                .single()
                .inviteCode,
        )
    }

    @Test
    fun `GIVEN the server answers a group without a code WHEN uploading THEN keeps the code the device had`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        pendingSync = true,
                        inviteCode = "aB3dE5gH",
                    ),
                ),
                serverInviteCodes = emptyMap(),
            )

            // When
            val result = repository.uploadPendingGroups("user-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals("aB3dE5gH", local.group("group-1")?.inviteCode)
        }

    @Test
    fun `GIVEN the server answers a new group without a code WHEN uploading THEN the group still has none`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    pendingSync = true,
                ),
            ),
            serverInviteCodes = emptyMap(),
        )

        // When
        val result = repository.uploadPendingGroups("user-1")

        // Then
        assertTrue(result.isSuccess)
        assertNull(local.group("group-1")?.inviteCode)
        assertEquals(false, local.group("group-1")?.pendingSync)
    }

    @Test
    fun `GIVEN the server fails the groups WHEN uploading THEN stores no invite code`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    pendingSync = true,
                ),
            ),
            upsertGroupsError = IllegalStateException("server down"),
            serverInviteCodes = mapOf("group-1" to "aB3dE5gH"),
        )

        // When
        val result = repository.uploadPendingGroups("user-1")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertNull(local.group("group-1")?.inviteCode)
        assertEquals(true, local.group("group-1")?.pendingSync)
    }

    @Test
    fun `GIVEN a group only the server has WHEN downloading THEN the group shows its invite code`() = runTest {
        // Given
        prepareScenario(
            remoteGroups = listOf(
                groupDto(
                    id = "group-1",
                    inviteCode = "aB3dE5gH",
                ),
            ),
        )

        // When
        repository.downloadGroups("user-1")

        // Then
        assertEquals("aB3dE5gH", local.group("group-1")?.inviteCode)
        assertEquals(
            "aB3dE5gH",
            repository
                .observeGroups()
                .first()
                .single()
                .inviteCode,
        )
    }

    @Test
    fun `GIVEN a group pushed from here with the same server updatedAt WHEN downloading THEN takes its invite code`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        name = "Viagem Floripa",
                        updatedAt = 5L,
                    ),
                ),
                remoteGroups = listOf(
                    groupDto(
                        id = "group-1",
                        name = "Viagem Floripa",
                        updatedAt = 5L,
                        inviteCode = "aB3dE5gH",
                    ),
                ),
            )

            // When
            val result = repository.downloadGroups("user-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals("aB3dE5gH", local.group("group-1")?.inviteCode)
            assertEquals(false, local.group("group-1")?.pendingSync)
        }

    @Test
    fun `GIVEN a group edited here after its push WHEN downloading THEN keeps the edit and takes the invite code`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        name = "Viagem Floripa 2026",
                        updatedAt = 9L,
                        pendingSync = true,
                    ),
                ),
                remoteGroups = listOf(
                    groupDto(
                        id = "group-1",
                        name = "Viagem Floripa",
                        updatedAt = 5L,
                        inviteCode = "aB3dE5gH",
                    ),
                ),
            )

            // When
            repository.downloadGroups("user-1")

            // Then
            val stored = local.group("group-1")
            assertEquals("Viagem Floripa 2026", stored?.name)
            assertEquals(9L, stored?.updatedAt)
            assertEquals(true, stored?.pendingSync)
            assertEquals("aB3dE5gH", stored?.inviteCode)
        }

    @Test
    fun `GIVEN a newer server copy of a group WHEN downloading THEN takes its invite code with it`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    updatedAt = 5L,
                ),
            ),
            remoteGroups = listOf(
                groupDto(
                    id = "group-1",
                    name = "Viagem Floripa 2026",
                    updatedAt = 9L,
                    inviteCode = "aB3dE5gH",
                ),
            ),
        )

        // When
        repository.downloadGroups("user-1")

        // Then
        assertEquals("Viagem Floripa 2026", local.group("group-1")?.name)
        assertEquals("aB3dE5gH", local.group("group-1")?.inviteCode)
    }

    @Test
    fun `GIVEN a group with an invite code WHEN changing its link THEN asks the server and stores the new code`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        inviteCode = "aB3dE5gH",
                    ),
                ),
            )

            // When
            val result = repository.rotateInviteCode("group-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf("group-1"), remote.rotatedInviteCodeGroupIds)
            assertEquals("newCode1", local.group("group-1")?.inviteCode)
        }

    @Test
    fun `GIVEN a group with an invite code WHEN changing its link THEN the group shows the new code without a sync`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        inviteCode = "aB3dE5gH",
                    ),
                ),
            )

            // When
            repository.rotateInviteCode("group-1")

            // Then
            assertEquals(
                "newCode1",
                repository
                    .observeGroups()
                    .first()
                    .single()
                    .inviteCode,
            )
            assertTrue(remote.calls.isEmpty())
        }

    @Test
    fun `GIVEN a synced group WHEN changing its link THEN it stays synced with nothing to push`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    updatedAt = 5L,
                    pendingSync = false,
                    inviteCode = "aB3dE5gH",
                ),
            ),
        )

        // When
        repository.rotateInviteCode("group-1")

        // Then
        assertEquals(false, local.group("group-1")?.pendingSync)
        assertEquals(5L, local.group("group-1")?.updatedAt)
        assertFalse(repository.observeHasPendingChanges().first())
    }

    @Test
    fun `GIVEN the server refuses WHEN changing the link THEN returns the error and keeps the old code`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    inviteCode = "aB3dE5gH",
                ),
            ),
            rotateInviteCodeError = IllegalStateException("only the creator may change the link"),
        )

        // When
        val result = repository.rotateInviteCode("group-1")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals("aB3dE5gH", local.group("group-1")?.inviteCode)
    }

    @Test
    fun `GIVEN two groups with invite codes WHEN changing the link of one THEN the other keeps its code`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    inviteCode = "aB3dE5gH",
                ),
                group(
                    id = "group-2",
                    inviteCode = "Zx9Yw8Vu",
                ),
            ),
        )
        val otherBefore = local.group("group-2")

        // When
        repository.rotateInviteCode("group-1")

        // Then
        assertEquals("newCode1", local.group("group-1")?.inviteCode)
        assertEquals(otherBefore, local.group("group-2"))
    }

    @Test
    fun `GIVEN a profile of an account WHEN creating a group THEN the group shows as created by me`() = runTest {
        // Given
        prepareScenario(profileOwnerUserId = "user-1")
        repository.createGroup(newGroup, createdAt = 1_000L)

        // When
        val group = repository.observeGroups().first().single()

        // Then
        assertTrue(group.isCreatedByMe)
    }

    @Test
    fun `GIVEN a group created before any account WHEN observing THEN it shows as created by me`() = runTest {
        // Given
        prepareScenario(profileOwnerUserId = null)
        repository.createGroup(newGroup, createdAt = 1_000L)

        // When
        val group = repository.observeGroups().first().single()

        // Then
        assertTrue(group.isCreatedByMe)
    }

    @Test
    fun `GIVEN a group created before any account WHEN a download claims it THEN it shows as created by me`() =
        runTest {
            // Given
            prepareScenario(profileOwnerUserId = null)
            repository.createGroup(newGroup, createdAt = 1_000L)

            // When
            repository.downloadGroups("user-1")

            // Then
            assertTrue(
                repository
                    .observeGroups()
                    .first()
                    .single()
                    .isCreatedByMe,
            )
        }

    @Test
    fun `GIVEN a group another account created WHEN downloading THEN it is not created by me`() = runTest {
        // Given
        prepareScenario(
            remoteGroups = listOf(
                groupDto(
                    id = "group-1",
                    createdBy = "user-2",
                ),
            ),
        )

        // When
        repository.downloadGroups("user-1")

        // Then
        assertFalse(
            repository
                .observeGroups()
                .first()
                .single()
                .isCreatedByMe,
        )
    }

    @Test
    fun `GIVEN a group on the device WHEN adding a member THEN stores them pending with no account at that time`() =
        runTest {
            // Given
            prepareScenario(storedGroups = listOf(group(id = "group-1")))

            // When
            val result = repository.addMember(
                groupId = "group-1",
                member = bruno,
                addedAt = 5_000L,
            )

            // Then
            assertTrue(result.isSuccess)
            assertEquals(
                GroupMemberEntity(
                    id = "m-new",
                    groupId = "group-1",
                    name = "Bruno",
                    avatarColor = "sand",
                    userId = null,
                    isMe = false,
                    addedAt = 5_000L,
                    updatedAt = 5_000L,
                    pendingSync = true,
                    isRemoved = false,
                    inviteCode = null,
                ),
                local.member("m-new"),
            )
        }

    @Test
    fun `GIVEN a member just added WHEN observing the groups THEN the group shows them pending and not joined`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(group(id = "group-1")),
                storedMembers = listOf(
                    member(
                        id = "m-1",
                        groupId = "group-1",
                        name = "Pierre",
                        isMe = true,
                        userId = "user-1",
                    ),
                ),
            )
            repository.addMember(
                groupId = "group-1",
                member = bruno,
                addedAt = 5_000L,
            )

            // When
            val group = repository.observeGroups().first().single()

            // Then
            assertEquals(listOf("Pierre", "Bruno"), group.members.map { it.name })
            assertFalse(group.members[1].hasJoined)
            assertTrue(group.isPending)
        }

    @Test
    fun `GIVEN a group no longer on the device WHEN adding a member THEN returns the error and stores nothing`() =
        runTest {
            // Given
            prepareScenario(storedGroups = listOf(group(id = "group-1")))

            // When
            val result = repository.addMember(
                groupId = "group-gone",
                member = bruno,
                addedAt = 5_000L,
            )

            // Then
            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertTrue(local.members.value.isEmpty())
        }

    @Test
    fun `GIVEN the device fails to write WHEN adding a member THEN returns the error instead of throwing`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            memberWriteError = IllegalStateException("disk full"),
        )

        // When
        val result = repository.addMember(
            groupId = "group-1",
            member = bruno,
            addedAt = 5_000L,
        )

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertNull(local.member("m-new"))
    }

    @Test
    fun `GIVEN a stored member WHEN renaming them THEN stores the new name pending at that time`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            storedMembers = listOf(
                member(
                    id = "m-1",
                    groupId = "group-1",
                    name = "Ana",
                ),
            ),
        )

        // When
        val result = repository.updateMemberName(
            memberId = "m-1",
            name = "Ana Paula",
            updatedAt = 6_000L,
        )

        // Then
        assertTrue(result.isSuccess)
        assertEquals("Ana Paula", local.member("m-1")?.name)
        assertEquals(6_000L, local.member("m-1")?.updatedAt)
        assertEquals(true, local.member("m-1")?.pendingSync)
    }

    @Test
    fun `GIVEN the device fails to write WHEN renaming a member THEN returns the error instead of throwing`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(group(id = "group-1")),
                storedMembers = listOf(
                    member(
                        id = "m-1",
                        groupId = "group-1",
                        name = "Ana",
                    ),
                ),
                memberWriteError = IllegalStateException("disk full"),
            )

            // When
            val result = repository.updateMemberName(
                memberId = "m-1",
                name = "Ana Paula",
                updatedAt = 6_000L,
            )

            // Then
            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertEquals("Ana", local.member("m-1")?.name)
        }

    @Test
    fun `GIVEN a stored group WHEN editing it THEN stores the new name type color and currency at that time`() =
        runTest {
            // Given
            prepareScenario(storedGroups = listOf(group(id = "group-1")))

            // When
            val result = repository.updateGroup(
                groupId = "group-1",
                name = "Casa da praia",
                type = GroupType.HOME,
                color = AvatarColor.PEACH,
                currency = GroupCurrency.USD,
                updatedAt = 7_000L,
            )

            // Then
            assertTrue(result.isSuccess)
            val stored = local.group("group-1")
            assertEquals("Casa da praia", stored?.name)
            assertEquals("home", stored?.type)
            assertEquals("peach", stored?.color)
            assertEquals("USD", stored?.currency)
            assertEquals(7_000L, stored?.updatedAt)
        }

    @Test
    fun `GIVEN a synced group WHEN editing it THEN the group shows pending until the next sync`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    pendingSync = false,
                ),
            ),
        )
        repository.updateGroup(
            groupId = "group-1",
            name = "Casa da praia",
            type = GroupType.HOME,
            color = AvatarColor.PEACH,
            currency = GroupCurrency.USD,
            updatedAt = 7_000L,
        )

        // When
        val group = repository.observeGroups().first().single()

        // Then
        assertTrue(group.isPending)
        assertEquals("Casa da praia", group.name)
        assertEquals(GroupType.HOME, group.type)
        assertEquals(AvatarColor.PEACH, group.color)
        assertEquals(GroupCurrency.USD, group.currency)
        assertTrue(repository.observeHasPendingChanges().first())
    }

    @Test
    fun `GIVEN a group with members and an invite code WHEN editing it THEN keeps its creator members and code`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        createdBy = "user-1",
                        ownerUserId = "user-1",
                        inviteCode = "Xy7kQ2",
                    ),
                ),
                storedMembers = listOf(
                    member(
                        id = "m-1",
                        groupId = "group-1",
                        name = "Pierre",
                        userId = "user-1",
                        isMe = true,
                    ),
                    member(
                        id = "m-2",
                        groupId = "group-1",
                        name = "Ana",
                        inviteCode = "Ab3dE5",
                    ),
                ),
            )
            val membersBefore = local.members.value

            // When
            repository.updateGroup(
                groupId = "group-1",
                name = "Casa da praia",
                type = GroupType.HOME,
                color = AvatarColor.PEACH,
                currency = GroupCurrency.USD,
                updatedAt = 7_000L,
            )

            // Then
            val stored = local.group("group-1")
            assertEquals("user-1", stored?.createdBy)
            assertEquals("user-1", stored?.ownerUserId)
            assertEquals("Xy7kQ2", stored?.inviteCode)
            assertEquals(1L, stored?.addedAt)
            assertEquals(membersBefore, local.members.value)
        }

    @Test
    fun `GIVEN two stored groups WHEN editing one THEN the other stays as it was`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(id = "group-1"),
                group(
                    id = "group-2",
                    name = "Rep",
                ),
            ),
        )
        val otherBefore = local.group("group-2")

        // When
        repository.updateGroup(
            groupId = "group-1",
            name = "Casa da praia",
            type = GroupType.HOME,
            color = AvatarColor.PEACH,
            currency = GroupCurrency.USD,
            updatedAt = 7_000L,
        )

        // Then
        assertEquals(otherBefore, local.group("group-2"))
    }

    @Test
    fun `GIVEN a stored group WHEN editing it THEN calls nothing on the server`() = runTest {
        // Given
        prepareScenario(storedGroups = listOf(group(id = "group-1")))

        // When
        repository.updateGroup(
            groupId = "group-1",
            name = "Casa da praia",
            type = GroupType.HOME,
            color = AvatarColor.PEACH,
            currency = GroupCurrency.USD,
            updatedAt = 7_000L,
        )

        // Then
        assertTrue(remote.calls.isEmpty())
    }

    @Test
    fun `GIVEN the device fails to write WHEN editing a group THEN returns the error instead of throwing`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            groupWriteError = IllegalStateException("disk full"),
        )

        // When
        val result = repository.updateGroup(
            groupId = "group-1",
            name = "Casa da praia",
            type = GroupType.HOME,
            color = AvatarColor.PEACH,
            currency = GroupCurrency.USD,
            updatedAt = 7_000L,
        )

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals("Viagem Floripa", local.group("group-1")?.name)
        assertEquals(false, local.group("group-1")?.pendingSync)
    }

    @Test
    fun `GIVEN a member who has not joined WHEN removing them THEN keeps the removal pending at that time`() = runTest {
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
        val result = repository.removeMember(
            memberId = "m-1",
            removedAt = 7_000L,
        )

        // Then
        assertTrue(result.isSuccess)
        assertEquals(true, local.member("m-1")?.isRemoved)
        assertEquals(true, local.member("m-1")?.pendingSync)
        assertEquals(7_000L, local.member("m-1")?.updatedAt)
    }

    @Test
    fun `GIVEN a member just removed WHEN observing the groups THEN the group hides them and is pending`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            storedMembers = listOf(
                member(
                    id = "m-1",
                    groupId = "group-1",
                    name = "Pierre",
                    isMe = true,
                    userId = "user-1",
                ),
                member(
                    id = "m-2",
                    groupId = "group-1",
                    name = "Ana",
                ),
            ),
        )
        repository.removeMember(
            memberId = "m-2",
            removedAt = 7_000L,
        )

        // When
        val group = repository.observeGroups().first().single()

        // Then
        assertEquals(listOf("Pierre"), group.members.map { it.name })
        assertTrue(group.isPending)
    }

    @Test
    fun `GIVEN the device fails to write WHEN removing a member THEN returns the error instead of throwing`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(group(id = "group-1")),
                storedMembers = listOf(
                    member(
                        id = "m-1",
                        groupId = "group-1",
                    ),
                ),
                memberWriteError = IllegalStateException("disk full"),
            )

            // When
            val result = repository.removeMember(
                memberId = "m-1",
                removedAt = 7_000L,
            )

            // Then
            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertEquals(false, local.member("m-1")?.isRemoved)
        }

    @Test
    fun `GIVEN a removal and pending changes WHEN uploading THEN deletes after the group and member pushes`() =
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
                    removedMember(id = "m-2"),
                ),
            )

            // When
            val result = repository.uploadPendingGroups("user-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf("groups", "members", "delete members"), remote.calls)
            assertEquals(listOf(listOf("m-2")), remote.deletedMemberIds)
        }

    @Test
    fun `GIVEN a member removed here WHEN uploading THEN the member push does not carry them`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            storedMembers = listOf(
                member(
                    id = "m-1",
                    groupId = "group-1",
                    pendingSync = true,
                ),
                removedMember(id = "m-2"),
            ),
        )

        // When
        repository.uploadPendingGroups("user-1")

        // Then
        assertEquals(listOf("m-1"), remote.upsertedMembers.flatten().map { it.id })
    }

    @Test
    fun `GIVEN a member removed here WHEN the server deletes them THEN they leave the device`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            storedMembers = listOf(
                member(
                    id = "m-1",
                    groupId = "group-1",
                ),
                removedMember(id = "m-2"),
            ),
        )

        // When
        val result = repository.uploadPendingGroups("user-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("m-1"), local.members.value.map { it.id })
    }

    @Test
    fun `GIVEN only a removal WHEN uploading THEN deletes it without pushing groups or members`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            storedMembers = listOf(removedMember(id = "m-2")),
        )

        // When
        repository.uploadPendingGroups("user-1")

        // Then
        assertEquals(listOf("delete members"), remote.calls)
    }

    @Test
    fun `GIVEN nothing removed WHEN uploading THEN deletes nothing on the server`() = runTest {
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
        repository.uploadPendingGroups("user-1")

        // Then
        assertFalse("delete members" in remote.calls)
        assertTrue(remote.deletedMemberIds.isEmpty())
    }

    @Test
    fun `GIVEN the server fails the delete WHEN uploading THEN returns the error and keeps the removal`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            storedMembers = listOf(removedMember(id = "m-2")),
            deleteMembersError = IllegalStateException("server down"),
        )

        // When
        val result = repository.uploadPendingGroups("user-1")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(true, local.member("m-2")?.isRemoved)
        assertEquals(true, local.member("m-2")?.pendingSync)
    }

    @Test
    fun `GIVEN the server fails the delete WHEN uploading THEN what went up before it is synced`() = runTest {
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
                removedMember(id = "m-2"),
            ),
            deleteMembersError = IllegalStateException("server down"),
        )

        // When
        repository.uploadPendingGroups("user-1")

        // Then
        assertEquals(false, local.group("group-1")?.pendingSync)
        assertEquals(false, local.member("m-1")?.pendingSync)
    }

    @Test
    fun `GIVEN the server fails the delete once WHEN uploading again THEN deletes the member then`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            storedMembers = listOf(removedMember(id = "m-2")),
            deleteMembersError = IllegalStateException("server down"),
        )
        repository.uploadPendingGroups("user-1")
        remote.deleteMembersError = null

        // When
        val result = repository.uploadPendingGroups("user-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(listOf("m-2")), remote.deletedMemberIds)
        assertNull(local.member("m-2"))
    }

    @Test
    fun `GIVEN the server fails the members WHEN uploading THEN deletes nothing and keeps the removal`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            storedMembers = listOf(
                member(
                    id = "m-1",
                    groupId = "group-1",
                    pendingSync = true,
                ),
                removedMember(id = "m-2"),
            ),
            upsertMembersError = IllegalStateException("server down"),
        )

        // When
        repository.uploadPendingGroups("user-1")

        // Then
        assertTrue(remote.deletedMemberIds.isEmpty())
        assertEquals(true, local.member("m-2")?.isRemoved)
    }

    @Test
    fun `GIVEN a removal already deleted on the server WHEN uploading again THEN deletes nothing`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            storedMembers = listOf(removedMember(id = "m-2")),
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
    fun `GIVEN a member removed in another user group WHEN uploading THEN does not delete them`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    ownerUserId = "user-2",
                ),
            ),
            storedMembers = listOf(removedMember(id = "m-2")),
        )

        // When
        repository.uploadPendingGroups("user-1")

        // Then
        assertTrue(remote.deletedMemberIds.isEmpty())
        assertEquals(true, local.member("m-2")?.isRemoved)
    }

    @Test
    fun `GIVEN a member removed in a group nobody owns yet WHEN uploading THEN does not delete them`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    ownerUserId = null,
                ),
            ),
            storedMembers = listOf(removedMember(id = "m-2")),
        )

        // When
        repository.uploadPendingGroups("user-1")

        // Then
        assertTrue(remote.deletedMemberIds.isEmpty())
        assertEquals(true, local.member("m-2")?.isRemoved)
    }

    @Test
    fun `GIVEN a member removed here and edited newer on the server WHEN downloading THEN they come back synced`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(group(id = "group-1")),
                storedMembers = listOf(
                    removedMember(
                        id = "m-2",
                        updatedAt = 5L,
                    ),
                ),
                remoteGroups = listOf(groupDto(id = "group-1")),
                remoteMembers = listOf(
                    memberDto(
                        id = "m-2",
                        groupId = "group-1",
                        name = "Ana Paula",
                        updatedAt = 9L,
                    ),
                ),
            )

            // When
            repository.downloadGroups("user-1")

            // Then
            assertEquals(false, local.member("m-2")?.isRemoved)
            assertEquals(false, local.member("m-2")?.pendingSync)
            assertEquals(
                listOf("Ana Paula"),
                repository
                    .observeGroups()
                    .first()
                    .single()
                    .members
                    .map { it.name },
            )
        }

    @Test
    fun `GIVEN a member removed here after the server copy WHEN downloading THEN the removal stays pending`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(group(id = "group-1")),
                storedMembers = listOf(
                    removedMember(
                        id = "m-2",
                        updatedAt = 9L,
                    ),
                ),
                remoteGroups = listOf(groupDto(id = "group-1")),
                remoteMembers = listOf(
                    memberDto(
                        id = "m-2",
                        groupId = "group-1",
                        updatedAt = 5L,
                    ),
                ),
            )

            // When
            repository.downloadGroups("user-1")

            // Then
            assertEquals(true, local.member("m-2")?.isRemoved)
            assertEquals(true, local.member("m-2")?.pendingSync)
            assertTrue(
                repository
                    .observeGroups()
                    .first()
                    .single()
                    .members
                    .isEmpty(),
            )
        }

    @Test
    fun `GIVEN a removed member who joined before the delete WHEN downloading next THEN they come back joined`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(group(id = "group-1")),
                storedMembers = listOf(
                    removedMember(
                        id = "m-2",
                        updatedAt = 9L,
                    ),
                ),
                remoteGroups = listOf(groupDto(id = "group-1")),
                remoteMembers = listOf(
                    memberDto(
                        id = "m-2",
                        groupId = "group-1",
                        userId = "user-2",
                        updatedAt = 5L,
                    ),
                ),
            )
            repository.uploadPendingGroups("user-1")

            // When
            repository.downloadGroups("user-1")

            // Then
            val members = repository
                .observeGroups()
                .first()
                .single()
                .members
            assertEquals(listOf("m-2"), members.map { it.id })
            assertTrue(members.single().hasJoined)
        }

    @Test
    fun `GIVEN a member going up for the first time WHEN uploading THEN stores the invite code the server made`() =
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
                serverMemberInviteCodes = mapOf("m-1" to "Xy7kQ2"),
            )

            // When
            val result = repository.uploadPendingGroups("user-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals("Xy7kQ2", local.member("m-1")?.inviteCode)
            assertEquals(false, local.member("m-1")?.pendingSync)
        }

    @Test
    fun `GIVEN two members going up WHEN uploading THEN each stores its own invite code`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
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
            serverMemberInviteCodes = mapOf(
                "m-1" to "Xy7kQ2",
                "m-2" to "Pq9Rs4",
            ),
        )

        // When
        repository.uploadPendingGroups("user-1")

        // Then
        assertEquals("Xy7kQ2", local.member("m-1")?.inviteCode)
        assertEquals("Pq9Rs4", local.member("m-2")?.inviteCode)
    }

    @Test
    fun `GIVEN a renamed member with an invite code WHEN uploading THEN pushes it without the code`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            storedMembers = listOf(
                member(
                    id = "m-1",
                    groupId = "group-1",
                    pendingSync = true,
                    inviteCode = "Xy7kQ2",
                ),
            ),
        )

        // When
        repository.uploadPendingGroups("user-1")

        // Then
        assertNull(
            remote.upsertedMembers
                .single()
                .single()
                .inviteCode,
        )
    }

    @Test
    fun `GIVEN the server answers a member without a code WHEN uploading THEN keeps the code the device had`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(group(id = "group-1")),
                storedMembers = listOf(
                    member(
                        id = "m-1",
                        groupId = "group-1",
                        pendingSync = true,
                        inviteCode = "Xy7kQ2",
                    ),
                ),
                serverMemberInviteCodes = emptyMap(),
            )

            // When
            val result = repository.uploadPendingGroups("user-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals("Xy7kQ2", local.member("m-1")?.inviteCode)
        }

    @Test
    fun `GIVEN the server answers a new member without a code WHEN uploading THEN the member still has none`() =
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
                    member(
                        id = "m-2",
                        groupId = "group-1",
                        pendingSync = true,
                    ),
                ),
                serverMemberInviteCodes = mapOf("m-2" to "Pq9Rs4"),
            )

            // When
            val result = repository.uploadPendingGroups("user-1")

            // Then
            assertTrue(result.isSuccess)
            assertNull(local.member("m-1")?.inviteCode)
            assertEquals(false, local.member("m-1")?.pendingSync)
            assertEquals("Pq9Rs4", local.member("m-2")?.inviteCode)
        }

    @Test
    fun `GIVEN the server fails the members WHEN uploading THEN stores no member invite code`() = runTest {
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
            serverMemberInviteCodes = mapOf("m-1" to "Xy7kQ2"),
        )

        // When
        val result = repository.uploadPendingGroups("user-1")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertNull(local.member("m-1")?.inviteCode)
        assertEquals(true, local.member("m-1")?.pendingSync)
    }

    @Test
    fun `GIVEN a group created offline WHEN observing THEN none of its members has an invite code yet`() = runTest {
        // Given
        prepareScenario(profileOwnerUserId = "user-1")
        repository.createGroup(newGroup, createdAt = 1_000L)

        // When
        val members = repository
            .observeGroups()
            .first()
            .single()
            .members

        // Then
        assertEquals(listOf(null, null), members.map { it.inviteCode })
    }

    @Test
    fun `GIVEN a member just added offline WHEN observing the groups THEN the member has no invite code yet`() =
        runTest {
            // Given
            prepareScenario(storedGroups = listOf(group(id = "group-1")))
            repository.addMember(
                groupId = "group-1",
                member = bruno,
                addedAt = 5_000L,
            )

            // When
            val member = repository
                .observeGroups()
                .first()
                .single()
                .members
                .single()

            // Then
            assertNull(member.inviteCode)
        }

    @Test
    fun `GIVEN a group created on the device WHEN its first upload succeeds THEN each member shows its invite code`() =
        runTest {
            // Given
            prepareScenario(
                profileOwnerUserId = "user-1",
                serverInviteCodes = mapOf("group-new" to "aB3dE5gH"),
                serverMemberInviteCodes = mapOf(
                    "new-1" to "Xy7kQ2",
                    "new-2" to "Pq9Rs4",
                ),
            )
            repository.createGroup(newGroup, createdAt = 1_000L)

            // When
            repository.uploadPendingGroups("user-1")

            // Then
            val members = repository
                .observeGroups()
                .first()
                .single()
                .members
            assertEquals(listOf("Xy7kQ2", "Pq9Rs4"), members.map { it.inviteCode })
        }

    @Test
    fun `GIVEN a member only the server has WHEN downloading THEN the member shows its invite code`() = runTest {
        // Given
        prepareScenario(
            remoteGroups = listOf(groupDto(id = "group-1")),
            remoteMembers = listOf(
                memberDto(
                    id = "m-1",
                    groupId = "group-1",
                    inviteCode = "Xy7kQ2",
                ),
            ),
        )

        // When
        repository.downloadGroups("user-1")

        // Then
        assertEquals(
            "Xy7kQ2",
            repository
                .observeGroups()
                .first()
                .single()
                .members
                .single()
                .inviteCode,
        )
    }

    @Test
    fun `GIVEN a member pushed from here with the same server updatedAt WHEN downloading THEN takes its invite code`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(group(id = "group-1")),
                storedMembers = listOf(
                    member(
                        id = "m-1",
                        groupId = "group-1",
                        updatedAt = 5L,
                    ),
                ),
                remoteGroups = listOf(groupDto(id = "group-1")),
                remoteMembers = listOf(
                    memberDto(
                        id = "m-1",
                        groupId = "group-1",
                        updatedAt = 5L,
                        inviteCode = "Xy7kQ2",
                    ),
                ),
            )

            // When
            val result = repository.downloadGroups("user-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals("Xy7kQ2", local.member("m-1")?.inviteCode)
        }

    @Test
    fun `GIVEN a member renamed here after its push WHEN downloading THEN keeps the new name and takes the code`() =
        runTest {
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
                        inviteCode = "Xy7kQ2",
                    ),
                ),
            )

            // When
            repository.downloadGroups("user-1")

            // Then
            val stored = local.member("m-1")
            assertEquals("Ana Paula", stored?.name)
            assertEquals(9L, stored?.updatedAt)
            assertEquals(true, stored?.pendingSync)
            assertEquals("Xy7kQ2", stored?.inviteCode)
        }

    @Test
    fun `GIVEN a newer server copy of a member WHEN downloading THEN takes its invite code with it`() = runTest {
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
                    name = "Ana Paula",
                    updatedAt = 9L,
                    inviteCode = "Xy7kQ2",
                ),
            ),
        )

        // When
        repository.downloadGroups("user-1")

        // Then
        assertEquals("Ana Paula", local.member("m-1")?.name)
        assertEquals("Xy7kQ2", local.member("m-1")?.inviteCode)
    }

    @Test
    fun `GIVEN the server answers a stored member without a code WHEN downloading THEN keeps the code it had`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(group(id = "group-1")),
                storedMembers = listOf(
                    member(
                        id = "m-1",
                        groupId = "group-1",
                        updatedAt = 5L,
                        inviteCode = "Xy7kQ2",
                    ),
                ),
                remoteGroups = listOf(groupDto(id = "group-1")),
                remoteMembers = listOf(
                    memberDto(
                        id = "m-1",
                        groupId = "group-1",
                        updatedAt = 9L,
                        inviteCode = null,
                    ),
                ),
            )

            // When
            repository.downloadGroups("user-1")

            // Then
            assertEquals("Xy7kQ2", local.member("m-1")?.inviteCode)
        }

    @Test
    fun `GIVEN a member someone joined as WHEN releasing them THEN asks the server to release that member`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            storedMembers = listOf(pierreMember, carlaJoined),
            profileOwnerUserId = "user-1",
            releasedMember = carlaReleased,
        )

        // When
        val result = repository.requestMemberRelease("m-2")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("m-2"), remote.releasedMemberIds)
    }

    @Test
    fun `GIVEN the server releases the member WHEN releasing them THEN stores the member as the server answered`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(group(id = "group-1")),
                storedMembers = listOf(pierreMember, carlaJoined),
                profileOwnerUserId = "user-1",
                releasedMember = carlaReleased,
            )

            // When
            repository.requestMemberRelease("m-2")

            // Then
            assertEquals(
                member(
                    id = "m-2",
                    groupId = "group-1",
                    name = "Carla",
                    userId = null,
                    isMe = false,
                    updatedAt = 9L,
                    pendingSync = false,
                    inviteCode = "NewCode9",
                ),
                local.member("m-2"),
            )
            assertEquals(pierreMember, local.member("m-1"))
        }

    @Test
    fun `GIVEN the server releases the member WHEN observing the groups THEN they show not joined with the new code`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(group(id = "group-1")),
                storedMembers = listOf(pierreMember, carlaJoined),
                profileOwnerUserId = "user-1",
                releasedMember = carlaReleased,
            )
            repository.requestMemberRelease("m-2")

            // When
            val carla = repository
                .observeGroups()
                .first()
                .single()
                .members
                .single { it.id == "m-2" }

            // Then
            assertFalse(carla.isMe)
            assertFalse(carla.hasJoined)
            assertEquals("NewCode9", carla.inviteCode)
            assertFalse(repository.observeHasPendingChanges().first())
        }

    @Test
    fun `GIVEN the member is no longer on the server WHEN releasing them THEN succeeds and changes nothing here`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(group(id = "group-1")),
                storedMembers = listOf(pierreMember, carlaJoined),
                profileOwnerUserId = "user-1",
                releasedMember = null,
            )

            // When
            val result = repository.requestMemberRelease("m-2")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf("m-2"), remote.releasedMemberIds)
            assertEquals(listOf(pierreMember, carlaJoined), local.members.value)
        }

    @Test
    fun `GIVEN no internet WHEN releasing a member THEN returns the error and changes nothing here`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            storedMembers = listOf(pierreMember, carlaJoined),
            profileOwnerUserId = "user-1",
            releaseError = IllegalStateException("offline"),
        )

        // When
        val result = repository.requestMemberRelease("m-2")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(listOf(pierreMember, carlaJoined), local.members.value)
    }

    @Test
    fun `GIVEN the server refuses to release this device user WHEN releasing THEN fails and they stay me`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            storedMembers = listOf(pierreMember, carlaJoined),
            profileOwnerUserId = "user-1",
            releaseError = IllegalStateException("cannot release yourself"),
        )

        // When
        val result = repository.requestMemberRelease("m-1")

        // Then
        assertTrue(result.isFailure)
        assertEquals(pierreMember, local.member("m-1"))
    }

    @Test
    fun `GIVEN no account on the device WHEN releasing a member THEN returns the error and changes nothing here`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        createdBy = null,
                        ownerUserId = null,
                        pendingSync = true,
                    ),
                ),
                storedMembers = listOf(carlaJoined),
                profileOwnerUserId = null,
                releasedMember = carlaReleased,
            )

            // When
            val result = repository.requestMemberRelease("m-2")

            // Then
            assertTrue(result.isFailure)
            assertEquals(listOf(carlaJoined), local.members.value)
        }

    private val pierreMember = member(
        id = "m-1",
        groupId = "group-1",
        name = "Pierre",
        userId = "user-1",
        isMe = true,
        updatedAt = 5L,
    )

    private val carlaJoined = member(
        id = "m-2",
        groupId = "group-1",
        name = "Carla",
        userId = "user-2",
        updatedAt = 5L,
        inviteCode = "OldCode1",
    )

    private val carlaReleased = memberDto(
        id = "m-2",
        groupId = "group-1",
        name = "Carla",
        userId = null,
        updatedAt = 9L,
        inviteCode = "NewCode9",
    )

    private val bruno = GroupMember(
        id = "m-new",
        name = "Bruno",
        avatarColor = AvatarColor.SAND,
        isMe = false,
        hasJoined = false,
        userId = null,
        inviteCode = null,
    )

    private fun removedMember(
        id: String,
        updatedAt: Long = 5L,
    ): GroupMemberEntity = member(
        id = id,
        groupId = "group-1",
        updatedAt = updatedAt,
        pendingSync = true,
        isRemoved = true,
    )

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
                userId = null,
                inviteCode = null,
            ),
            GroupMember(
                id = "new-2",
                name = "Ana",
                avatarColor = AvatarColor.GREEN,
                isMe = false,
                hasJoined = false,
                userId = null,
                inviteCode = null,
            ),
        ),
        isPending = true,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
    )

    @Test
    fun `GIVEN a group WHEN saving its default split THEN the device holds it pending with the incomes`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    updatedAt = 5L,
                ),
            ),
        )
        val split = GroupDefaultSplit(DefaultSplitMode.INCOME, mapOf("m-1" to 75L, "m-2" to 25L))

        // When
        val result = repository.updateDefaultSplit("group-1", split, mapOf("m-1" to 900_000L), updatedAt = 9L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            split,
            repository
                .observeGroups()
                .first()
                .single()
                .defaultSplit,
        )
        assertEquals(mapOf("m-1" to 900_000L), repository.getSplitIncomes("group-1").getOrThrow())
        assertTrue(local.group("group-1")!!.pendingSync)
    }

    @Test
    fun `GIVEN the device cannot write WHEN saving the default split THEN it fails`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            groupWriteError = IllegalStateException("disk full"),
        )

        // When
        val result = repository.updateDefaultSplit("group-1", null, emptyMap(), updatedAt = 9L)

        // Then
        assertTrue(result.isFailure)
    }

    private fun group(
        id: String,
        name: String = "Viagem Floripa",
        createdBy: String? = "user-1",
        updatedAt: Long = 1L,
        pendingSync: Boolean = false,
        ownerUserId: String? = "user-1",
        inviteCode: String? = null,
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
        inviteCode = inviteCode,
        defaultSplit = null,
        splitIncomes = null,
    )

    private fun member(
        id: String,
        groupId: String,
        name: String = "Ana",
        userId: String? = null,
        isMe: Boolean = false,
        updatedAt: Long = 1L,
        pendingSync: Boolean = false,
        isRemoved: Boolean = false,
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
        isRemoved = isRemoved,
        inviteCode = inviteCode,
    )

    private fun removedGroup(
        groupId: String,
        reason: String,
        removedBy: String?,
        removedByName: String?,
    ): RemovedGroupDto = RemovedGroupDto(
        groupId = groupId,
        groupName = "Viagem Floripa",
        reason = reason,
        removedBy = removedBy,
        removedByName = removedByName,
    )

    private fun groupDto(
        id: String,
        name: String = "Viagem Floripa",
        createdBy: String? = "user-1",
        updatedAt: Long = 1L,
        inviteCode: String? = null,
    ): GroupDto = GroupDto(
        id = id,
        createdBy = createdBy,
        name = name,
        type = "trip",
        color = "blue",
        currency = "BRL",
        addedAt = 1L,
        updatedAt = updatedAt,
        inviteCode = inviteCode,
        defaultSplit = null,
    )

    private fun memberDto(
        id: String,
        groupId: String,
        name: String = "Ana",
        userId: String? = null,
        updatedAt: Long = 1L,
        inviteCode: String? = null,
    ): GroupMemberDto = GroupMemberDto(
        id = id,
        groupId = groupId,
        userId = userId,
        name = name,
        avatarColor = "green",
        addedAt = 1L,
        updatedAt = updatedAt,
        inviteCode = inviteCode,
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
        serverInviteCodes: Map<String, String> = emptyMap(),
        memberWriteError: Throwable? = null,
        groupWriteError: Throwable? = null,
        deleteMembersError: Throwable? = null,
        serverMemberInviteCodes: Map<String, String> = emptyMap(),
        rotateInviteCodeError: Throwable? = null,
        releasedMember: GroupMemberDto? = null,
        releaseError: Throwable? = null,
        exitStatus: String = "left",
        exitError: Throwable? = null,
        removedGroups: List<RemovedGroupDto> = emptyList(),
        removedGroupsError: Throwable? = null,
    ) {
        local = FakeGroupLocalDataSource(storedGroups, storedMembers).also {
            it.insertError = insertError
            it.memberWriteError = memberWriteError
            it.groupWriteError = groupWriteError
        }
        remote = FakeGroupRemoteDataSource(
            groups = remoteGroups,
            members = remoteMembers,
            upsertGroupsError = upsertGroupsError,
            upsertMembersError = upsertMembersError,
            fetchError = fetchError,
        ).also {
            it.inviteCodes = serverInviteCodes
            it.memberInviteCodes = serverMemberInviteCodes
            it.deleteMembersError = deleteMembersError
            it.rotateInviteCodeError = rotateInviteCodeError
            it.releasedMember = releasedMember
            it.releaseError = releaseError
            it.exitStatus = exitStatus
            it.exitError = exitError
            it.removedGroups = removedGroups
            it.removedGroupsError = removedGroupsError
        }
        deletions = InMemoryGroupDeletionRepository()
        repository = GroupRepositoryImpl(
            local = local,
            remote = remote,
            profileLocal = FakeGroupOwnerProfileLocalDataSource(profileOwnerUserId),
            mapper = GroupMapperImpl(),
            deletions = deletions,
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

    // Why: the add, rename and remove of one member, which fail alike when the device can't write.
    var memberWriteError: Throwable? = null

    // Why: "Editar grupo" when the device can't write.
    var groupWriteError: Throwable? = null

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
        return members.value.filter { it.pendingSync && !it.isRemoved && it.groupId in owned }
    }

    override suspend fun getRemovedMemberIds(ownerUserId: String): List<String> {
        val owned = groups.value
            .filter { it.ownerUserId == ownerUserId }
            .map { it.id }
            .toSet()
        return members.value.filter { it.isRemoved && it.groupId in owned }.map { it.id }
    }

    override suspend fun insertMember(entity: GroupMemberEntity) {
        memberWriteError?.let { throw it }
        check(group(entity.groupId) != null) { "No group ${entity.groupId}" }
        put(entity)
    }

    override suspend fun renameMember(
        id: String,
        name: String,
        updatedAt: Long,
    ) {
        memberWriteError?.let { throw it }
        members.value = members.value.map {
            if (it.id == id && !it.isRemoved) {
                it.copy(
                    name = name,
                    updatedAt = updatedAt,
                    pendingSync = true,
                )
            } else {
                it
            }
        }
    }

    override suspend fun updateGroup(
        id: String,
        name: String,
        type: String,
        color: String,
        currency: String,
        updatedAt: Long,
    ) {
        groupWriteError?.let { throw it }
        groups.value = groups.value.map {
            if (it.id == id) {
                it.copy(
                    name = name,
                    type = type,
                    color = color,
                    currency = currency,
                    updatedAt = maxOf(updatedAt, it.updatedAt + 1),
                    pendingSync = true,
                )
            } else {
                it
            }
        }
    }

    override suspend fun updateDefaultSplit(
        id: String,
        defaultSplit: String?,
        splitIncomes: String?,
        updatedAt: Long,
    ) {
        groupWriteError?.let { throw it }
        groups.value = groups.value.map {
            if (it.id == id) {
                it.copy(
                    defaultSplit = defaultSplit,
                    splitIncomes = splitIncomes,
                    updatedAt = maxOf(updatedAt, it.updatedAt + 1),
                    pendingSync = true,
                )
            } else {
                it
            }
        }
    }

    override suspend fun getSplitIncomes(id: String): String? {
        groupWriteError?.let { throw it }
        return group(id)?.splitIncomes
    }

    override suspend fun removeMember(
        id: String,
        removedAt: Long,
    ) {
        memberWriteError?.let { throw it }
        members.value = members.value.map {
            if (it.id == id && !it.isMe && it.userId == null) {
                it.copy(
                    isRemoved = true,
                    updatedAt = removedAt,
                    pendingSync = true,
                )
            } else {
                it
            }
        }
    }

    override suspend fun deleteRemovedMembers(ids: List<String>) {
        members.value = members.value.filterNot { it.id in ids && it.isRemoved }
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
                put(
                    entity.copy(
                        pendingSync = false,
                        splitIncomes = current.splitIncomes,
                    ),
                )
        }
        entity.inviteCode?.let {
            updateInviteCode(
                id = entity.id,
                inviteCode = it,
            )
        }
    }

    override suspend fun updateInviteCode(
        id: String,
        inviteCode: String,
    ) {
        groups.value = groups.value.map { if (it.id == id) it.copy(inviteCode = inviteCode) else it }
    }

    override suspend fun applyRemoteMember(entity: GroupMemberEntity) {
        val current = member(entity.id)
        when {
            current == null -> put(entity)

            // Why: like updateMemberFromRemote, a newer row leaves the code alone; only a code the server sends replaces it.
            current.updatedAt < entity.updatedAt -> put(
                entity.copy(
                    pendingSync = false,
                    isRemoved = false,
                    inviteCode = current.inviteCode,
                ),
            )
        }
        entity.inviteCode?.let {
            updateMemberInviteCodes(mapOf(entity.id to it))
        }
    }

    override suspend fun updateMemberInviteCodes(codesById: Map<String, String>) {
        members.value = members.value.map { member ->
            codesById[member.id]?.let { member.copy(inviteCode = it) } ?: member
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
            .filter { (!it.pendingSync || it.inviteCode != null) && it.id !in serverGroupIds }
            .map { it.id }
            .toSet()
        val ownedIds = owned.map { it.id }.toSet()
        members.value = members.value.filterNot {
            it.groupId in goneGroups ||
                (it.groupId in ownedIds && it.groupId in serverGroupIds && !it.pendingSync && it.id !in serverMemberIds)
        }
        groups.value = groups.value.filterNot { it.id in goneGroups }
    }

    override suspend fun getUploadedGroupIds(ownerUserId: String): List<String> = groups.value
        .filter { it.ownerUserId == ownerUserId && (!it.pendingSync || it.inviteCode != null) }
        .map { it.id }

    override suspend fun deleteGroups(ids: List<String>) {
        members.value = members.value.filterNot { it.groupId in ids }
        groups.value = groups.value.filterNot { it.id in ids }
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
    val deletedMemberIds = mutableListOf<List<String>>()
    var deleteMembersError: Throwable? = null
    var onUpsertGroups: () -> Unit = {}
    val releasedMemberIds = mutableListOf<String>()
    var releasedMember: GroupMemberDto? = null
    var releaseError: Throwable? = null

    // Why: the code the server makes for each group id, which the upsert answers with.
    var inviteCodes: Map<String, String> = emptyMap()
    var memberInviteCodes: Map<String, String> = emptyMap()

    // Why: what leave_group and delete_group answer, and what removed_groups holds for this user.
    var exitStatus: String = "left"
    var exitError: Throwable? = null
    val leftGroupIds = mutableListOf<String>()
    val deletedGroupIds = mutableListOf<String>()
    var removedGroups: List<RemovedGroupDto> = emptyList()
    val askedRemovedGroupIds = mutableListOf<List<String>>()
    var removedGroupsError: Throwable? = null

    override suspend fun upsertGroups(groups: List<GroupDto>): List<GroupDto> {
        calls += "groups"
        onUpsertGroups()
        upsertGroupsError?.let { throw it }
        upsertedGroups += groups
        return groups.map { it.copy(inviteCode = inviteCodes[it.id]) }
    }

    override suspend fun upsertMembers(members: List<GroupMemberDto>): List<GroupMemberDto> {
        calls += "members"
        upsertMembersError?.let { throw it }
        upsertedMembers += members
        return members.map { it.copy(inviteCode = memberInviteCodes[it.id]) }
    }

    override suspend fun deleteMembers(ids: List<String>) {
        calls += "delete members"
        deleteMembersError?.let { throw it }
        deletedMemberIds += ids
    }

    override suspend fun fetchGroups(): List<GroupDto> {
        fetchError?.let { throw it }
        return groups
    }

    override suspend fun fetchMembers(): List<GroupMemberDto> {
        fetchError?.let { throw it }
        return members
    }

    override suspend fun fetchInvite(code: String): InviteDto? = error("unused")

    override suspend fun fetchPersonalInvite(params: PersonalInviteParamsDto): InviteDto? = error("unused")

    override suspend fun joinGroup(params: JoinGroupParamsDto): JoinGroupResultDto = error("unused")

    val rotatedInviteCodeGroupIds = mutableListOf<String>()

    // Why: the code the server makes for the next change of a group's link.
    var nextInviteCode = "newCode1"
    var rotateInviteCodeError: Throwable? = null

    override suspend fun rotateInviteCode(groupId: String): String {
        rotatedInviteCodeGroupIds += groupId
        rotateInviteCodeError?.let { throw it }
        return nextInviteCode
    }

    override suspend fun requestMemberRelease(params: ReleaseMemberParamsDto): GroupMemberDto? {
        calls += "release"
        releasedMemberIds += params.memberId
        releaseError?.let { throw it }
        return releasedMember
    }

    override suspend fun requestLeaveGroup(groupId: String): GroupExitResultDto {
        exitError?.let { throw it }
        leftGroupIds += groupId
        return GroupExitResultDto(exitStatus)
    }

    override suspend fun deleteGroup(groupId: String): GroupExitResultDto {
        exitError?.let { throw it }
        deletedGroupIds += groupId
        return GroupExitResultDto(exitStatus)
    }

    override suspend fun fetchRemovedGroups(groupIds: List<String>): List<RemovedGroupDto> {
        removedGroupsError?.let { throw it }
        askedRemovedGroupIds += groupIds
        return removedGroups.filter { it.groupId in groupIds }
    }
}
