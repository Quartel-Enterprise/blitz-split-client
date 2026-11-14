package com.quare.blitzsplit.core.groups.data

import com.quare.blitzsplit.core.groups.data.datasource.GroupLocalDataSource
import com.quare.blitzsplit.core.groups.data.datasource.GroupPhotoRemoteDataSource
import com.quare.blitzsplit.core.groups.data.datasource.GroupRemoteDataSource
import com.quare.blitzsplit.core.groups.data.dto.GroupDto
import com.quare.blitzsplit.core.groups.data.dto.GroupExitResultDto
import com.quare.blitzsplit.core.groups.data.dto.GroupMemberDto
import com.quare.blitzsplit.core.groups.data.dto.InviteDto
import com.quare.blitzsplit.core.groups.data.dto.JoinGroupParamsDto
import com.quare.blitzsplit.core.groups.data.dto.JoinGroupResultDto
import com.quare.blitzsplit.core.groups.data.dto.MemberPhotoDto
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
import com.quare.blitzsplit.core.groups.domain.model.InviteCodeTakenException
import com.quare.blitzsplit.core.groups.domain.model.SimplifyDebtsChange
import com.quare.blitzsplit.core.groups.domain.model.SimplifyDebtsPair
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.profile.data.datasource.ProfileLocalDataSource
import com.quare.blitzsplit.core.profile.data.mapper.ProfileMapperImpl
import com.quare.blitzsplit.core.provider.room.entity.GroupEntity
import com.quare.blitzsplit.core.provider.room.entity.GroupMemberEntity
import com.quare.blitzsplit.core.provider.room.entity.MemberPhotoEntity
import com.quare.blitzsplit.core.provider.room.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
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
    private lateinit var profileLocal: FakeGroupOwnerProfileLocalDataSource
    private lateinit var photoRemote: FakeGroupPhotoRemoteDataSource
    private lateinit var repository: GroupRepositoryImpl

    // Why: the length of each code the repository asked RandomCode for, in order.
    private val madeCodeLengths = mutableListOf<Int>()

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
    fun `GIVEN two groups WHEN hiding one THEN the groups show only the other with its members and photos`() = runTest {
        // Given
        prepareScenario(
            storedGroups = twoGroups,
            storedMembers = twoGroupsMembers,
            storedPhotos = listOf(brunoPhoto),
        )
        val before = repository.observeGroups().first()

        // When
        repository.hideGroup("group-1")

        // Then
        val groups = repository.observeGroups().first()
        assertEquals(listOf(before.single { it.id == "group-2" }), groups)
        assertEquals(listOf("Bruno"), groups.single().members.map { it.name })
        assertEquals(listOf("https://photos/bruno.jpg"), groups.single().members.map { it.photoUrl })
    }

    @Test
    fun `GIVEN someone watching the groups WHEN hiding one THEN it leaves the groups at once`() = runTest {
        // Given
        prepareScenario(
            storedGroups = twoGroups,
            storedMembers = twoGroupsMembers,
            storedPhotos = listOf(brunoPhoto),
        )
        val emissions = mutableListOf<List<Group>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeGroups().collect(emissions::add)
        }

        // When
        repository.hideGroup("group-1")

        // Then
        assertEquals(
            listOf(listOf("group-1", "group-2"), listOf("group-2")),
            emissions.map { groups -> groups.map { it.id } },
        )
    }

    @Test
    fun `GIVEN a group WHEN hiding it THEN its rows stay on the device`() = runTest {
        // Given
        prepareScenario(
            storedGroups = twoGroups,
            storedMembers = twoGroupsMembers,
            storedPhotos = listOf(brunoPhoto),
        )

        // When
        repository.hideGroup("group-1")

        // Then
        assertEquals("Viagem Floripa", local.group("group-1")?.name)
        assertEquals("Ana", local.member("m-1")?.name)
    }

    @Test
    fun `GIVEN a hidden group WHEN showing it again THEN the groups are exactly as they were`() = runTest {
        // Given
        prepareScenario(
            storedGroups = twoGroups,
            storedMembers = twoGroupsMembers,
            storedPhotos = listOf(brunoPhoto),
        )
        val before = repository.observeGroups().first()
        repository.hideGroup("group-1")

        // When
        repository.showGroup("group-1")

        // Then
        assertEquals(before, repository.observeGroups().first())
    }

    @Test
    fun `GIVEN someone watching the groups and a hidden group WHEN showing it again THEN it comes back at once`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = twoGroups,
                storedMembers = twoGroupsMembers,
                storedPhotos = listOf(brunoPhoto),
            )
            repository.hideGroup("group-1")
            val emissions = mutableListOf<List<Group>>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                repository.observeGroups().collect(emissions::add)
            }

            // When
            repository.showGroup("group-1")

            // Then
            assertEquals(
                listOf(listOf("group-2"), listOf("group-1", "group-2")),
                emissions.map { groups -> groups.map { it.id } },
            )
        }

    @Test
    fun `GIVEN a hidden group WHEN it changes on the device meanwhile THEN it stays hidden`() = runTest {
        // Given
        prepareScenario(
            storedGroups = twoGroups,
            storedMembers = twoGroupsMembers,
            storedPhotos = listOf(brunoPhoto),
        )
        repository.hideGroup("group-1")

        // When
        local.put(
            group(
                id = "group-1",
                name = "Viagem Floripa 2",
                updatedAt = 9L,
            ),
        )

        // Then
        assertEquals(listOf("group-2"), repository.observeGroups().first().map { it.id })
    }

    @Test
    fun `GIVEN two groups WHEN hiding one THEN the device still has groups`() = runTest {
        // Given
        prepareScenario(
            storedGroups = twoGroups,
            storedMembers = twoGroupsMembers,
            storedPhotos = listOf(brunoPhoto),
        )

        // When
        repository.hideGroup("group-1")

        // Then
        assertTrue(repository.observeHasGroups().first())
    }

    @Test
    fun `GIVEN only one group WHEN hiding it THEN the device has no groups`() = runTest {
        // Given
        prepareScenario(storedGroups = listOf(group(id = "group-1")))

        // When
        repository.hideGroup("group-1")

        // Then
        assertFalse(repository.observeHasGroups().first())
    }

    @Test
    fun `GIVEN someone watching whether there are groups WHEN hiding the only one THEN it turns false at once`() =
        runTest {
            // Given
            prepareScenario(storedGroups = listOf(group(id = "group-1")))
            val emissions = mutableListOf<Boolean>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                repository.observeHasGroups().collect(emissions::add)
            }

            // When
            repository.hideGroup("group-1")

            // Then
            assertEquals(listOf(true, false), emissions)
        }

    @Test
    fun `GIVEN the only group hidden WHEN showing it again THEN the device has groups again`() = runTest {
        // Given
        prepareScenario(storedGroups = listOf(group(id = "group-1")))
        repository.hideGroup("group-1")

        // When
        repository.showGroup("group-1")

        // Then
        assertTrue(repository.observeHasGroups().first())
    }

    @Test
    fun `GIVEN the only group hidden WHEN it changes on the device meanwhile THEN the device still has no groups`() =
        runTest {
            // Given
            prepareScenario(storedGroups = listOf(group(id = "group-1")))
            repository.hideGroup("group-1")

            // When
            local.put(
                group(
                    id = "group-1",
                    name = "Viagem Floripa 2",
                    updatedAt = 9L,
                ),
            )

            // Then
            assertFalse(repository.observeHasGroups().first())
        }

    @Test
    fun `GIVEN the only group hidden WHEN another group comes to the device THEN the device has groups again`() =
        runTest {
            // Given
            prepareScenario(storedGroups = listOf(group(id = "group-1")))
            repository.hideGroup("group-1")

            // When
            local.put(group(id = "group-2"))

            // Then
            assertTrue(repository.observeHasGroups().first())
        }

    @Test
    fun `GIVEN a hidden group WHEN signing out and a group with the same id comes back THEN it shows`() = runTest {
        // Given
        prepareScenario(storedGroups = listOf(group(id = "group-1")))
        repository.hideGroup("group-1")
        repository.clear()

        // When
        local.put(group(id = "group-1"))

        // Then
        assertEquals(listOf("group-1"), repository.observeGroups().first().map { it.id })
        assertTrue(repository.observeHasGroups().first())
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
                        failure = null,
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
    fun `GIVEN a group created offline with its code WHEN observing THEN shows that code before any sync`() = runTest {
        // Given
        prepareScenario(profileOwnerUserId = "user-1")
        repository.createGroup(newGroup.copy(inviteCode = "DevCode1"), createdAt = 1_000L)

        // When
        val groups = repository.observeGroups().first()

        // Then
        assertEquals("DevCode1", groups.single().inviteCode)
        assertEquals(false, local.group("group-new")?.isOnServer)
    }

    @Test
    fun `GIVEN a group created offline WHEN its first sync reaches a server that keeps the code THEN the link stays`() =
        runTest {
            // Given
            prepareScenario(
                profileOwnerUserId = "user-1",
                keepsSentInviteCodes = true,
            )
            repository.createGroup(newGroup.copy(inviteCode = "DevCode1"), createdAt = 1_000L)

            // When
            val result = repository.uploadPendingGroups("user-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(
                "DevCode1",
                remote.upsertedGroups
                    .single()
                    .single()
                    .inviteCode,
            )
            assertEquals(
                "DevCode1",
                repository
                    .observeGroups()
                    .first()
                    .single()
                    .inviteCode,
            )
            assertEquals(true, local.group("group-new")?.isOnServer)
            assertEquals(false, local.group("group-new")?.pendingSync)
        }

    @Test
    fun `GIVEN a new group with its code WHEN an older server answers with its own THEN the server code wins`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        pendingSync = true,
                        inviteCode = "DevCode1",
                        isOnServer = false,
                    ),
                ),
                serverInviteCodes = mapOf("group-1" to "SrvCode1"),
            )

            // When
            val result = repository.uploadPendingGroups("user-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(
                "DevCode1",
                remote.upsertedGroups
                    .single()
                    .single()
                    .inviteCode,
            )
            assertEquals("SrvCode1", local.group("group-1")?.inviteCode)
            assertEquals(true, local.group("group-1")?.isOnServer)
        }

    @Test
    fun `GIVEN a new group and one already on the server WHEN uploading THEN only the new one sends its code`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-new",
                        pendingSync = true,
                        inviteCode = "DevCode1",
                        isOnServer = false,
                    ),
                    group(
                        id = "group-up",
                        pendingSync = true,
                        inviteCode = "SrvCode9",
                        isOnServer = true,
                    ),
                ),
                serverInviteCodes = mapOf("group-up" to "SrvCode9"),
                keepsSentInviteCodes = true,
            )

            // When
            val result = repository.uploadPendingGroups("user-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(
                mapOf("group-new" to "DevCode1", "group-up" to null),
                remote.upsertedGroups.single().associate { it.id to it.inviteCode },
            )
            assertEquals("DevCode1", local.group("group-new")?.inviteCode)
            assertEquals("SrvCode9", local.group("group-up")?.inviteCode)
            assertEquals(true, local.group("group-new")?.isOnServer)
            assertEquals(true, local.group("group-up")?.isOnServer)
        }

    @Test
    fun `GIVEN the server leaves a new group out of its answer WHEN uploading THEN it waits for a download`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        pendingSync = true,
                        inviteCode = "DevCode1",
                        isOnServer = false,
                    ),
                ),
                keepsSentInviteCodes = true,
            )
            remote.refusedGroupIds = setOf("group-1")

            // When
            val result = repository.uploadPendingGroups("user-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(false, local.group("group-1")?.isOnServer)
            assertEquals("DevCode1", local.group("group-1")?.inviteCode)
        }

    @Test
    fun `GIVEN a new group edited while it goes up WHEN the upload ends THEN it is on the server and its edit waits`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        pendingSync = true,
                        inviteCode = "DevCode1",
                        isOnServer = false,
                    ),
                ),
                keepsSentInviteCodes = true,
            )
            remote.onUpsertGroups = {
                local.put(
                    checkNotNull(local.group("group-1")).copy(
                        name = "Jantar",
                        updatedAt = 9L,
                        pendingSync = true,
                    ),
                )
            }

            // When
            val result = repository.uploadPendingGroups("user-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(true, local.group("group-1")?.isOnServer)
            assertEquals(true, local.group("group-1")?.pendingSync)
        }

    @Test
    fun `GIVEN a new group edited during its first upload WHEN uploading next THEN the edit goes up without a code`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        pendingSync = true,
                        inviteCode = "DevCode1",
                        isOnServer = false,
                    ),
                ),
                keepsSentInviteCodes = true,
            )
            remote.onUpsertGroups = {
                local.put(
                    checkNotNull(local.group("group-1")).copy(
                        name = "Jantar",
                        updatedAt = 9L,
                        pendingSync = true,
                    ),
                )
            }
            repository.uploadPendingGroups("user-1")
            remote.onUpsertGroups = {}

            // When
            val result = repository.uploadPendingGroups("user-1")

            // Then
            assertTrue(result.isSuccess)
            val pushed = remote.upsertedGroups.last().single()
            assertEquals("Jantar", pushed.name)
            assertNull(pushed.inviteCode)
        }

    @Test
    fun `GIVEN another group has the code made here WHEN uploading THEN pushes again with a new code and keeps it`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        pendingSync = true,
                        inviteCode = "DevCode1",
                        isOnServer = false,
                    ),
                ),
                takenInviteCodes = setOf("DevCode1"),
                keepsSentInviteCodes = true,
                madeCodes = listOf("FreeCod1"),
            )

            // When
            val result = repository.uploadPendingGroups("user-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(
                listOf(listOf("DevCode1"), listOf("FreeCod1")),
                remote.upsertGroupsAttempts.map { attempt -> attempt.map { it.inviteCode } },
            )
            assertEquals(listOf(8), madeCodeLengths)
            assertEquals("FreeCod1", local.group("group-1")?.inviteCode)
            assertEquals(true, local.group("group-1")?.isOnServer)
            assertEquals(false, local.group("group-1")?.pendingSync)
        }

    @Test
    fun `GIVEN a taken code beside a group on the server WHEN uploading THEN every new group gets a new code`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-a",
                        pendingSync = true,
                        inviteCode = "DevCode1",
                        isOnServer = false,
                    ),
                    group(
                        id = "group-b",
                        pendingSync = true,
                        inviteCode = "DevCode2",
                        isOnServer = false,
                    ),
                    group(
                        id = "group-up",
                        pendingSync = true,
                        inviteCode = "SrvCode9",
                        isOnServer = true,
                    ),
                ),
                serverInviteCodes = mapOf("group-up" to "SrvCode9"),
                takenInviteCodes = setOf("DevCode1"),
                keepsSentInviteCodes = true,
                madeCodes = listOf("FreeCod1", "FreeCod2"),
            )

            // When
            val result = repository.uploadPendingGroups("user-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(
                mapOf("group-a" to "FreeCod1", "group-b" to "FreeCod2", "group-up" to null),
                remote.upsertGroupsAttempts.last().associate { it.id to it.inviteCode },
            )
            assertEquals("FreeCod1", local.group("group-a")?.inviteCode)
            assertEquals("FreeCod2", local.group("group-b")?.inviteCode)
            assertEquals("SrvCode9", local.group("group-up")?.inviteCode)
            assertEquals(listOf(true, true, true), local.groups.value.map { it.isOnServer })
        }

    @Test
    fun `GIVEN every code made here is taken WHEN uploading THEN fails after three tries with a fresh code kept`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        pendingSync = true,
                        inviteCode = "DevCode1",
                        isOnServer = false,
                    ),
                ),
                storedMembers = listOf(
                    member(
                        id = "m-1",
                        groupId = "group-1",
                        pendingSync = true,
                    ),
                ),
                takenInviteCodes = setOf("DevCode1", "FreeCod1", "FreeCod2", "FreeCod3"),
                madeCodes = listOf("FreeCod1", "FreeCod2", "FreeCod3"),
            )

            // When
            val result = repository.uploadPendingGroups("user-1")

            // Then
            assertIs<InviteCodeTakenException>(result.exceptionOrNull())
            assertEquals(3, remote.upsertGroupsAttempts.size)
            assertEquals("FreeCod3", local.group("group-1")?.inviteCode)
            assertEquals(false, local.group("group-1")?.isOnServer)
            assertEquals(true, local.group("group-1")?.pendingSync)
            assertTrue(remote.upsertedMembers.isEmpty())
            assertEquals(true, local.member("m-1")?.pendingSync)
        }

    @Test
    fun `GIVEN an old group without a code beside a taken one WHEN uploading THEN it takes the server code`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-new",
                        pendingSync = true,
                        inviteCode = "DevCode1",
                        isOnServer = false,
                    ),
                    group(
                        id = "group-old",
                        pendingSync = true,
                        inviteCode = null,
                        isOnServer = false,
                    ),
                ),
                serverInviteCodes = mapOf("group-old" to "SrvCode7"),
                takenInviteCodes = setOf("DevCode1"),
                keepsSentInviteCodes = true,
                madeCodes = listOf("FreeCod1", "FreeCod2"),
            )

            // When
            val result = repository.uploadPendingGroups("user-1")

            // Then
            assertTrue(result.isSuccess)
            assertTrue(
                remote.upsertGroupsAttempts.all { attempt ->
                    attempt.single { it.id == "group-old" }.inviteCode == null
                },
            )
            assertEquals("SrvCode7", local.group("group-old")?.inviteCode)
            assertEquals(true, local.group("group-old")?.isOnServer)
        }

    @Test
    fun `GIVEN the server fails the groups for another reason WHEN uploading THEN tries once and makes no new code`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        pendingSync = true,
                        inviteCode = "DevCode1",
                        isOnServer = false,
                    ),
                ),
                upsertGroupsError = IllegalStateException("server down"),
            )

            // When
            val result = repository.uploadPendingGroups("user-1")

            // Then
            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertEquals(1, remote.upsertGroupsAttempts.size)
            assertTrue(madeCodeLengths.isEmpty())
            assertEquals("DevCode1", local.group("group-1")?.inviteCode)
            assertEquals(false, local.group("group-1")?.isOnServer)
        }

    @Test
    fun `GIVEN a new group with its code and a photo WHEN uploading THEN the photo goes up after its row`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    pendingSync = true,
                    updatedAt = 5L,
                    inviteCode = "DevCode1",
                    isOnServer = false,
                    pendingPhoto = photoJpeg,
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
        var callsBeforeUpload = emptyList<String>()
        photoRemote.onUpload = { callsBeforeUpload = remote.calls.toList() }

        // When
        repository.uploadPendingGroups("user-1")

        // Then
        assertEquals(listOf("groups", "members"), callsBeforeUpload)
    }

    @Test
    fun `GIVEN a group on the server without a code and a new photo WHEN uploading THEN the photo goes up first`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        pendingSync = true,
                        updatedAt = 5L,
                        inviteCode = null,
                        isOnServer = true,
                        pendingPhoto = photoJpeg,
                    ),
                ),
            )
            var callsBeforeUpload = listOf("not uploaded")
            photoRemote.onUpload = { callsBeforeUpload = remote.calls.toList() }

            // When
            repository.uploadPendingGroups("user-1")

            // Then
            assertEquals(emptyList(), callsBeforeUpload)
        }

    @Test
    fun `GIVEN a new group with its code not up yet WHEN a download misses it THEN it stays with its members`() =
        runTest {
            // Given
            prepareScenario(profileOwnerUserId = "user-1")
            repository.createGroup(newGroup.copy(inviteCode = "DevCode1"), createdAt = 1_000L)

            // When
            val result = repository.downloadGroups("user-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals("DevCode1", local.group("group-new")?.inviteCode)
            assertEquals(true, local.group("group-new")?.pendingSync)
            assertEquals(listOf("new-1", "new-2"), local.members.value.map { it.id })
        }

    @Test
    fun `GIVEN a new group with its code not up yet WHEN downloading THEN asks for no removed group`() = runTest {
        // Given
        prepareScenario(profileOwnerUserId = "user-1")
        repository.createGroup(newGroup.copy(inviteCode = "DevCode1"), createdAt = 1_000L)

        // When
        repository.downloadGroups("user-1")

        // Then
        assertTrue(remote.askedRemovedGroupIds.isEmpty())
    }

    @Test
    fun `GIVEN a group created offline that went up WHEN a download misses it THEN it leaves the device`() = runTest {
        // Given
        prepareScenario(
            profileOwnerUserId = "user-1",
            keepsSentInviteCodes = true,
        )
        repository.createGroup(newGroup.copy(inviteCode = "DevCode1"), createdAt = 1_000L)
        repository.uploadPendingGroups("user-1")

        // When
        val result = repository.downloadGroups("user-1")

        // Then
        assertTrue(result.isSuccess)
        assertNull(local.group("group-new"))
        assertTrue(local.members.value.isEmpty())
        assertEquals(listOf(listOf("group-new")), remote.askedRemovedGroupIds)
    }

    @Test
    fun `GIVEN a group on the server edited here without a code WHEN a download misses it THEN it leaves`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    pendingSync = true,
                    inviteCode = null,
                    isOnServer = true,
                ),
            ),
        )

        // When
        val result = repository.downloadGroups("user-1")

        // Then
        assertTrue(result.isSuccess)
        assertNull(local.group("group-1"))
    }

    @Test
    fun `GIVEN a new group the server lists WHEN downloading THEN it is on the server with the server code`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        updatedAt = 5L,
                        pendingSync = true,
                        inviteCode = "DevCode1",
                        isOnServer = false,
                    ),
                ),
                remoteGroups = listOf(
                    groupDto(
                        id = "group-1",
                        updatedAt = 1L,
                        inviteCode = "SrvCode1",
                    ),
                ),
            )

            // When
            val result = repository.downloadGroups("user-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(true, local.group("group-1")?.isOnServer)
            assertEquals("SrvCode1", local.group("group-1")?.inviteCode)
            assertEquals(true, local.group("group-1")?.pendingSync)
        }

    @Test
    fun `GIVEN a group only the server has WHEN downloading THEN it is stored as on the server`() = runTest {
        // Given
        prepareScenario(
            remoteGroups = listOf(
                groupDto(
                    id = "group-1",
                    inviteCode = "SrvCode1",
                ),
            ),
        )

        // When
        val result = repository.downloadGroups("user-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(true, local.group("group-1")?.isOnServer)
    }

    @Test
    fun `GIVEN a new group here that the server has WHEN downloading only that group THEN it is on the server`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        updatedAt = 5L,
                        pendingSync = true,
                        inviteCode = "DevCode1",
                        isOnServer = false,
                    ),
                ),
                remoteGroups = listOf(
                    groupDto(
                        id = "group-1",
                        updatedAt = 1L,
                        inviteCode = "DevCode1",
                    ),
                ),
            )
            val write = repository
                .fetchGroupDownload(
                    userId = "user-1",
                    groupId = "group-1",
                ).getOrThrow()

            // When
            write()

            // Then
            assertEquals(true, local.group("group-1")?.isOnServer)
        }

    @Test
    fun `GIVEN a group only the server has WHEN downloading only that group THEN it is stored as on the server`() =
        runTest {
            // Given
            prepareScenario(
                remoteGroups = listOf(
                    groupDto(
                        id = "group-1",
                        inviteCode = "SrvCode1",
                    ),
                ),
            )
            val write = repository
                .fetchGroupDownload(
                    userId = "user-1",
                    groupId = "group-1",
                ).getOrThrow()

            // When
            write()

            // Then
            assertEquals(true, local.group("group-1")?.isOnServer)
            assertEquals("SrvCode1", local.group("group-1")?.inviteCode)
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
                shape = GroupShape.COOKIE,
                photo = null,
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
    fun `GIVEN a group of a newer app WHEN saving only its photo THEN keeps its type color and name as stored`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(id = "group-1").copy(
                        name = "Concert",
                        type = "concert",
                        color = "ultraviolet",
                        pendingSync = false,
                    ),
                ),
            )

            // When
            val result = repository.updateGroupPhoto(
                groupId = "group-1",
                photo = ProfilePhoto.Remote("https://example.com/photo.jpg"),
                updatedAt = 7_000L,
            )

            // Then
            assertTrue(result.isSuccess)
            val stored = local.group("group-1")
            assertEquals("Concert", stored?.name)
            assertEquals("concert", stored?.type)
            assertEquals("ultraviolet", stored?.color)
            assertEquals("https://example.com/photo.jpg", stored?.photoUrl)
            assertEquals(7_000L, stored?.updatedAt)
            assertTrue(stored?.pendingSync == true)
        }

    @Test
    fun `GIVEN a failing write WHEN saving only the photo THEN returns the failure`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            groupWriteError = IllegalStateException("room failed"),
        )

        // When
        val result = repository.updateGroupPhoto(
            groupId = "group-1",
            photo = null,
            updatedAt = 7_000L,
        )

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
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
            shape = GroupShape.COOKIE,
            photo = null,
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
                shape = GroupShape.COOKIE,
                photo = null,
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
            shape = GroupShape.COOKIE,
            photo = null,
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
            shape = GroupShape.COOKIE,
            photo = null,
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
            shape = GroupShape.COOKIE,
            photo = null,
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
        photoUrl = null,
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
        shape = GroupShape.SQUARE,
        photo = null,
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
                photoUrl = null,
            ),
            GroupMember(
                id = "new-2",
                name = "Ana",
                avatarColor = AvatarColor.GREEN,
                isMe = false,
                hasJoined = false,
                userId = null,
                inviteCode = null,
                photoUrl = null,
            ),
        ),
        isPending = true,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
        simplifiesDebts = false,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = null,
        simplifyDebtsConflictBy = null,
    )

    @Test
    fun `GIVEN a group WHEN saving its default split THEN the device holds it pending`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    updatedAt = 5L,
                ),
            ),
        )
        val split = GroupDefaultSplit(DefaultSplitMode.INCOME, mapOf("m-1" to 75L, "m-2" to 25L), emptyList())

        // When
        val result = repository.updateDefaultSplit("group-1", split, updatedAt = 9L)

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
        assertTrue(local.group("group-1")!!.pendingSync)
    }

    @Test
    fun `GIVEN a server split with a key this version lacks WHEN editing it THEN the push keeps the key`() = runTest {
        // Given
        val serverRow = Json.decodeFromString<GroupDto>(
            """
            {"id":"group-1","created_by":"user-1","name":"Apê 302","type":"home","color":"blue","currency":"BRL",
            "added_at":1,"updated_at":2,"invite_code":"abc","shape":"petal","photo_url":null,
            "default_split":{"mode":"shares","weights":[{"member_id":"m-1","weight":2}],
            "rounding":[{"category":"food","mode":"equal"}]},
            "simplify_debts":false,"simplify_debts_at":0,"simplify_debts_by":null,"simplify_debts_change":null}
            """.trimIndent(),
        )
        prepareScenario(remoteGroups = listOf(serverRow))
        repository.downloadGroups("user-1").getOrThrow()
        val split = GroupDefaultSplit(DefaultSplitMode.PERCENT, mapOf("m-1" to 60L, "m-2" to 40L), emptyList())

        // When
        repository.updateDefaultSplit("group-1", split, updatedAt = 9L).getOrThrow()
        repository.uploadPendingGroups("user-1").getOrThrow()

        // Then
        val pushed = remote.upsertedGroups
            .single()
            .single()
            .defaultSplit!!
        assertEquals(serverRow.defaultSplit!!["rounding"], pushed["rounding"])
        assertEquals("\"percent\"", pushed["mode"].toString())
        assertEquals(
            """[{"member_id":"m-1","weight":60},{"member_id":"m-2","weight":40}]""",
            pushed["weights"].toString(),
        )
    }

    @Test
    fun `GIVEN the device cannot write WHEN saving the default split THEN it fails`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            groupWriteError = IllegalStateException("disk full"),
        )

        // When
        val result = repository.updateDefaultSplit("group-1", null, updatedAt = 9L)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `GIVEN photos on the device WHEN downloading THEN the server photos take the place of every one`() = runTest {
        // Given
        prepareScenario(
            remoteGroups = listOf(groupDto(id = "group-1")),
            storedPhotos = listOf(
                MemberPhotoEntity(
                    userId = "user-2",
                    photoUrl = "https://photos/ana-old.jpg",
                ),
                MemberPhotoEntity(
                    userId = "user-3",
                    photoUrl = "https://photos/bruno.jpg",
                ),
            ),
            remoteMemberPhotos = listOf(
                MemberPhotoDto(
                    userId = "user-2",
                    photoUrl = "https://photos/ana-new.jpg",
                ),
                MemberPhotoDto(
                    userId = "user-4",
                    photoUrl = "https://photos/carla.jpg",
                ),
            ),
        )

        // When
        val result = repository.downloadGroups("user-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            listOf(
                MemberPhotoEntity(
                    userId = "user-2",
                    photoUrl = "https://photos/ana-new.jpg",
                ),
                MemberPhotoEntity(
                    userId = "user-4",
                    photoUrl = "https://photos/carla.jpg",
                ),
            ),
            local.photos.value,
        )
    }

    @Test
    fun `GIVEN a photo someone removed WHEN downloading THEN it leaves the device`() = runTest {
        // Given
        prepareScenario(
            remoteGroups = listOf(groupDto(id = "group-1")),
            storedPhotos = listOf(
                MemberPhotoEntity(
                    userId = "user-2",
                    photoUrl = "https://photos/ana.jpg",
                ),
            ),
            remoteMemberPhotos = listOf(
                MemberPhotoDto(
                    userId = "user-2",
                    photoUrl = null,
                ),
            ),
        )

        // When
        repository.downloadGroups("user-1")

        // Then
        assertTrue(local.photos.value.isEmpty())
    }

    @Test
    fun `GIVEN profiles with and without a photo WHEN downloading THEN only the ones with a photo are kept`() =
        runTest {
            // Given
            prepareScenario(
                remoteGroups = listOf(groupDto(id = "group-1")),
                remoteMemberPhotos = listOf(
                    MemberPhotoDto(
                        userId = "user-2",
                        photoUrl = "https://photos/ana.jpg",
                    ),
                    MemberPhotoDto(
                        userId = "user-3",
                        photoUrl = null,
                    ),
                ),
            )

            // When
            repository.downloadGroups("user-1")

            // Then
            assertEquals(
                listOf(
                    MemberPhotoEntity(
                        userId = "user-2",
                        photoUrl = "https://photos/ana.jpg",
                    ),
                ),
                local.photos.value,
            )
        }

    @Test
    fun `GIVEN a member with a photo on the server WHEN downloading THEN the group shows their photo`() = runTest {
        // Given
        prepareScenario(
            remoteGroups = listOf(groupDto(id = "group-1")),
            remoteMembers = listOf(
                memberDto(
                    id = "m-2",
                    groupId = "group-1",
                    userId = "user-2",
                    name = "Ana",
                ),
                memberDto(
                    id = "m-3",
                    groupId = "group-1",
                    userId = null,
                    name = "Bruno",
                ),
            ),
            remoteMemberPhotos = listOf(
                MemberPhotoDto(
                    userId = "user-2",
                    photoUrl = "https://photos/ana.jpg",
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
        assertEquals(listOf("https://photos/ana.jpg", null), members.map { it.photoUrl })
    }

    @Test
    fun `GIVEN the photos fail to load WHEN downloading THEN it succeeds and keeps the photos the device had`() =
        runTest {
            // Given
            val storedPhotos = listOf(
                MemberPhotoEntity(
                    userId = "user-2",
                    photoUrl = "https://photos/ana.jpg",
                ),
            )
            prepareScenario(
                remoteGroups = listOf(groupDto(id = "group-1")),
                remoteMembers = listOf(
                    memberDto(
                        id = "m-2",
                        groupId = "group-1",
                        userId = "user-2",
                    ),
                ),
                storedPhotos = storedPhotos,
                memberPhotosError = IllegalStateException("server down"),
            )

            // When
            val result = repository.downloadGroups("user-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(storedPhotos, local.photos.value)
            assertEquals(listOf("m-2"), local.members.value.map { it.id })
        }

    @Test
    fun `GIVEN the groups fail to load WHEN downloading THEN asks for no photo and keeps the ones it had`() = runTest {
        // Given
        val storedPhotos = listOf(
            MemberPhotoEntity(
                userId = "user-2",
                photoUrl = "https://photos/ana.jpg",
            ),
        )
        prepareScenario(
            fetchError = IllegalStateException("server down"),
            storedPhotos = storedPhotos,
        )

        // When
        val result = repository.downloadGroups("user-1")

        // Then
        assertTrue(result.isFailure)
        assertEquals(0, remote.photoFetches)
        assertEquals(storedPhotos, local.photos.value)
    }

    @Test
    fun `GIVEN photos on the device WHEN downloading the member photos THEN the server photos replace them`() =
        runTest {
            // Given
            prepareScenario(
                storedPhotos = listOf(
                    MemberPhotoEntity(
                        userId = "user-2",
                        photoUrl = "https://photos/ana-old.jpg",
                    ),
                    MemberPhotoEntity(
                        userId = "user-3",
                        photoUrl = "https://photos/bruno.jpg",
                    ),
                ),
                remoteMemberPhotos = listOf(
                    MemberPhotoDto(
                        userId = "user-2",
                        photoUrl = "https://photos/ana-new.jpg",
                    ),
                    MemberPhotoDto(
                        userId = "user-3",
                        photoUrl = null,
                    ),
                ),
            )

            // When
            val result = repository.downloadMemberPhotos()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(
                listOf(
                    MemberPhotoEntity(
                        userId = "user-2",
                        photoUrl = "https://photos/ana-new.jpg",
                    ),
                ),
                local.photos.value,
            )
        }

    @Test
    fun `GIVEN the photos fail to load WHEN downloading the member photos THEN it fails and keeps the stored ones`() =
        runTest {
            // Given
            val storedPhotos = listOf(
                MemberPhotoEntity(
                    userId = "user-2",
                    photoUrl = "https://photos/ana.jpg",
                ),
            )
            prepareScenario(
                storedPhotos = storedPhotos,
                memberPhotosError = IllegalStateException("server down"),
            )

            // When
            val result = repository.downloadMemberPhotos()

            // Then
            assertTrue(result.isFailure)
            assertEquals(storedPhotos, local.photos.value)
        }

    @Test
    fun `GIVEN the groups are observed WHEN a member photo changes on the device THEN the group shows the new photo`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(group(id = "group-1")),
                storedMembers = listOf(carlaJoined),
                storedPhotos = listOf(
                    MemberPhotoEntity(
                        userId = "user-2",
                        photoUrl = "https://photos/carla-old.jpg",
                    ),
                ),
            )
            val emissions = mutableListOf<List<Group>>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                repository.observeGroups().collect(emissions::add)
            }

            // When
            local.photos.value = listOf(
                MemberPhotoEntity(
                    userId = "user-2",
                    photoUrl = "https://photos/carla-new.jpg",
                ),
            )

            // Then
            assertEquals(
                listOf("https://photos/carla-old.jpg", "https://photos/carla-new.jpg"),
                emissions.map {
                    it
                        .single()
                        .members
                        .single()
                        .photoUrl
                },
            )
        }

    @Test
    fun `GIVEN the groups are observed WHEN the profile photo changes THEN this device user shows the new photo`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(group(id = "group-1")),
                storedMembers = listOf(pierreMember, carlaJoined),
                storedPhotos = listOf(
                    MemberPhotoEntity(
                        userId = "user-1",
                        photoUrl = "https://photos/pierre-synced.jpg",
                    ),
                ),
                myPhotoUrl = null,
            )
            val emissions = mutableListOf<List<Group>>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                repository.observeGroups().collect(emissions::add)
            }

            // When
            profileLocal.photoUrl.value = "https://photos/pierre-local.jpg"

            // Then
            assertEquals(
                listOf(listOf(null, null), listOf("https://photos/pierre-local.jpg", null)),
                emissions.map { groups -> groups.single().members.map { it.photoUrl } },
            )
        }

    @Test
    fun `GIVEN a photo picked offline WHEN observing the groups THEN this device user shows it as a data URI`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(group(id = "group-1")),
                storedMembers = listOf(pierreMember),
                myPhotoUrl = "https://photos/pierre-old.jpg",
            )
            profileLocal.pendingPhoto = byteArrayOf(1, 2, 3)

            // When
            val groups = repository.observeGroups().first()

            // Then
            assertEquals(
                "data:image/jpeg;base64,AQID",
                groups
                    .single()
                    .members
                    .single()
                    .photoUrl,
            )
        }

    @Test
    fun `GIVEN photos on the device WHEN clearing THEN the device has none`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            storedMembers = listOf(carlaJoined),
            storedPhotos = listOf(
                MemberPhotoEntity(
                    userId = "user-2",
                    photoUrl = "https://photos/carla.jpg",
                ),
            ),
        )

        // When
        repository.clear()

        // Then
        assertTrue(local.photos.value.isEmpty())
    }

    @Test
    fun `GIVEN groups on the server WHEN listing the first download THEN returns their ids in the server order`() =
        runTest {
            // Given
            prepareScenario(
                remoteGroups = listOf(
                    groupDto(id = "group-recent"),
                    groupDto(id = "group-middle"),
                    groupDto(id = "group-old"),
                ),
            )

            // When
            val result = repository.fetchFirstDownloadGroupIds()

            // Then
            assertEquals(listOf("group-recent", "group-middle", "group-old"), result.getOrNull())
        }

    @Test
    fun `GIVEN the server fails WHEN listing the first download THEN returns the error`() = runTest {
        // Given
        prepareScenario(
            remoteGroups = listOf(groupDto(id = "group-1")),
            fetchError = IllegalStateException("server down"),
        )

        // When
        val result = repository.fetchFirstDownloadGroupIds()

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN a group on the server WHEN only fetching it THEN nothing reaches the device until its write runs`() =
        runTest {
            // Given
            prepareScenario(
                remoteGroups = listOf(groupDto(id = "group-1")),
                remoteMembers = listOf(
                    memberDto(
                        id = "m-1",
                        groupId = "group-1",
                        userId = "user-1",
                        name = "Pierre",
                    ),
                ),
            )

            // When
            val result = repository.fetchGroupDownload(
                userId = "user-1",
                groupId = "group-1",
            )

            // Then
            assertTrue(result.isSuccess)
            assertNull(local.group("group-1"))
            assertNull(local.member("m-1"))
        }

    @Test
    fun `GIVEN a group with members on the server WHEN downloading it THEN stores them synced and owned by the user`() =
        runTest {
            // Given
            prepareScenario(
                remoteGroups = listOf(
                    groupDto(
                        id = "group-1",
                        createdBy = "user-2",
                        inviteCode = "abc123",
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
            val result = repository.fetchGroupDownload(
                userId = "user-1",
                groupId = "group-1",
            )
            result.getOrNull()?.invoke()

            // Then
            assertTrue(result.isSuccess)
            val stored = local.group("group-1")
            assertEquals("user-1", stored?.ownerUserId)
            assertEquals("user-2", stored?.createdBy)
            assertEquals(false, stored?.pendingSync)
            assertEquals("abc123", stored?.inviteCode)
            assertEquals(listOf(false, true, false), listOf("m-1", "m-2", "m-3").map { local.member(it)?.isMe })
            assertFalse(repository.observeHasPendingChanges().first())
        }

    @Test
    fun `GIVEN members on the server WHEN downloading their group THEN only those with an account have joined`() =
        runTest {
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
            repository
                .fetchGroupDownload(
                    userId = "user-1",
                    groupId = "group-1",
                ).getOrThrow()()

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
    fun `GIVEN two groups on the server WHEN downloading one of them THEN only it and its members come down`() =
        runTest {
            // Given
            prepareScenario(
                remoteGroups = listOf(
                    groupDto(id = "group-1"),
                    groupDto(id = "group-2"),
                ),
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
            repository
                .fetchGroupDownload(
                    userId = "user-1",
                    groupId = "group-1",
                ).getOrThrow()()

            // Then
            assertEquals(listOf("group-1"), local.groups.value.map { it.id })
            assertEquals(listOf("m-1"), local.members.value.map { it.id })
        }

    @Test
    fun `GIVEN synced rows the server no longer has WHEN downloading one group THEN deletes none of them`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(id = "group-1"),
                group(id = "group-gone"),
                group(
                    id = "group-other-user",
                    ownerUserId = "user-2",
                ),
            ),
            storedMembers = listOf(
                member(
                    id = "m-gone-from-group-1",
                    groupId = "group-1",
                ),
                member(
                    id = "m-of-gone-group",
                    groupId = "group-gone",
                ),
                member(
                    id = "m-of-other-user",
                    groupId = "group-other-user",
                ),
            ),
            remoteGroups = listOf(groupDto(id = "group-1")),
            remoteMembers = listOf(
                memberDto(
                    id = "m-1",
                    groupId = "group-1",
                ),
            ),
        )

        // When
        repository
            .fetchGroupDownload(
                userId = "user-1",
                groupId = "group-1",
            ).getOrThrow()()

        // Then
        assertEquals(
            setOf("group-1", "group-gone", "group-other-user"),
            local.groups.value
                .map { it.id }
                .toSet(),
        )
        assertEquals(
            setOf("m-gone-from-group-1", "m-of-gone-group", "m-of-other-user", "m-1"),
            local.members.value
                .map { it.id }
                .toSet(),
        )
        assertTrue(deletions.observeDeletions().first().isEmpty())
    }

    @Test
    fun `GIVEN a newer server copy of a stored group WHEN downloading that group THEN the server copy wins`() =
        runTest {
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
            repository
                .fetchGroupDownload(
                    userId = "user-1",
                    groupId = "group-1",
                ).getOrThrow()()

            // Then
            val stored = local.group("group-1")
            assertEquals("Viagem Floripa", stored?.name)
            assertEquals(9L, stored?.updatedAt)
            assertEquals(false, stored?.pendingSync)
        }

    @Test
    fun `GIVEN an older server copy of a stored group WHEN downloading that group THEN keeps the device change`() =
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
            repository
                .fetchGroupDownload(
                    userId = "user-1",
                    groupId = "group-1",
                ).getOrThrow()()

            // Then
            val stored = local.group("group-1")
            assertEquals("Viagem Floripa", stored?.name)
            assertEquals(9L, stored?.updatedAt)
            assertEquals(true, stored?.pendingSync)
        }

    @Test
    fun `GIVEN a newer server copy of a stored member WHEN downloading its group THEN the server copy wins`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(group(id = "group-1")),
                storedMembers = listOf(
                    member(
                        id = "m-1",
                        groupId = "group-1",
                        name = "Ana",
                        updatedAt = 5L,
                        pendingSync = true,
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
            repository
                .fetchGroupDownload(
                    userId = "user-1",
                    groupId = "group-1",
                ).getOrThrow()()

            // Then
            val stored = local.member("m-1")
            assertEquals("Ana Paula", stored?.name)
            assertEquals("user-2", stored?.userId)
            assertEquals(false, stored?.pendingSync)
        }

    @Test
    fun `GIVEN an older server copy of a stored member WHEN downloading its group THEN keeps the device change`() =
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
                    ),
                ),
            )

            // When
            repository
                .fetchGroupDownload(
                    userId = "user-1",
                    groupId = "group-1",
                ).getOrThrow()()

            // Then
            assertEquals("Ana Paula", local.member("m-1")?.name)
            assertEquals(true, local.member("m-1")?.pendingSync)
        }

    @Test
    fun `GIVEN a group deleted after the list was read WHEN downloading it THEN succeeds and stores nothing`() =
        runTest {
            // Given
            prepareScenario(
                remoteGroups = listOf(groupDto(id = "group-1")),
                remoteMembers = listOf(
                    memberDto(
                        id = "m-gone",
                        groupId = "group-gone",
                    ),
                ),
            )

            // When
            val result = repository.fetchGroupDownload(
                userId = "user-1",
                groupId = "group-gone",
            )
            result.getOrNull()?.invoke()

            // Then
            assertTrue(result.isSuccess)
            assertTrue(local.groups.value.isEmpty())
            assertTrue(local.members.value.isEmpty())
        }

    @Test
    fun `GIVEN the server fails WHEN downloading one group THEN returns the error and stores nothing`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    name = "Viagem",
                ),
            ),
            remoteGroups = listOf(
                groupDto(
                    id = "group-1",
                    name = "Viagem Floripa",
                    updatedAt = 9L,
                ),
            ),
            remoteMembers = listOf(
                memberDto(
                    id = "m-1",
                    groupId = "group-1",
                ),
            ),
            fetchError = IllegalStateException("server down"),
        )

        // When
        val result = repository.fetchGroupDownload(
            userId = "user-1",
            groupId = "group-1",
        )
        result.getOrNull()?.invoke()

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals("Viagem", local.group("group-1")?.name)
        assertTrue(local.members.value.isEmpty())
    }

    @Test
    fun `GIVEN a stored group WHEN editing its shape THEN stores the shape code`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    shape = "square",
                ),
            ),
        )

        // When
        editGroup(shape = GroupShape.PETAL)

        // Then
        assertEquals("petal", local.group("group-1")?.shape)
    }

    @Test
    fun `GIVEN a server photo WHEN picking a new photo THEN keeps it on the device without the old URL`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    photoUrl = oldPhotoUrl,
                ),
            ),
        )

        // When
        editGroup(photo = ProfilePhoto.Pending(photoJpeg))

        // Then
        val stored = local.group("group-1")
        assertTrue(photoJpeg.contentEquals(stored?.pendingPhoto))
        assertNull(stored?.photoUrl)
    }

    @Test
    fun `GIVEN a photo not up yet WHEN saving a server photo THEN keeps the URL and drops the pending one`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    pendingPhoto = photoJpeg,
                ),
            ),
        )

        // When
        editGroup(photo = ProfilePhoto.Remote(oldPhotoUrl))

        // Then
        val stored = local.group("group-1")
        assertEquals(oldPhotoUrl, stored?.photoUrl)
        assertNull(stored?.pendingPhoto)
    }

    @Test
    fun `GIVEN a group with a photo WHEN removing the photo THEN clears both the URL and the pending one`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    photoUrl = oldPhotoUrl,
                    pendingPhoto = photoJpeg,
                ),
            ),
        )

        // When
        editGroup(photo = null)

        // Then
        val stored = local.group("group-1")
        assertNull(stored?.photoUrl)
        assertNull(stored?.pendingPhoto)
    }

    @Test
    fun `GIVEN a photo picked offline WHEN editing the group THEN the group shows it pending and uploads nothing`() =
        runTest {
            // Given
            prepareScenario(storedGroups = listOf(group(id = "group-1")))

            // When
            editGroup(photo = ProfilePhoto.Pending(photoJpeg))

            // Then
            val shown = repository.observeGroups().first().single()
            assertEquals(ProfilePhoto.Pending(photoJpeg), shown.photo)
            assertTrue(shown.isPending)
            assertTrue(photoRemote.uploads.isEmpty())
        }

    @Test
    fun `GIVEN a photo not up yet WHEN uploading THEN pushes the row with the URL the photo will have`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    pendingSync = true,
                    updatedAt = 5L,
                    pendingPhoto = photoJpeg,
                ),
            ),
        )

        // When
        val result = repository.uploadPendingGroups("user-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            "https://cdn.test/storage/v1/object/public/group-photos/group-1/photo.jpg?v=5",
            remote.upsertedGroups
                .single()
                .single()
                .photoUrl,
        )
    }

    @Test
    fun `GIVEN a photo not up yet WHEN uploading THEN uploads that photo for the group`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    pendingSync = true,
                    updatedAt = 5L,
                    pendingPhoto = photoJpeg,
                ),
            ),
        )

        // When
        repository.uploadPendingGroups("user-1")

        // Then
        val (groupId, jpeg) = photoRemote.uploads.single()
        assertEquals("group-1", groupId)
        assertTrue(photoJpeg.contentEquals(jpeg))
    }

    @Test
    fun `GIVEN a photo not up yet WHEN uploading THEN it goes up after the group and its members`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    pendingSync = true,
                    updatedAt = 5L,
                    pendingPhoto = photoJpeg,
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
        var callsBeforeUpload = emptyList<String>()
        photoRemote.onUpload = { callsBeforeUpload = remote.calls.toList() }

        // When
        repository.uploadPendingGroups("user-1")

        // Then
        assertEquals(listOf("groups", "members"), callsBeforeUpload)
    }

    @Test
    fun `GIVEN a group already on the server with a new photo WHEN uploading THEN the photo goes up before its row`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        pendingSync = true,
                        updatedAt = 5L,
                        inviteCode = "ABC123",
                        pendingPhoto = photoJpeg,
                    ),
                ),
            )
            var callsBeforeUpload = listOf("not uploaded")
            photoRemote.onUpload = { callsBeforeUpload = remote.calls.toList() }

            // When
            repository.uploadPendingGroups("user-1")

            // Then
            assertEquals(emptyList(), callsBeforeUpload)
            assertEquals(
                "https://cdn.test/storage/v1/object/public/group-photos/group-1/photo.jpg?v=5",
                remote.upsertedGroups
                    .single()
                    .single()
                    .photoUrl,
            )
        }

    @Test
    fun `GIVEN a group on the server whose photo fails WHEN uploading THEN only that group waits for the next round`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        pendingSync = true,
                        updatedAt = 5L,
                        inviteCode = "ABC123",
                        pendingPhoto = photoJpeg,
                    ),
                    group(
                        id = "group-2",
                        pendingSync = true,
                        inviteCode = "DEF456",
                    ),
                ),
            )
            photoRemote.uploadError = IllegalStateException("storage down")

            // When
            val result = repository.uploadPendingGroups("user-1")

            // Then
            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertEquals(listOf("group-2"), remote.upsertedGroups.single().map { it.id })
            assertEquals(true, local.group("group-1")?.pendingSync)
            assertEquals(false, local.group("group-2")?.pendingSync)
        }

    @Test
    fun `GIVEN a group on the server whose photo is refused WHEN uploading THEN it goes up without the photo`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        pendingSync = true,
                        updatedAt = 5L,
                        inviteCode = "ABC123",
                        pendingPhoto = photoJpeg,
                    ),
                ),
            )
            photoRemote.refusesUploads = true

            // When
            val result = repository.uploadPendingGroups("user-1")

            // Then
            assertTrue(result.isSuccess)
            assertNull(
                remote.upsertedGroups
                    .single()
                    .single()
                    .photoUrl,
            )
            val stored = local.group("group-1")
            assertNull(stored?.pendingPhoto)
            assertEquals(false, stored?.pendingSync)
        }

    @Test
    fun `GIVEN a new group whose photo is refused WHEN uploading again THEN its row goes up without the photo`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        pendingSync = true,
                        updatedAt = 5L,
                        pendingPhoto = photoJpeg,
                    ),
                ),
            )
            photoRemote.refusesUploads = true
            repository.uploadPendingGroups("user-1")

            // When
            val result = repository.uploadPendingGroups("user-1")

            // Then
            assertTrue(result.isSuccess)
            assertNull(
                remote.upsertedGroups
                    .last()
                    .single()
                    .photoUrl,
            )
            assertEquals(false, local.group("group-1")?.pendingSync)
        }

    @Test
    fun `GIVEN the group edited while its photo goes up WHEN the upload ends THEN the photo still takes its URL`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        pendingSync = true,
                        updatedAt = 5L,
                        pendingPhoto = photoJpeg,
                    ),
                ),
            )
            photoRemote.onUpload = {
                editGroup(
                    shape = GroupShape.CIRCLE,
                    photo = ProfilePhoto.Pending(photoJpeg),
                )
            }

            // When
            repository.uploadPendingGroups("user-1")

            // Then
            val stored = local.group("group-1")
            assertEquals(
                "https://cdn.test/storage/v1/object/public/group-photos/group-1/photo.jpg?v=5",
                stored?.photoUrl,
            )
            assertNull(stored?.pendingPhoto)
            assertEquals(true, stored?.pendingSync)
        }

    @Test
    fun `GIVEN a photo uploaded WHEN the upload ends THEN the device keeps its URL drops the bytes and is synced`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        pendingSync = true,
                        updatedAt = 5L,
                        pendingPhoto = photoJpeg,
                    ),
                ),
            )

            // When
            repository.uploadPendingGroups("user-1")

            // Then
            val stored = local.group("group-1")
            assertEquals(
                "https://cdn.test/storage/v1/object/public/group-photos/group-1/photo.jpg?v=5",
                stored?.photoUrl,
            )
            assertNull(stored?.pendingPhoto)
            assertEquals(false, stored?.pendingSync)
        }

    @Test
    fun `GIVEN a photo uploaded WHEN observing the groups THEN the group shows the server photo`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    pendingSync = true,
                    updatedAt = 5L,
                    pendingPhoto = photoJpeg,
                ),
            ),
        )
        repository.uploadPendingGroups("user-1")

        // When
        val shown = repository.observeGroups().first().single()

        // Then
        assertEquals(
            ProfilePhoto.Remote("https://cdn.test/storage/v1/object/public/group-photos/group-1/photo.jpg?v=5"),
            shown.photo,
        )
    }

    @Test
    fun `GIVEN a pending group with a server photo WHEN uploading THEN pushes its URL and uploads nothing`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    pendingSync = true,
                    photoUrl = oldPhotoUrl,
                ),
            ),
        )

        // When
        repository.uploadPendingGroups("user-1")

        // Then
        assertEquals(
            oldPhotoUrl,
            remote.upsertedGroups
                .single()
                .single()
                .photoUrl,
        )
        assertTrue(photoRemote.uploads.isEmpty())
        assertEquals(oldPhotoUrl, local.group("group-1")?.photoUrl)
    }

    @Test
    fun `GIVEN a pending group without a photo WHEN uploading THEN pushes no photo and uploads nothing`() = runTest {
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
        repository.uploadPendingGroups("user-1")

        // Then
        assertNull(
            remote.upsertedGroups
                .single()
                .single()
                .photoUrl,
        )
        assertTrue(photoRemote.uploads.isEmpty())
    }

    @Test
    fun `GIVEN a pending group with a shape WHEN uploading THEN pushes the shape code as stored`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    pendingSync = true,
                    shape = "cookie",
                ),
            ),
        )

        // When
        repository.uploadPendingGroups("user-1")

        // Then
        assertEquals(
            "cookie",
            remote.upsertedGroups
                .single()
                .single()
                .shape,
        )
    }

    @Test
    fun `GIVEN a shape a newer app wrote WHEN uploading THEN pushes that code untouched`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    pendingSync = true,
                    shape = "star",
                ),
            ),
        )

        // When
        repository.uploadPendingGroups("user-1")

        // Then
        assertEquals(
            "star",
            remote.upsertedGroups
                .single()
                .single()
                .shape,
        )
    }

    @Test
    fun `GIVEN a pending group made before shapes WHEN uploading THEN pushes no shape`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    pendingSync = true,
                    shape = null,
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
                .shape,
        )
    }

    @Test
    fun `GIVEN the photo upload fails WHEN uploading THEN returns the error and the photo stays pending`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    pendingSync = true,
                    updatedAt = 5L,
                    pendingPhoto = photoJpeg,
                ),
            ),
        )
        photoRemote.uploadError = IllegalStateException("storage down")

        // When
        val result = repository.uploadPendingGroups("user-1")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        val stored = local.group("group-1")
        assertEquals(true, stored?.pendingSync)
        assertTrue(photoJpeg.contentEquals(stored?.pendingPhoto))
        assertNull(stored?.photoUrl)
    }

    @Test
    fun `GIVEN the photo upload failed once WHEN uploading again THEN uploads the photo then`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    pendingSync = true,
                    updatedAt = 5L,
                    pendingPhoto = photoJpeg,
                ),
            ),
        )
        photoRemote.uploadError = IllegalStateException("storage down")
        repository.uploadPendingGroups("user-1")
        photoRemote.uploadError = null

        // When
        val result = repository.uploadPendingGroups("user-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("group-1"), photoRemote.uploads.map { it.first })
        assertNull(local.group("group-1")?.pendingPhoto)
        assertEquals(false, local.group("group-1")?.pendingSync)
    }

    @Test
    fun `GIVEN the server fails the groups WHEN uploading a group with a photo THEN uploads no photo`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    pendingSync = true,
                    pendingPhoto = photoJpeg,
                ),
            ),
            upsertGroupsError = IllegalStateException("offline"),
        )

        // When
        repository.uploadPendingGroups("user-1")

        // Then
        assertTrue(photoRemote.uploads.isEmpty())
        assertTrue(photoJpeg.contentEquals(local.group("group-1")?.pendingPhoto))
    }

    @Test
    fun `GIVEN another photo picked while the first goes up WHEN uploading THEN keeps the new photo pending`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        pendingSync = true,
                        updatedAt = 5L,
                        pendingPhoto = photoJpeg,
                    ),
                ),
            )
            photoRemote.onUpload = { editGroup(photo = ProfilePhoto.Pending(newPhotoJpeg)) }

            // When
            repository.uploadPendingGroups("user-1")

            // Then
            val stored = local.group("group-1")
            assertTrue(newPhotoJpeg.contentEquals(stored?.pendingPhoto))
            assertNull(stored?.photoUrl)
            assertEquals(true, stored?.pendingSync)
        }

    @Test
    fun `GIVEN another photo picked during the upload WHEN uploading next THEN pushes it under its own version`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        pendingSync = true,
                        updatedAt = 5L,
                        pendingPhoto = photoJpeg,
                    ),
                ),
            )
            photoRemote.onUpload = { editGroup(photo = ProfilePhoto.Pending(newPhotoJpeg)) }
            repository.uploadPendingGroups("user-1")
            photoRemote.onUpload = {}
            val version = local.group("group-1")!!.updatedAt

            // When
            repository.uploadPendingGroups("user-1")

            // Then
            assertEquals(
                "https://cdn.test/storage/v1/object/public/group-photos/group-1/photo.jpg?v=$version",
                remote.upsertedGroups
                    .last()
                    .single()
                    .photoUrl,
            )
            assertTrue(newPhotoJpeg.contentEquals(photoRemote.uploads.last().second))
            assertEquals(
                "https://cdn.test/storage/v1/object/public/group-photos/group-1/photo.jpg?v=$version",
                local.group("group-1")?.photoUrl,
            )
            assertNull(local.group("group-1")?.pendingPhoto)
        }

    @Test
    fun `GIVEN the photo is taken off while it goes up WHEN uploading THEN the group keeps no photo`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    pendingSync = true,
                    updatedAt = 5L,
                    pendingPhoto = photoJpeg,
                ),
            ),
        )
        photoRemote.onUpload = { editGroup(photo = null) }

        // When
        repository.uploadPendingGroups("user-1")

        // Then
        val stored = local.group("group-1")
        assertNull(stored?.photoUrl)
        assertNull(stored?.pendingPhoto)
        assertEquals(true, stored?.pendingSync)
    }

    @Test
    fun `GIVEN a photo already uploaded WHEN uploading again THEN uploads nothing`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    pendingSync = true,
                    updatedAt = 5L,
                    pendingPhoto = photoJpeg,
                ),
            ),
        )
        repository.uploadPendingGroups("user-1")

        // When
        repository.uploadPendingGroups("user-1")

        // Then
        assertEquals(1, photoRemote.uploads.size)
    }

    @Test
    fun `GIVEN a newer server copy with a shape and photo WHEN downloading THEN they replace the device ones`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        updatedAt = 5L,
                        shape = "cookie",
                        photoUrl = oldPhotoUrl,
                    ),
                ),
                remoteGroups = listOf(
                    groupDto(
                        id = "group-1",
                        updatedAt = 9L,
                        shape = "circle",
                        photoUrl = serverPhotoUrl,
                    ),
                ),
            )

            // When
            repository.downloadGroups("user-1")

            // Then
            val stored = local.group("group-1")
            assertEquals("circle", stored?.shape)
            assertEquals(serverPhotoUrl, stored?.photoUrl)
        }

    @Test
    fun `GIVEN a newer server copy without a photo WHEN downloading THEN the device photo is gone`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    updatedAt = 5L,
                    photoUrl = oldPhotoUrl,
                ),
            ),
            remoteGroups = listOf(
                groupDto(
                    id = "group-1",
                    updatedAt = 9L,
                    photoUrl = null,
                ),
            ),
        )

        // When
        repository.downloadGroups("user-1")

        // Then
        assertNull(local.group("group-1")?.photoUrl)
    }

    @Test
    fun `GIVEN a photo not up yet WHEN a newer server copy comes down THEN shows the server photo instead`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    updatedAt = 5L,
                    pendingSync = true,
                    pendingPhoto = photoJpeg,
                ),
            ),
            remoteGroups = listOf(
                groupDto(
                    id = "group-1",
                    updatedAt = 9L,
                    photoUrl = serverPhotoUrl,
                ),
            ),
        )

        // When
        repository.downloadGroups("user-1")

        // Then
        val shown = repository.observeGroups().first().single()
        assertEquals(ProfilePhoto.Remote(serverPhotoUrl), shown.photo)
        assertNull(local.group("group-1")?.pendingPhoto)
        assertFalse(shown.isPending)
    }

    @Test
    fun `GIVEN a photo and shape picked here WHEN an older server copy comes down THEN keeps them pending`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    updatedAt = 9L,
                    pendingSync = true,
                    shape = "petal",
                    pendingPhoto = photoJpeg,
                ),
            ),
            remoteGroups = listOf(
                groupDto(
                    id = "group-1",
                    updatedAt = 5L,
                    shape = "circle",
                    photoUrl = serverPhotoUrl,
                ),
            ),
        )

        // When
        repository.downloadGroups("user-1")

        // Then
        val stored = local.group("group-1")
        assertEquals("petal", stored?.shape)
        assertTrue(photoJpeg.contentEquals(stored?.pendingPhoto))
        assertNull(stored?.photoUrl)
        assertEquals(true, stored?.pendingSync)
    }

    @Test
    fun `GIVEN a group from the server without a shape WHEN downloading THEN it shows as a square`() = runTest {
        // Given
        prepareScenario(
            remoteGroups = listOf(
                groupDto(
                    id = "group-1",
                    shape = null,
                ),
            ),
        )

        // When
        repository.downloadGroups("user-1")

        // Then
        assertEquals(
            GroupShape.SQUARE,
            repository
                .observeGroups()
                .first()
                .single()
                .shape,
        )
    }

    @Test
    fun `GIVEN a group from the server with a shape and photo WHEN downloading THEN it shows them`() = runTest {
        // Given
        prepareScenario(
            remoteGroups = listOf(
                groupDto(
                    id = "group-1",
                    shape = "petal",
                    photoUrl = serverPhotoUrl,
                ),
            ),
        )

        // When
        repository.downloadGroups("user-1")

        // Then
        val shown = repository.observeGroups().first().single()
        assertEquals(GroupShape.PETAL, shown.shape)
        assertEquals(ProfilePhoto.Remote(serverPhotoUrl), shown.photo)
    }

    @Test
    fun `GIVEN a group WHEN turning on Simplificar dividas THEN the device holds it pending by the profile account`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(group(id = "group-1")),
                profileOwnerUserId = "user-1",
            )

            // When
            val result = turnOnSimplifyDebts()

            // Then
            assertTrue(result.isSuccess)
            val shown = repository.observeGroups().first().single()
            assertTrue(shown.simplifiesDebts)
            assertTrue(shown.isSimplifyDebtsPending)
            assertEquals("user-1", shown.simplifyDebtsChangedBy)
            assertEquals(9L, local.group("group-1")?.simplifyDebtsAt)
            assertEquals(true, local.group("group-1")?.pendingSync)
        }

    @Test
    fun `GIVEN a group WHEN turning on Simplificar dividas THEN keeps the pairs it moved as the server JSON`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(group(id = "group-1")),
                profileOwnerUserId = "user-1",
            )

            // When
            turnOnSimplifyDebts()

            // Then
            assertEquals(
                Json.parseToJsonElement(simplificationJson),
                Json.parseToJsonElement(local.group("group-1")!!.simplifyDebtsChange!!),
            )
        }

    @Test
    fun `GIVEN a group WHEN turning on Simplificar dividas THEN its other fields and last change stay`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    name = "Apê 302",
                    updatedAt = 5L,
                ),
            ),
            profileOwnerUserId = "user-1",
        )

        // When
        turnOnSimplifyDebts()

        // Then
        val stored = local.group("group-1")!!
        assertEquals(5L, stored.updatedAt)
        assertEquals("Apê 302", stored.name)
        assertEquals(
            5L,
            repository
                .observeGroups()
                .first()
                .single()
                .updatedAt,
        )
    }

    @Test
    fun `GIVEN no account yet WHEN turning on Simplificar dividas THEN it waits with nobody as who changed it`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        ownerUserId = null,
                    ),
                ),
                profileOwnerUserId = null,
            )

            // When
            val result = turnOnSimplifyDebts()

            // Then
            assertTrue(result.isSuccess)
            val shown = repository.observeGroups().first().single()
            assertTrue(shown.simplifiesDebts)
            assertTrue(shown.isSimplifyDebtsPending)
            assertNull(shown.simplifyDebtsChangedBy)
        }

    @Test
    fun `GIVEN the device cannot write WHEN turning on Simplificar dividas THEN it fails`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group(id = "group-1")),
            groupWriteError = IllegalStateException("disk full"),
        )

        // When
        val result = turnOnSimplifyDebts()

        // Then
        assertTrue(result.isFailure)
        assertFalse(local.group("group-1")!!.simplifyDebts)
    }

    @Test
    fun `GIVEN someone turned it off after this device WHEN consuming the notice THEN the group no longer has it`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(id = "group-1").copy(
                        simplifyDebtsAt = 30L,
                        simplifyDebtsBy = "user-2",
                        simplifyDebtsConflictBy = "user-2",
                    ),
                ),
            )

            // When
            val result = repository.consumeSimplifyDebtsConflict("group-1")

            // Then
            assertTrue(result.isSuccess)
            val shown = repository.observeGroups().first().single()
            assertNull(shown.simplifyDebtsConflictBy)
            assertEquals("user-2", shown.simplifyDebtsChangedBy)
        }

    @Test
    fun `GIVEN the switch turned on here WHEN uploading THEN sends it with its stamp and the change and no author`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        inviteCode = "abc",
                    ),
                ),
                profileOwnerUserId = "user-1",
            )
            turnOnSimplifyDebts()

            // When
            repository.uploadPendingGroups("user-1")

            // Then
            val pushed = remote.upsertedGroups.single().single()
            assertEquals(true, pushed.simplifyDebts)
            assertEquals(9L, pushed.simplifyDebtsAt)
            assertNull(pushed.simplifyDebtsBy)
            assertEquals(Json.parseToJsonElement(simplificationJson).jsonObject, pushed.simplifyDebtsChange)
        }

    @Test
    fun `GIVEN a renamed group with the switch already up WHEN uploading THEN sends the switch without a change`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        pendingSync = true,
                        inviteCode = "abc",
                    ).copy(
                        simplifyDebts = true,
                        simplifyDebtsAt = 4L,
                        simplifyDebtsBy = "user-2",
                    ),
                ),
            )

            // When
            repository.uploadPendingGroups("user-1")

            // Then
            val pushed = remote.upsertedGroups.single().single()
            assertEquals(true, pushed.simplifyDebts)
            assertEquals(4L, pushed.simplifyDebtsAt)
            assertNull(pushed.simplifyDebtsBy)
            assertNull(pushed.simplifyDebtsChange)
        }

    @Test
    fun `GIVEN the switch turned on here WHEN the server takes it THEN it no longer waits`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    inviteCode = "abc",
                ),
            ),
            profileOwnerUserId = "user-1",
        )
        turnOnSimplifyDebts()

        // When
        repository.uploadPendingGroups("user-1")

        // Then
        val stored = local.group("group-1")!!
        assertTrue(stored.simplifyDebts)
        assertFalse(stored.simplifyDebtsPending)
        assertNull(stored.simplifyDebtsChange)
        assertFalse(stored.pendingSync)
        assertFalse(
            repository
                .observeGroups()
                .first()
                .single()
                .isSimplifyDebtsPending,
        )
    }

    @Test
    fun `GIVEN the server refuses the row WHEN uploading THEN the switch keeps waiting with its change`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(
                group(
                    id = "group-1",
                    inviteCode = "abc",
                ),
            ),
            profileOwnerUserId = "user-1",
        )
        turnOnSimplifyDebts()
        remote.refusedGroupIds = setOf("group-1")

        // When
        repository.uploadPendingGroups("user-1")

        // Then
        val stored = local.group("group-1")!!
        assertTrue(stored.simplifyDebtsPending)
        assertEquals(
            Json.parseToJsonElement(simplificationJson),
            Json.parseToJsonElement(stored.simplifyDebtsChange!!),
        )
    }

    @Test
    fun `GIVEN the server keeps a later change of the switch WHEN uploading THEN this device change keeps waiting`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        inviteCode = "abc",
                    ),
                ),
                profileOwnerUserId = "user-1",
            )
            turnOnSimplifyDebts()
            remote.serverSimplifyDebtsAt = mapOf("group-1" to 30L)

            // When
            repository.uploadPendingGroups("user-1")

            // Then
            assertEquals(true, local.group("group-1")?.simplifyDebtsPending)
            assertTrue(
                repository
                    .observeGroups()
                    .first()
                    .single()
                    .isSimplifyDebtsPending,
            )
        }

    @Test
    fun `GIVEN the server refused the switch for a later one of Ana WHEN downloading THEN takes hers as a conflict`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        inviteCode = "abc",
                    ),
                ),
                remoteGroups = listOf(
                    groupDto(
                        id = "group-1",
                        inviteCode = "abc",
                        simplifyDebts = false,
                        simplifyDebtsAt = 30L,
                        simplifyDebtsBy = "user-2",
                    ),
                ),
                profileOwnerUserId = "user-1",
            )
            turnOnSimplifyDebts()
            remote.refusedGroupIds = setOf("group-1")
            repository.uploadPendingGroups("user-1")

            // When
            repository.downloadGroups("user-1")

            // Then
            val shown = repository.observeGroups().first().single()
            assertFalse(shown.simplifiesDebts)
            assertFalse(shown.isSimplifyDebtsPending)
            assertEquals("user-2", shown.simplifyDebtsChangedBy)
            assertEquals("user-2", shown.simplifyDebtsConflictBy)
        }

    @Test
    fun `GIVEN the switch changes while its push is in flight WHEN uploading THEN the newer change keeps waiting`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        inviteCode = "abc",
                    ),
                ),
                profileOwnerUserId = "user-1",
            )
            turnOnSimplifyDebts()
            remote.onUpsertGroups = { turnOffSimplifyDebts() }

            // When
            repository.uploadPendingGroups("user-1")

            // Then
            val stored = local.group("group-1")!!
            assertFalse(stored.simplifyDebts)
            assertEquals(20L, stored.simplifyDebtsAt)
            assertTrue(stored.simplifyDebtsPending)
        }

    @Test
    fun `GIVEN the switch changed while its push was in flight WHEN uploading next THEN sends the newer change`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        inviteCode = "abc",
                    ),
                ),
                profileOwnerUserId = "user-1",
            )
            turnOnSimplifyDebts()
            remote.onUpsertGroups = { turnOffSimplifyDebts() }
            repository.uploadPendingGroups("user-1")
            remote.onUpsertGroups = {}

            // When
            repository.uploadPendingGroups("user-1")

            // Then
            assertEquals(2, remote.upsertedGroups.size)
            val pushed = remote.upsertedGroups.last().single()
            assertEquals(false, pushed.simplifyDebts)
            assertEquals(20L, pushed.simplifyDebtsAt)
            assertFalse(local.group("group-1")!!.simplifyDebtsPending)
        }

    @Test
    fun `GIVEN the switch changed while its push was in flight WHEN the push ends THEN changes are still pending`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        inviteCode = "abc",
                    ),
                ),
                profileOwnerUserId = "user-1",
            )
            turnOnSimplifyDebts()
            remote.onUpsertGroups = { turnOffSimplifyDebts() }

            // When
            repository.uploadPendingGroups("user-1")

            // Then
            assertTrue(repository.observeHasPendingChanges().first())
        }

    @Test
    fun `GIVEN a group on the server whose photo fails to go up WHEN uploading THEN its switch keeps waiting`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        pendingSync = true,
                        inviteCode = "abc",
                        pendingPhoto = photoJpeg,
                    ),
                ),
                profileOwnerUserId = "user-1",
            )
            turnOnSimplifyDebts()
            photoRemote.uploadError = IllegalStateException("no connection")

            // When
            repository.uploadPendingGroups("user-1")

            // Then
            assertTrue(remote.upsertedGroups.isEmpty())
            val stored = local.group("group-1")!!
            assertTrue(stored.simplifyDebtsPending)
            assertTrue(stored.pendingSync)
        }

    @Test
    fun `GIVEN a new group whose photo Storage refuses WHEN uploading THEN the switch its row took up stops waiting`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        pendingSync = true,
                        pendingPhoto = photoJpeg,
                    ),
                ),
                profileOwnerUserId = "user-1",
            )
            turnOnSimplifyDebts()
            photoRemote.refusesUploads = true

            // When
            repository.uploadPendingGroups("user-1")

            // Then
            assertEquals(
                9L,
                remote.upsertedGroups
                    .single()
                    .single()
                    .simplifyDebtsAt,
            )
            val stored = local.group("group-1")!!
            assertFalse(stored.simplifyDebtsPending)
            assertTrue(stored.pendingSync)
        }

    private val simplificationJson =
        """{"before":[{"from":"m-2","to":"m-1","amount":3000},{"from":"m-3","to":"m-2","amount":3000}],""" +
            """"after":[{"from":"m-3","to":"m-1","amount":3000}]}"""

    private val simplification = SimplifyDebtsChange(
        before = listOf(
            SimplifyDebtsPair(
                fromMemberId = "m-2",
                toMemberId = "m-1",
                amountMinor = 3_000L,
                currency = null,
            ),
            SimplifyDebtsPair(
                fromMemberId = "m-3",
                toMemberId = "m-2",
                amountMinor = 3_000L,
                currency = null,
            ),
        ),
        after = listOf(
            SimplifyDebtsPair(
                fromMemberId = "m-3",
                toMemberId = "m-1",
                amountMinor = 3_000L,
                currency = null,
            ),
        ),
    )

    private suspend fun turnOnSimplifyDebts(): Result<Unit> = repository.updateSimplifiesDebts(
        groupId = "group-1",
        simplifiesDebts = true,
        change = simplification,
        changedAt = 9L,
    )

    private suspend fun turnOffSimplifyDebts() {
        repository.updateSimplifiesDebts(
            groupId = "group-1",
            simplifiesDebts = false,
            change = SimplifyDebtsChange(
                before = simplification.after,
                after = simplification.before,
            ),
            changedAt = 20L,
        )
    }

    private val photoJpeg = byteArrayOf(1, 2, 3)
    private val newPhotoJpeg = byteArrayOf(4, 5, 6)
    private val oldPhotoUrl = "https://cdn.test/storage/v1/object/public/group-photos/group-1/photo.jpg?v=1"
    private val serverPhotoUrl = "https://cdn.test/storage/v1/object/public/group-photos/group-1/photo.jpg?v=9"

    private val twoGroups = listOf(
        group(id = "group-1"),
        group(
            id = "group-2",
            name = "Jantar",
        ),
    )
    private val twoGroupsMembers = listOf(
        member(
            id = "m-1",
            groupId = "group-1",
            name = "Ana",
        ),
        member(
            id = "m-2",
            groupId = "group-2",
            name = "Bruno",
            userId = "user-2",
        ),
    )
    private val brunoPhoto = MemberPhotoEntity(
        userId = "user-2",
        photoUrl = "https://photos/bruno.jpg",
    )

    private suspend fun editGroup(
        shape: GroupShape = GroupShape.SQUARE,
        photo: ProfilePhoto? = null,
    ) {
        repository.updateGroup(
            groupId = "group-1",
            name = "Viagem Floripa",
            type = GroupType.TRIP,
            color = AvatarColor.BLUE,
            shape = shape,
            photo = photo,
            currency = GroupCurrency.BRL,
            updatedAt = 7L,
        )
    }

    private fun group(
        id: String,
        name: String = "Viagem Floripa",
        createdBy: String? = "user-1",
        updatedAt: Long = 1L,
        pendingSync: Boolean = false,
        ownerUserId: String? = "user-1",
        inviteCode: String? = null,
        isOnServer: Boolean = inviteCode != null || !pendingSync,
        shape: String? = null,
        photoUrl: String? = null,
        pendingPhoto: ByteArray? = null,
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
        shape = shape,
        photoUrl = photoUrl,
        pendingPhoto = pendingPhoto,
        simplifyDebts = false,
        simplifyDebtsAt = 0L,
        simplifyDebtsBy = null,
        simplifyDebtsChange = null,
        simplifyDebtsPending = false,
        simplifyDebtsConflictBy = null,
        isOnServer = isOnServer,
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
        shape: String? = null,
        photoUrl: String? = null,
        simplifyDebts: Boolean? = null,
        simplifyDebtsAt: Long? = null,
        simplifyDebtsBy: String? = null,
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
        shape = shape,
        photoUrl = photoUrl,
        simplifyDebts = simplifyDebts,
        simplifyDebtsAt = simplifyDebtsAt,
        simplifyDebtsBy = simplifyDebtsBy,
        simplifyDebtsChange = null,
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
        storedPhotos: List<MemberPhotoEntity> = emptyList(),
        remoteMemberPhotos: List<MemberPhotoDto> = emptyList(),
        memberPhotosError: Throwable? = null,
        myPhotoUrl: String? = null,
        takenInviteCodes: Set<String> = emptySet(),
        keepsSentInviteCodes: Boolean = false,
        madeCodes: List<String> = emptyList(),
    ) {
        local = FakeGroupLocalDataSource(storedGroups, storedMembers).also {
            it.insertError = insertError
            it.memberWriteError = memberWriteError
            it.groupWriteError = groupWriteError
            it.photos.value = storedPhotos
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
            it.memberPhotos = remoteMemberPhotos
            it.memberPhotosError = memberPhotosError
            it.takenInviteCodes = takenInviteCodes
            it.keepsSentInviteCodes = keepsSentInviteCodes
        }
        profileLocal = FakeGroupOwnerProfileLocalDataSource(profileOwnerUserId).also {
            it.photoUrl.value = myPhotoUrl
        }
        deletions = InMemoryGroupDeletionRepository()
        photoRemote = FakeGroupPhotoRemoteDataSource()
        repository = GroupRepositoryImpl(
            local = local,
            remote = remote,
            photoRemote = photoRemote,
            profileLocal = profileLocal,
            profileMapper = ProfileMapperImpl(),
            mapper = GroupMapperImpl(),
            deletions = deletions,
            randomCode = { length ->
                madeCodeLengths += length
                madeCodes.getOrNull(madeCodeLengths.lastIndex) ?: "N".repeat(length)
            },
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
    val photos = MutableStateFlow<List<MemberPhotoEntity>>(emptyList())
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

    override fun observeHasGroups(): Flow<Boolean> = groups.map { it.isNotEmpty() }

    override fun observeMembers(): Flow<List<GroupMemberEntity>> = members

    override fun observeMemberPhotos(): Flow<List<MemberPhotoEntity>> = photos

    override fun observeMemberShareCode(userId: String): Flow<String?> =
        photos.map { entities -> entities.firstOrNull { it.userId == userId }?.shareCode }

    override suspend fun replaceMemberPhotos(entities: List<MemberPhotoEntity>) {
        photos.value = entities
    }

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

    override suspend fun updateGroupPhoto(
        id: String,
        photoUrl: String?,
        pendingPhoto: ByteArray?,
        updatedAt: Long,
    ) {
        groupWriteError?.let { throw it }
        groups.value = groups.value.map {
            if (it.id == id) {
                it.copy(
                    photoUrl = photoUrl,
                    pendingPhoto = pendingPhoto,
                    updatedAt = maxOf(updatedAt, it.updatedAt + 1),
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
        shape: String?,
        photoUrl: String?,
        pendingPhoto: ByteArray?,
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
                    shape = shape ?: it.shape,
                    photoUrl = photoUrl,
                    pendingPhoto = pendingPhoto,
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
        updatedAt: Long,
    ) {
        groupWriteError?.let { throw it }
        groups.value = groups.value.map {
            if (it.id == id) {
                it.copy(
                    defaultSplit = defaultSplit,
                    updatedAt = maxOf(updatedAt, it.updatedAt + 1),
                    pendingSync = true,
                )
            } else {
                it
            }
        }
    }

    override suspend fun getDefaultSplit(id: String): String? {
        groupWriteError?.let { throw it }
        return group(id)?.defaultSplit
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
        syncedSimplifyDebtsAt: Long,
    ) {
        groups.value = groups.value.map {
            val isSynced = it.id == id && it.updatedAt == syncedUpdatedAt && it.simplifyDebtsAt == syncedSimplifyDebtsAt
            if (isSynced) it.copy(pendingSync = false) else it
        }
    }

    override suspend fun markGroupPhotoUploaded(
        id: String,
        jpeg: ByteArray,
        photoUrl: String,
    ) {
        groups.value = groups.value.map {
            if (it.id == id && it.pendingPhoto?.contentEquals(jpeg) == true) {
                it.copy(
                    photoUrl = photoUrl,
                    pendingPhoto = null,
                )
            } else {
                it
            }
        }
    }

    override suspend fun dropRefusedGroupPhoto(
        id: String,
        jpeg: ByteArray,
    ) {
        groups.value = groups.value.map {
            if (it.id == id && it.pendingPhoto?.contentEquals(jpeg) == true) it.copy(pendingPhoto = null) else it
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
                        pendingPhoto = null,
                        splitIncomes = current.splitIncomes,
                        simplifyDebts = current.simplifyDebts,
                        simplifyDebtsAt = current.simplifyDebtsAt,
                        simplifyDebtsBy = current.simplifyDebtsBy,
                        simplifyDebtsChange = current.simplifyDebtsChange,
                        simplifyDebtsPending = current.simplifyDebtsPending,
                        simplifyDebtsConflictBy = current.simplifyDebtsConflictBy,
                    ),
                )
        }
        entity.inviteCode?.let {
            updateInviteCode(
                id = entity.id,
                inviteCode = it,
            )
        }
        markGroupsOnServer(listOf(entity.id))
        val now = group(entity.id)
        if (current != null && now != null && now.ownerUserId == entity.ownerUserId &&
            now.simplifyDebtsAt <= entity.simplifyDebtsAt
        ) {
            val isConflict = now.simplifyDebtsPending &&
                now.simplifyDebts != entity.simplifyDebts &&
                now.simplifyDebtsAt < entity.simplifyDebtsAt
            put(
                now.copy(
                    simplifyDebts = entity.simplifyDebts,
                    simplifyDebtsAt = entity.simplifyDebtsAt,
                    simplifyDebtsBy = entity.simplifyDebtsBy,
                    simplifyDebtsChange = null,
                    simplifyDebtsPending = false,
                    simplifyDebtsConflictBy = if (isConflict) entity.simplifyDebtsBy else now.simplifyDebtsConflictBy,
                ),
            )
        }
        group(
            entity.id,
        )?.takeIf { it.simplifyDebtsPending && !it.pendingSync }?.let { put(it.copy(pendingSync = true)) }
    }

    override suspend fun updateSimplifyDebts(
        id: String,
        simplifyDebts: Boolean,
        change: String?,
        changedAt: Long,
        by: String?,
    ) {
        groupWriteError?.let { throw it }
        groups.value = groups.value.map {
            if (it.id == id) {
                it.copy(
                    simplifyDebts = simplifyDebts,
                    simplifyDebtsChange = change,
                    simplifyDebtsAt = maxOf(changedAt, it.simplifyDebtsAt + 1),
                    simplifyDebtsBy = by,
                    simplifyDebtsPending = true,
                    simplifyDebtsConflictBy = null,
                    pendingSync = true,
                )
            } else {
                it
            }
        }
    }

    override suspend fun markSimplifyDebtsSynced(
        id: String,
        syncedAt: Long,
    ) {
        groups.value = groups.value.map {
            if (it.id == id && it.simplifyDebtsAt == syncedAt && it.simplifyDebtsPending) {
                it.copy(
                    simplifyDebtsPending = false,
                    simplifyDebtsChange = null,
                )
            } else {
                it
            }
        }
    }

    override suspend fun clearSimplifyDebtsConflict(id: String) {
        groups.value = groups.value.map { if (it.id == id) it.copy(simplifyDebtsConflictBy = null) else it }
    }

    override suspend fun updateInviteCode(
        id: String,
        inviteCode: String,
    ) {
        groups.value = groups.value.map { if (it.id == id) it.copy(inviteCode = inviteCode) else it }
    }

    override suspend fun isOnServer(id: String): Boolean = group(id)?.isOnServer == true

    override suspend fun markGroupsOnServer(ids: List<String>) {
        groups.value = groups.value.map { if (it.id in ids) it.copy(isOnServer = true) else it }
    }

    override suspend fun replaceInviteCodeBeforeUpload(
        id: String,
        inviteCode: String,
    ) {
        groups.value = groups.value.map { if (it.id == id && !it.isOnServer) it.copy(inviteCode = inviteCode) else it }
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
            .filter { it.isOnServer && it.id !in serverGroupIds }
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
        .filter { it.ownerUserId == ownerUserId && it.isOnServer }
        .map { it.id }

    override suspend fun deleteGroups(ids: List<String>) {
        members.value = members.value.filterNot { it.groupId in ids }
        groups.value = groups.value.filterNot { it.id in ids }
    }

    override suspend fun clear() {
        members.value = emptyList()
        groups.value = emptyList()
        photos.value = emptyList()
    }
}

// Why: the URL is the bucket's public one with the version, as Supabase's; uploads are recorded in order.
private class FakeGroupPhotoRemoteDataSource : GroupPhotoRemoteDataSource {
    val uploads = mutableListOf<Pair<String, ByteArray>>()
    var uploadError: Throwable? = null
    var refusesUploads = false

    // Why: runs while the photo is going up, so a test can edit the group meanwhile.
    var onUpload: suspend () -> Unit = {}

    override fun urlOf(
        groupId: String,
        version: Long,
    ): String = "https://cdn.test/storage/v1/object/public/group-photos/$groupId/photo.jpg?v=$version"

    override suspend fun upload(
        groupId: String,
        jpeg: ByteArray,
    ): Boolean {
        onUpload()
        uploadError?.let { throw it }
        if (refusesUploads) return false
        uploads += groupId to jpeg
        return true
    }
}

private class FakeGroupOwnerProfileLocalDataSource(
    private val ownerUserId: String?,
) : ProfileLocalDataSource {
    // Why: the profile's own photo, which this device's user's member shows.
    val photoUrl = MutableStateFlow<String?>(null)

    // Why: a photo picked offline, not uploaded yet.
    var pendingPhoto: ByteArray? = null

    override fun observe(): Flow<ProfileEntity?> = photoUrl.map(::profile)

    override suspend fun get(): ProfileEntity = profile(photoUrl.value)

    private fun profile(photoUrl: String?): ProfileEntity = ProfileEntity(
        id = "profile-1",
        displayName = "Pierre",
        avatarColor = "lilac",
        photoUrl = photoUrl,
        updatedAt = 1L,
        pendingSync = false,
        pendingPhoto = pendingPhoto,
        ownerUserId = ownerUserId,
        shareCode = null,
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

    override suspend fun createShareCode(
        ownerUserId: String,
        shareCode: String,
    ) = error("unused")

    override suspend fun replaceShareCode(
        ownerUserId: String,
        shareCode: String,
    ) = error("unused")

    override suspend fun applyRemoteShareCode(
        ownerUserId: String,
        shareCode: String?,
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
    var onUpsertGroups: suspend () -> Unit = {}

    // Why: every push of the groups, a refused one included, with what it sent.
    val upsertGroupsAttempts = mutableListOf<List<GroupDto>>()

    // Why: the codes other groups have, which the server refuses a new group's push with, the whole push at once.
    var takenInviteCodes: Set<String> = emptySet()

    // Why: a server that keeps the code a new group goes up with; an older one answers with its own (inviteCodes).
    var keepsSentInviteCodes = false

    // Why: rows the server leaves out of its answer, and the switch stamp it answers with in place of the one sent.
    var refusedGroupIds: Set<String> = emptySet()
    var serverSimplifyDebtsAt: Map<String, Long> = emptyMap()
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

    // Why: the profiles' photos the server lets this user read.
    var memberPhotos: List<MemberPhotoDto> = emptyList()
    var memberPhotosError: Throwable? = null
    var photoFetches = 0

    override suspend fun upsertGroups(groups: List<GroupDto>): List<GroupDto> {
        calls += "groups"
        upsertGroupsAttempts += groups
        onUpsertGroups()
        upsertGroupsError?.let { throw it }
        if (groups.mapNotNull { it.inviteCode }.any { it in takenInviteCodes }) throw InviteCodeTakenException()
        upsertedGroups += groups
        return groups
            .filterNot { it.id in refusedGroupIds }
            .map {
                it.copy(
                    inviteCode = if (keepsSentInviteCodes) it.inviteCode ?: inviteCodes[it.id] else inviteCodes[it.id],
                    simplifyDebtsAt = serverSimplifyDebtsAt[it.id] ?: it.simplifyDebtsAt,
                )
            }
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

    // Why: the first download's order is the server's, so the groups come in the order given.
    override suspend fun fetchFirstDownloadGroupIds(): List<String> {
        fetchError?.let { throw it }
        return groups.map { it.id }
    }

    override suspend fun fetchGroup(groupId: String): GroupDto? {
        fetchError?.let { throw it }
        return groups.firstOrNull { it.id == groupId }
    }

    override suspend fun fetchMembers(groupId: String): List<GroupMemberDto> {
        fetchError?.let { throw it }
        return members.filter { it.groupId == groupId }
    }

    override suspend fun fetchMemberPhotos(): List<MemberPhotoDto> {
        photoFetches++
        memberPhotosError?.let { throw it }
        return memberPhotos
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
