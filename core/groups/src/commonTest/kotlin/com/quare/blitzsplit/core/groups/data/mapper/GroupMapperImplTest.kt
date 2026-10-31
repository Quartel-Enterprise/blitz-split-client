package com.quare.blitzsplit.core.groups.data.mapper

import com.quare.blitzsplit.core.groups.data.dto.GroupDto
import com.quare.blitzsplit.core.groups.data.dto.GroupMemberDto
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.provider.room.entity.GroupEntity
import com.quare.blitzsplit.core.provider.room.entity.GroupMemberEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupMapperImplTest {
    private val mapper = GroupMapperImpl()

    @Test
    fun `GIVEN members of two groups interleaved WHEN mapping to the domain THEN each gets its own in order`() {
        // Given
        val groups = listOf(groupEntity(id = "group-1"), groupEntity(id = "group-2"))
        val members = listOf(
            memberEntity(
                id = "m-1",
                groupId = "group-1",
            ),
            memberEntity(
                id = "m-2",
                groupId = "group-2",
            ),
            memberEntity(
                id = "m-3",
                groupId = "group-1",
            ),
        )

        // When
        val mapped = mapper.toDomain(groups, members)

        // Then
        assertEquals(listOf("group-1", "group-2"), mapped.map { it.id })
        assertEquals(listOf("m-1", "m-3"), mapped[0].members.map { it.id })
        assertEquals(listOf("m-2"), mapped[1].members.map { it.id })
    }

    @Test
    fun `GIVEN a group without members WHEN mapping to the domain THEN it has none`() {
        // Given
        val groups = listOf(groupEntity(id = "group-1"))
        val members = listOf(
            memberEntity(
                id = "m-1",
                groupId = "group-2",
            ),
        )

        // When
        val mapped = mapper.toDomain(groups, members)

        // Then
        assertEquals(emptyList(), mapped.single().members)
    }

    @Test
    fun `GIVEN known codes WHEN mapping to the domain THEN reads the type color and currency`() {
        // Given
        val groups = listOf(
            groupEntity(
                id = "group-1",
                type = "home",
                color = "peach",
                currency = "EUR",
            ),
        )

        // When
        val mapped = mapper.toDomain(groups, emptyList()).single()

        // Then
        assertEquals("Viagem Floripa", mapped.name)
        assertEquals(GroupType.HOME, mapped.type)
        assertEquals(AvatarColor.PEACH, mapped.color)
        assertEquals(GroupCurrency.EUR, mapped.currency)
    }

    @Test
    fun `GIVEN a type this version does not know WHEN mapping to the domain THEN reads it as other`() {
        // Given
        val groups = listOf(
            groupEntity(
                id = "group-1",
                type = "wedding",
            ),
        )

        // When
        val mapped = mapper.toDomain(groups, emptyList()).single()

        // Then
        assertEquals(GroupType.OTHER, mapped.type)
    }

    @Test
    fun `GIVEN a color this version does not know WHEN mapping to the domain THEN reads it as the default color`() {
        // Given
        val groups = listOf(
            groupEntity(
                id = "group-1",
                color = "magenta",
            ),
        )

        // When
        val mapped = mapper.toDomain(groups, emptyList()).single()

        // Then
        assertEquals(AvatarColor.default, mapped.color)
    }

    @Test
    fun `GIVEN a currency this version does not know WHEN mapping to the domain THEN reads it as dollars`() {
        // Given
        val groups = listOf(
            groupEntity(
                id = "group-1",
                currency = "XYZ",
            ),
        )

        // When
        val mapped = mapper.toDomain(groups, emptyList()).single()

        // Then
        assertEquals(GroupCurrency.USD, mapped.currency)
    }

    @Test
    fun `GIVEN an unknown member color WHEN mapping to the domain THEN reads it as the default color`() {
        // Given
        val groups = listOf(groupEntity(id = "group-1"))
        val members = listOf(
            memberEntity(
                id = "m-1",
                avatarColor = "magenta",
            ),
        )

        // When
        val mapped = mapper.toDomain(groups, members).single()

        // Then
        assertEquals(AvatarColor.default, mapped.members.single().avatarColor)
    }

    @Test
    fun `GIVEN a stored member WHEN mapping to the domain THEN keeps its name and color`() {
        // Given
        val groups = listOf(groupEntity(id = "group-1"))
        val members = listOf(
            memberEntity(
                id = "m-1",
                name = "Ana",
                avatarColor = "green",
            ),
        )

        // When
        val mapped = mapper.toDomain(groups, members).single()

        // Then
        assertEquals(
            GroupMember(
                id = "m-1",
                name = "Ana",
                avatarColor = AvatarColor.GREEN,
                isMe = false,
                hasJoined = false,
                inviteCode = null,
            ),
            mapped.members.single(),
        )
    }

    @Test
    fun `GIVEN this device user before any account WHEN mapping to the domain THEN is me and has joined`() {
        // Given
        val groups = listOf(groupEntity(id = "group-1"))
        val members = listOf(
            memberEntity(
                id = "m-1",
                isMe = true,
                userId = null,
            ),
        )

        // When
        val member = mapper
            .toDomain(groups, members)
            .single()
            .members
            .single()

        // Then
        assertTrue(member.isMe)
        assertTrue(member.hasJoined)
    }

    @Test
    fun `GIVEN another member with an account WHEN mapping to the domain THEN is not me but has joined`() {
        // Given
        val groups = listOf(groupEntity(id = "group-1"))
        val members = listOf(
            memberEntity(
                id = "m-1",
                isMe = false,
                userId = "user-2",
            ),
        )

        // When
        val member = mapper
            .toDomain(groups, members)
            .single()
            .members
            .single()

        // Then
        assertFalse(member.isMe)
        assertTrue(member.hasJoined)
    }

    @Test
    fun `GIVEN a member that is just a name WHEN mapping to the domain THEN has not joined`() {
        // Given
        val groups = listOf(groupEntity(id = "group-1"))
        val members = listOf(
            memberEntity(
                id = "m-1",
                isMe = false,
                userId = null,
            ),
        )

        // When
        val member = mapper
            .toDomain(groups, members)
            .single()
            .members
            .single()

        // Then
        assertFalse(member.hasJoined)
    }

    @Test
    fun `GIVEN a group pending sync WHEN mapping to the domain THEN is pending`() {
        // Given
        val groups = listOf(
            groupEntity(
                id = "group-1",
                pendingSync = true,
            ),
        )

        // When
        val mapped = mapper.toDomain(groups, emptyList()).single()

        // Then
        assertTrue(mapped.isPending)
    }

    @Test
    fun `GIVEN a synced group with a pending member WHEN mapping to the domain THEN is pending`() {
        // Given
        val groups = listOf(
            groupEntity(
                id = "group-1",
                pendingSync = false,
            ),
        )
        val members = listOf(
            memberEntity(
                id = "m-1",
                pendingSync = true,
            ),
        )

        // When
        val mapped = mapper.toDomain(groups, members).single()

        // Then
        assertTrue(mapped.isPending)
    }

    @Test
    fun `GIVEN a synced group and synced members WHEN mapping to the domain THEN is not pending`() {
        // Given
        val groups = listOf(
            groupEntity(
                id = "group-1",
                pendingSync = false,
            ),
        )
        val members = listOf(
            memberEntity(
                id = "m-1",
                pendingSync = false,
            ),
        )

        // When
        val mapped = mapper.toDomain(groups, members).single()

        // Then
        assertFalse(mapped.isPending)
    }

    @Test
    fun `GIVEN a new group WHEN mapping it to an entity THEN it is pending with no owner and no creator yet`() {
        // Given
        val createdAt = 1_000L

        // When
        val entity = mapper.toEntity(
            group = newGroup,
            createdAt = createdAt,
            ownerUserId = null,
        )

        // Then
        assertEquals(
            GroupEntity(
                id = "group-1",
                name = "Viagem Floripa",
                type = "trip",
                color = "peach",
                currency = "BRL",
                createdBy = null,
                addedAt = 1_000L,
                updatedAt = 1_000L,
                pendingSync = true,
                ownerUserId = null,
                inviteCode = null,
            ),
            entity,
        )
    }

    @Test
    fun `GIVEN three new members WHEN mapping them THEN each is added a millisecond after the previous`() {
        // Given
        val createdAt = 1_000L

        // When
        val entities = mapper.toMemberEntities(
            group = newGroup,
            createdAt = createdAt,
            ownerUserId = null,
        )

        // Then
        assertEquals(listOf("m-1", "m-2", "m-3"), entities.map { it.id })
        assertEquals(listOf(1_000L, 1_001L, 1_002L), entities.map { it.addedAt })
    }

    @Test
    fun `GIVEN a new group WHEN mapping its members THEN they are pending in the group without accounts`() {
        // Given
        val createdAt = 1_000L

        // When
        val entities = mapper.toMemberEntities(
            group = newGroup,
            createdAt = createdAt,
            ownerUserId = null,
        )

        // Then
        assertTrue(entities.all { it.groupId == "group-1" })
        assertTrue(entities.all { it.pendingSync })
        assertTrue(entities.all { it.userId == null })
        assertEquals(listOf(true, false, false), entities.map { it.isMe })
        assertEquals(listOf("Pierre", "Ana", "Bruno"), entities.map { it.name })
        assertEquals(listOf("lilac", "green", "sand"), entities.map { it.avatarColor })
    }

    @Test
    fun `GIVEN a new group of an account WHEN mapping it to an entity THEN that account owns and created it`() {
        // Given
        val ownerUserId = "user-1"

        // When
        val entity = mapper.toEntity(
            group = newGroup,
            createdAt = 1_000L,
            ownerUserId = ownerUserId,
        )

        // Then
        assertEquals("user-1", entity.ownerUserId)
        assertEquals("user-1", entity.createdBy)
        assertTrue(entity.pendingSync)
    }

    @Test
    fun `GIVEN a new group of an account WHEN mapping its members THEN only the creator carries that account`() {
        // Given
        val ownerUserId = "user-1"

        // When
        val entities = mapper.toMemberEntities(
            group = newGroup,
            createdAt = 1_000L,
            ownerUserId = ownerUserId,
        )

        // Then
        assertEquals(listOf("user-1", null, null), entities.map { it.userId })
        assertEquals(listOf(true, false, false), entities.map { it.isMe })
        assertTrue(entities.all { it.pendingSync })
    }

    @Test
    fun `GIVEN a group with its creator WHEN mapping it to a DTO THEN keeps that creator`() {
        // Given
        val entity = groupEntity(
            id = "group-1",
            createdBy = "user-2",
        )

        // When
        val dto = mapper.toDto(entity, userId = "user-1")

        // Then
        assertEquals("user-2", dto.createdBy)
    }

    @Test
    fun `GIVEN a group without a creator WHEN mapping it to a DTO THEN the syncing user is the creator`() {
        // Given
        val entity = groupEntity(
            id = "group-1",
            createdBy = null,
        )

        // When
        val dto = mapper.toDto(entity, userId = "user-1")

        // Then
        assertEquals("user-1", dto.createdBy)
    }

    @Test
    fun `GIVEN a stored group WHEN mapping it to a DTO THEN carries its fields`() {
        // Given
        val entity = groupEntity(
            id = "group-1",
            type = "event",
            color = "sand",
            currency = "ARS",
            addedAt = 5L,
            updatedAt = 9L,
        )

        // When
        val dto = mapper.toDto(entity, userId = "user-1")

        // Then
        assertEquals(
            GroupDto(
                id = "group-1",
                createdBy = "user-1",
                name = "Viagem Floripa",
                type = "event",
                color = "sand",
                currency = "ARS",
                addedAt = 5L,
                updatedAt = 9L,
                inviteCode = null,
            ),
            dto,
        )
    }

    @Test
    fun `GIVEN the member of this device user WHEN mapping it to a DTO THEN carries its fields`() {
        // Given
        val entity = memberEntity(
            id = "m-1",
            name = "Ana",
            avatarColor = "green",
            userId = "user-2",
            isMe = true,
            addedAt = 3L,
        )

        // When
        val dto = mapper.toDto(entity)

        // Then
        assertEquals(
            GroupMemberDto(
                id = "m-1",
                groupId = "group-1",
                userId = "user-2",
                name = "Ana",
                avatarColor = "green",
                addedAt = 3L,
                updatedAt = 1L,
                inviteCode = null,
            ),
            dto,
        )
    }

    @Test
    fun `GIVEN a group from the server WHEN mapping it to an entity THEN it is synced and owned by the syncing user`() {
        // Given
        val dto = groupDto(
            id = "group-1",
            createdBy = "user-2",
        )

        // When
        val entity = mapper.toEntity(dto, ownerUserId = "user-1")

        // Then
        assertEquals(
            GroupEntity(
                id = "group-1",
                name = "Viagem Floripa",
                type = "trip",
                color = "blue",
                currency = "BRL",
                createdBy = "user-2",
                addedAt = 1L,
                updatedAt = 7L,
                pendingSync = false,
                ownerUserId = "user-1",
                inviteCode = null,
            ),
            entity,
        )
    }

    @Test
    fun `GIVEN the syncing user as a member on the server WHEN mapping it to an entity THEN is me`() {
        // Given
        val dto = memberDto(
            id = "m-1",
            userId = "user-1",
        )

        // When
        val entity = mapper.toEntity(dto, userId = "user-1")

        // Then
        assertTrue(entity.isMe)
        assertEquals("user-1", entity.userId)
        assertFalse(entity.pendingSync)
    }

    @Test
    fun `GIVEN another user as a member on the server WHEN mapping it to an entity THEN is not me`() {
        // Given
        val dto = memberDto(
            id = "m-1",
            userId = "user-2",
        )

        // When
        val entity = mapper.toEntity(dto, userId = "user-1")

        // Then
        assertFalse(entity.isMe)
        assertEquals("user-2", entity.userId)
    }

    @Test
    fun `GIVEN a member on the server that is just a name WHEN mapping it to an entity THEN is not me`() {
        // Given
        val dto = memberDto(
            id = "m-1",
            userId = null,
        )

        // When
        val entity = mapper.toEntity(dto, userId = "user-1")

        // Then
        assertFalse(entity.isMe)
    }

    @Test
    fun `GIVEN a member on the server WHEN mapping it to an entity THEN carries its fields synced`() {
        // Given
        val dto = memberDto(
            id = "m-1",
            userId = "user-2",
        )

        // When
        val entity = mapper.toEntity(dto, userId = "user-1")

        // Then
        assertEquals(
            GroupMemberEntity(
                id = "m-1",
                groupId = "group-1",
                name = "Ana",
                avatarColor = "green",
                userId = "user-2",
                isMe = false,
                addedAt = 2L,
                updatedAt = 8L,
                pendingSync = false,
                isRemoved = false,
                inviteCode = null,
            ),
            entity,
        )
    }

    @Test
    fun `GIVEN a stored group with an invite code WHEN mapping to the domain THEN carries the code`() {
        // Given
        val groups = listOf(
            groupEntity(
                id = "group-1",
                inviteCode = "aB3dE5gH",
            ),
        )

        // When
        val mapped = mapper.toDomain(groups, emptyList()).single()

        // Then
        assertEquals("aB3dE5gH", mapped.inviteCode)
    }

    @Test
    fun `GIVEN a stored group without an invite code WHEN mapping to the domain THEN has no code`() {
        // Given
        val groups = listOf(groupEntity(id = "group-1"))

        // When
        val mapped = mapper.toDomain(groups, emptyList()).single()

        // Then
        assertNull(mapped.inviteCode)
    }

    @Test
    fun `GIVEN a group from the server with its invite code WHEN mapping it to an entity THEN keeps the code`() {
        // Given
        val dto = groupDto(
            id = "group-1",
            createdBy = "user-2",
            inviteCode = "aB3dE5gH",
        )

        // When
        val entity = mapper.toEntity(dto, ownerUserId = "user-1")

        // Then
        assertEquals("aB3dE5gH", entity.inviteCode)
    }

    @Test
    fun `GIVEN a group that already has an invite code WHEN mapping it to an entity THEN keeps the code`() {
        // Given
        val group = newGroup.copy(inviteCode = "aB3dE5gH")

        // When
        val entity = mapper.toEntity(
            group = group,
            createdAt = 1_000L,
            ownerUserId = "user-1",
        )

        // Then
        assertEquals("aB3dE5gH", entity.inviteCode)
    }

    @Test
    fun `GIVEN a stored group with an invite code WHEN mapping it to a DTO THEN sends no code`() {
        // Given
        val entity = groupEntity(
            id = "group-1",
            inviteCode = "aB3dE5gH",
        )

        // When
        val dto = mapper.toDto(entity, userId = "user-1")

        // Then
        assertNull(dto.inviteCode)
    }

    @Test
    fun `GIVEN a member removed here WHEN mapping to the domain THEN the group no longer shows them`() {
        // Given
        val groups = listOf(groupEntity(id = "group-1"))
        val members = listOf(
            memberEntity(id = "m-1"),
            memberEntity(
                id = "m-2",
                pendingSync = true,
                isRemoved = true,
            ),
            memberEntity(id = "m-3"),
        )

        // When
        val mapped = mapper.toDomain(groups, members).single()

        // Then
        assertEquals(listOf("m-1", "m-3"), mapped.members.map { it.id })
    }

    @Test
    fun `GIVEN a synced group with only a removal not up yet WHEN mapping to the domain THEN is pending`() {
        // Given
        val groups = listOf(
            groupEntity(
                id = "group-1",
                pendingSync = false,
            ),
        )
        val members = listOf(
            memberEntity(
                id = "m-1",
                pendingSync = false,
            ),
            memberEntity(
                id = "m-2",
                pendingSync = true,
                isRemoved = true,
            ),
        )

        // When
        val mapped = mapper.toDomain(groups, members).single()

        // Then
        assertTrue(mapped.isPending)
        assertEquals(listOf("m-1"), mapped.members.map { it.id })
    }

    @Test
    fun `GIVEN a person added to a group WHEN mapping them THEN pending with no account and not removed`() {
        // Given
        val member = GroupMember(
            id = "m-4",
            name = "Carla",
            avatarColor = AvatarColor.SAND,
            isMe = false,
            hasJoined = false,
            inviteCode = null,
        )

        // When
        val entity = mapper.toMemberEntity(
            groupId = "group-1",
            member = member,
            addedAt = 5_000L,
        )

        // Then
        assertEquals(
            GroupMemberEntity(
                id = "m-4",
                groupId = "group-1",
                name = "Carla",
                avatarColor = "sand",
                userId = null,
                isMe = false,
                addedAt = 5_000L,
                updatedAt = 5_000L,
                pendingSync = true,
                isRemoved = false,
                inviteCode = null,
            ),
            entity,
        )
    }

    @Test
    fun `GIVEN a new group WHEN mapping its members THEN none of them is removed`() {
        // Given
        val createdAt = 1_000L

        // When
        val entities = mapper.toMemberEntities(
            group = newGroup,
            createdAt = createdAt,
            ownerUserId = "user-1",
        )

        // Then
        assertTrue(entities.none { it.isRemoved })
    }

    @Test
    fun `GIVEN a member on the server with its invite code WHEN mapping it to an entity THEN keeps the code`() {
        // Given
        val dto = memberDto(
            id = "m-1",
            userId = null,
            inviteCode = "Xy7kQ2",
        )

        // When
        val entity = mapper.toEntity(dto, userId = "user-1")

        // Then
        assertEquals("Xy7kQ2", entity.inviteCode)
    }

    @Test
    fun `GIVEN a stored member with an invite code WHEN mapping to the domain THEN the member carries the code`() {
        // Given
        val groups = listOf(groupEntity(id = "group-1"))
        val members = listOf(
            memberEntity(
                id = "m-1",
                inviteCode = "Xy7kQ2",
            ),
        )

        // When
        val mapped = mapper.toDomain(groups, members).single()

        // Then
        assertEquals("Xy7kQ2", mapped.members.single().inviteCode)
    }

    @Test
    fun `GIVEN a stored member without an invite code WHEN mapping to the domain THEN the member has no code`() {
        // Given
        val groups = listOf(groupEntity(id = "group-1"))
        val members = listOf(memberEntity(id = "m-1"))

        // When
        val mapped = mapper.toDomain(groups, members).single()

        // Then
        assertNull(mapped.members.single().inviteCode)
    }

    @Test
    fun `GIVEN a stored member with an invite code WHEN mapping it to a DTO THEN sends no code`() {
        // Given
        val entity = memberEntity(
            id = "m-1",
            inviteCode = "Xy7kQ2",
        )

        // When
        val dto = mapper.toDto(entity)

        // Then
        assertNull(dto.inviteCode)
    }

    @Test
    fun `GIVEN a person with a code added to a group WHEN mapping them THEN the new row has no code`() {
        // Given
        val member = GroupMember(
            id = "m-4",
            name = "Carla",
            avatarColor = AvatarColor.SAND,
            isMe = false,
            hasJoined = false,
            inviteCode = "Xy7kQ2",
        )

        // When
        val entity = mapper.toMemberEntity(
            groupId = "group-1",
            member = member,
            addedAt = 5_000L,
        )

        // Then
        assertNull(entity.inviteCode)
    }

    @Test
    fun `GIVEN a new group whose members carry codes WHEN mapping its members THEN none of them has a code`() {
        // Given
        val group = newGroup.copy(members = newGroup.members.map { it.copy(inviteCode = "Xy7kQ2") })

        // When
        val entities = mapper.toMemberEntities(
            group = group,
            createdAt = 1_000L,
            ownerUserId = "user-1",
        )

        // Then
        assertEquals(listOf(null, null, null), entities.map { it.inviteCode })
    }

    @Test
    fun `GIVEN a group this device account created WHEN mapping to the domain THEN is created by me`() {
        // Given
        val groups = listOf(
            groupEntity(
                id = "group-1",
                createdBy = "user-1",
                ownerUserId = "user-1",
            ),
        )

        // When
        val mapped = mapper.toDomain(groups, emptyList()).single()

        // Then
        assertTrue(mapped.isCreatedByMe)
    }

    @Test
    fun `GIVEN a group made here before any account WHEN mapping to the domain THEN is created by me`() {
        // Given
        val groups = listOf(
            groupEntity(
                id = "group-1",
                createdBy = null,
                ownerUserId = null,
                pendingSync = true,
            ),
        )

        // When
        val mapped = mapper.toDomain(groups, emptyList()).single()

        // Then
        assertTrue(mapped.isCreatedByMe)
    }

    @Test
    fun `GIVEN a group someone else created WHEN mapping to the domain THEN is not created by me`() {
        // Given
        val groups = listOf(
            groupEntity(
                id = "group-1",
                createdBy = "user-2",
                ownerUserId = "user-1",
            ),
        )

        // When
        val mapped = mapper.toDomain(groups, emptyList()).single()

        // Then
        assertFalse(mapped.isCreatedByMe)
    }

    @Test
    fun `GIVEN a group whose creator deleted their account WHEN mapping to the domain THEN is not created by me`() {
        // Given
        val groups = listOf(
            groupEntity(
                id = "group-1",
                createdBy = null,
                ownerUserId = "user-1",
            ),
        )

        // When
        val mapped = mapper.toDomain(groups, emptyList()).single()

        // Then
        assertFalse(mapped.isCreatedByMe)
    }

    @Test
    fun `GIVEN another member someone joined as WHEN mapping it to a DTO THEN sends no account and keeps the rest`() {
        // Given
        val entity = memberEntity(
            id = "m-2",
            name = "Carla",
            avatarColor = "sand",
            userId = "user-2",
            isMe = false,
            addedAt = 4L,
        )

        // When
        val dto = mapper.toDto(entity)

        // Then
        assertEquals(
            GroupMemberDto(
                id = "m-2",
                groupId = "group-1",
                userId = null,
                name = "Carla",
                avatarColor = "sand",
                addedAt = 4L,
                updatedAt = 1L,
                inviteCode = null,
            ),
            dto,
        )
    }

    @Test
    fun `GIVEN a member that is just a name WHEN mapping it to a DTO THEN sends no account`() {
        // Given
        val entity = memberEntity(
            id = "m-2",
            userId = null,
            isMe = false,
        )

        // When
        val dto = mapper.toDto(entity)

        // Then
        assertNull(dto.userId)
    }

    @Test
    fun `GIVEN this device user before any account WHEN mapping it to a DTO THEN sends no account`() {
        // Given
        val entity = memberEntity(
            id = "m-1",
            userId = null,
            isMe = true,
        )

        // When
        val dto = mapper.toDto(entity)

        // Then
        assertNull(dto.userId)
    }

    private val newGroup = Group(
        id = "group-1",
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.PEACH,
        currency = GroupCurrency.BRL,
        members = listOf(
            GroupMember(
                id = "m-1",
                name = "Pierre",
                avatarColor = AvatarColor.LILAC,
                isMe = true,
                hasJoined = true,
                inviteCode = null,
            ),
            GroupMember(
                id = "m-2",
                name = "Ana",
                avatarColor = AvatarColor.GREEN,
                isMe = false,
                hasJoined = false,
                inviteCode = null,
            ),
            GroupMember(
                id = "m-3",
                name = "Bruno",
                avatarColor = AvatarColor.SAND,
                isMe = false,
                hasJoined = false,
                inviteCode = null,
            ),
        ),
        isPending = true,
        inviteCode = null,
        isCreatedByMe = true,
        updatedAt = 0L,
    )

    private fun groupEntity(
        id: String,
        type: String = "trip",
        color: String = "blue",
        currency: String = "BRL",
        createdBy: String? = "user-1",
        addedAt: Long = 1L,
        updatedAt: Long = 1L,
        pendingSync: Boolean = false,
        ownerUserId: String? = "user-1",
        inviteCode: String? = null,
    ): GroupEntity = GroupEntity(
        id = id,
        name = "Viagem Floripa",
        type = type,
        color = color,
        currency = currency,
        createdBy = createdBy,
        addedAt = addedAt,
        updatedAt = updatedAt,
        pendingSync = pendingSync,
        ownerUserId = ownerUserId,
        inviteCode = inviteCode,
    )

    private fun memberEntity(
        id: String,
        groupId: String = "group-1",
        name: String = "Ana",
        avatarColor: String = "green",
        userId: String? = null,
        isMe: Boolean = false,
        addedAt: Long = 1L,
        pendingSync: Boolean = false,
        isRemoved: Boolean = false,
        inviteCode: String? = null,
    ): GroupMemberEntity = GroupMemberEntity(
        id = id,
        groupId = groupId,
        name = name,
        avatarColor = avatarColor,
        userId = userId,
        isMe = isMe,
        addedAt = addedAt,
        updatedAt = 1L,
        pendingSync = pendingSync,
        isRemoved = isRemoved,
        inviteCode = inviteCode,
    )

    private fun groupDto(
        id: String,
        createdBy: String?,
        inviteCode: String? = null,
    ): GroupDto = GroupDto(
        id = id,
        createdBy = createdBy,
        name = "Viagem Floripa",
        type = "trip",
        color = "blue",
        currency = "BRL",
        addedAt = 1L,
        updatedAt = 7L,
        inviteCode = inviteCode,
    )

    private fun memberDto(
        id: String,
        userId: String?,
        inviteCode: String? = null,
    ): GroupMemberDto = GroupMemberDto(
        id = id,
        groupId = "group-1",
        userId = userId,
        name = "Ana",
        avatarColor = "green",
        addedAt = 2L,
        updatedAt = 8L,
        inviteCode = inviteCode,
    )
}
