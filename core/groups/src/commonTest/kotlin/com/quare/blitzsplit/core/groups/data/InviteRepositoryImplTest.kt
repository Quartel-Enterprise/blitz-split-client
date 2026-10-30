package com.quare.blitzsplit.core.groups.data

import com.quare.blitzsplit.core.groups.data.datasource.GroupRemoteDataSource
import com.quare.blitzsplit.core.groups.data.dto.GroupDto
import com.quare.blitzsplit.core.groups.data.dto.GroupMemberDto
import com.quare.blitzsplit.core.groups.data.dto.InviteDto
import com.quare.blitzsplit.core.groups.data.dto.InviteMemberDto
import com.quare.blitzsplit.core.groups.data.dto.JoinGroupParamsDto
import com.quare.blitzsplit.core.groups.data.dto.JoinGroupResultDto
import com.quare.blitzsplit.core.groups.data.mapper.InviteMapperImpl
import com.quare.blitzsplit.core.groups.data.repository.InviteRepositoryImpl
import com.quare.blitzsplit.core.groups.domain.model.Invite
import com.quare.blitzsplit.core.groups.domain.model.InviteMember
import com.quare.blitzsplit.core.groups.domain.model.JoinChoice
import com.quare.blitzsplit.core.groups.domain.model.JoinOutcome
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InviteRepositoryImplTest {
    private lateinit var remote: FakeInviteRemoteDataSource
    private lateinit var repository: InviteRepositoryImpl

    @Test
    fun `GIVEN a code of a group WHEN fetching its invite THEN returns the group as the invite shows it`() = runTest {
        // Given
        prepareScenario(invite = inviteDto)

        // When
        val result = repository.fetchInvite("aB3dE5gH")

        // Then
        assertEquals(
            Invite(
                code = "aB3dE5gH",
                groupId = "group-1",
                groupName = "Viagem Floripa",
                groupType = GroupType.TRIP,
                groupColor = AvatarColor.PEACH,
                currency = GroupCurrency.BRL,
                creatorName = "Pierre",
                isMember = false,
                members = listOf(
                    InviteMember(
                        id = "m-1",
                        name = "Pierre",
                        avatarColor = AvatarColor.BLUE,
                        hasJoined = true,
                    ),
                ),
            ),
            result.getOrThrow(),
        )
        assertEquals(listOf("aB3dE5gH"), remote.fetchedCodes)
    }

    @Test
    fun `GIVEN a code no group has WHEN fetching its invite THEN succeeds with no invite`() = runTest {
        // Given
        prepareScenario(invite = null)

        // When
        val result = repository.fetchInvite("aB3dE5gH")

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow())
    }

    @Test
    fun `GIVEN the server can not be reached WHEN fetching an invite THEN returns the error`() = runTest {
        // Given
        prepareScenario(fetchError = IllegalStateException("offline"))

        // When
        val result = repository.fetchInvite("aB3dE5gH")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN the fetch is cancelled WHEN fetching an invite THEN the cancellation goes through`() = runTest {
        // Given
        prepareScenario(fetchError = CancellationException("left the screen"))

        // When
        val error = assertFailsWith<CancellationException> { repository.fetchInvite("aB3dE5gH") }

        // Then
        assertEquals("left the screen", error.message)
    }

    @Test
    fun `GIVEN a member nobody joined as WHEN joining as that member THEN sends the member and no new name`() =
        runTest {
            // Given
            prepareScenario()
            val choice = JoinChoice.AsMember(memberId = "m-2")

            // When
            repository.joinGroup("aB3dE5gH", choice, AvatarColor.GREEN)

            // Then
            assertEquals(
                listOf(
                    JoinGroupParamsDto(
                        code = "aB3dE5gH",
                        avatarColor = "green",
                        memberId = "m-2",
                        newName = null,
                    ),
                ),
                remote.joins,
            )
        }

    @Test
    fun `GIVEN someone not on the list WHEN joining with a new name THEN sends the name and no member`() = runTest {
        // Given
        prepareScenario()
        val choice = JoinChoice.AsNewMember(name = "Carla")

        // When
        repository.joinGroup("aB3dE5gH", choice, AvatarColor.SAND)

        // Then
        assertEquals(
            listOf(
                JoinGroupParamsDto(
                    code = "aB3dE5gH",
                    avatarColor = "sand",
                    memberId = null,
                    newName = "Carla",
                ),
            ),
            remote.joins,
        )
    }

    @Test
    fun `GIVEN the server joins the group WHEN joining THEN returns joined to that group`() = runTest {
        // Given
        prepareScenario(
            joinResult = JoinGroupResultDto(
                status = "joined",
                groupId = "group-1",
                memberId = "member-9",
            ),
        )

        // When
        val result = repository.joinGroup("aB3dE5gH", JoinChoice.AsMember("m-2"), AvatarColor.BLUE)

        // Then
        assertEquals(JoinOutcome.Joined("group-1"), result.getOrThrow())
    }

    @Test
    fun `GIVEN someone else took the member first WHEN joining THEN returns the member taken`() = runTest {
        // Given
        prepareScenario(
            joinResult = JoinGroupResultDto(
                status = "taken",
                groupId = "group-1",
                memberId = null,
            ),
        )

        // When
        val result = repository.joinGroup("aB3dE5gH", JoinChoice.AsMember("m-2"), AvatarColor.BLUE)

        // Then
        assertEquals(JoinOutcome.MemberTaken, result.getOrThrow())
    }

    @Test
    fun `GIVEN the server can not be reached WHEN joining THEN returns the error`() = runTest {
        // Given
        prepareScenario(joinError = IllegalStateException("offline"))

        // When
        val result = repository.joinGroup("aB3dE5gH", JoinChoice.AsMember("m-2"), AvatarColor.BLUE)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN the join is cancelled WHEN joining THEN the cancellation goes through`() = runTest {
        // Given
        prepareScenario(joinError = CancellationException("left the screen"))

        // When
        val error = assertFailsWith<CancellationException> {
            repository.joinGroup("aB3dE5gH", JoinChoice.AsMember("m-2"), AvatarColor.BLUE)
        }

        // Then
        assertEquals("left the screen", error.message)
    }

    @Test
    fun `GIVEN the server answers joined without a group WHEN joining THEN returns an error`() = runTest {
        // Given
        prepareScenario(
            joinResult = JoinGroupResultDto(
                status = "joined",
                groupId = null,
                memberId = "member-9",
            ),
        )

        // When
        val result = repository.joinGroup("aB3dE5gH", JoinChoice.AsMember("m-2"), AvatarColor.BLUE)

        // Then
        assertTrue(result.isFailure)
    }

    private val inviteDto = InviteDto(
        groupId = "group-1",
        name = "Viagem Floripa",
        type = "trip",
        color = "peach",
        currency = "BRL",
        creatorName = "Pierre",
        isMember = false,
        members = listOf(
            InviteMemberDto(
                id = "m-1",
                name = "Pierre",
                avatarColor = "blue",
                hasJoined = true,
            ),
        ),
    )

    private fun prepareScenario(
        invite: InviteDto? = null,
        fetchError: Throwable? = null,
        joinResult: JoinGroupResultDto = JoinGroupResultDto(
            status = "joined",
            groupId = "group-1",
            memberId = "member-9",
        ),
        joinError: Throwable? = null,
    ) {
        remote = FakeInviteRemoteDataSource(invite, fetchError, joinResult, joinError)
        repository = InviteRepositoryImpl(
            remote = remote,
            mapper = InviteMapperImpl(),
        )
    }
}

private class FakeInviteRemoteDataSource(
    private val invite: InviteDto?,
    private val fetchError: Throwable?,
    private val joinResult: JoinGroupResultDto,
    private val joinError: Throwable?,
) : GroupRemoteDataSource {
    val fetchedCodes = mutableListOf<String>()
    val joins = mutableListOf<JoinGroupParamsDto>()

    override suspend fun fetchInvite(code: String): InviteDto? {
        fetchedCodes += code
        fetchError?.let { throw it }
        return invite
    }

    override suspend fun joinGroup(params: JoinGroupParamsDto): JoinGroupResultDto {
        joins += params
        joinError?.let { throw it }
        return joinResult
    }

    override suspend fun upsertGroups(groups: List<GroupDto>): List<GroupDto> = error("unused")

    override suspend fun upsertMembers(members: List<GroupMemberDto>) = error("unused")

    override suspend fun deleteMembers(ids: List<String>) = error("unused")

    override suspend fun fetchGroups(): List<GroupDto> = error("unused")

    override suspend fun fetchMembers(): List<GroupMemberDto> = error("unused")
}
