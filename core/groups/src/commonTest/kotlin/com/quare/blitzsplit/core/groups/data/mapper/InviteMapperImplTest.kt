package com.quare.blitzsplit.core.groups.data.mapper

import com.quare.blitzsplit.core.groups.data.dto.InviteDto
import com.quare.blitzsplit.core.groups.data.dto.InviteMemberDto
import com.quare.blitzsplit.core.groups.data.dto.JoinGroupResultDto
import com.quare.blitzsplit.core.groups.domain.model.Invite
import com.quare.blitzsplit.core.groups.domain.model.InviteMember
import com.quare.blitzsplit.core.groups.domain.model.JoinOutcome
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InviteMapperImplTest {
    private val mapper = InviteMapperImpl()

    @Test
    fun `GIVEN the invite of a group WHEN mapping it THEN keeps the code the group and its members in order`() {
        // Given
        val dto = inviteDto(
            members = listOf(
                memberDto(
                    id = "m-1",
                    name = "Pierre",
                    avatarColor = "peach",
                    hasJoined = true,
                ),
                memberDto(
                    id = "m-2",
                    name = "Ana",
                    avatarColor = "green",
                    hasJoined = false,
                ),
            ),
        )

        // When
        val invite = mapper.toDomain("aB3dE5gH", dto)

        // Then
        assertEquals(
            Invite(
                code = "aB3dE5gH",
                groupId = "group-1",
                groupName = "Viagem Floripa",
                groupType = GroupType.HOME,
                groupColor = AvatarColor.LILAC,
                currency = GroupCurrency.EUR,
                creatorName = "Pierre",
                isMember = false,
                members = listOf(
                    InviteMember(
                        id = "m-1",
                        name = "Pierre",
                        avatarColor = AvatarColor.PEACH,
                        hasJoined = true,
                    ),
                    InviteMember(
                        id = "m-2",
                        name = "Ana",
                        avatarColor = AvatarColor.GREEN,
                        hasJoined = false,
                    ),
                ),
                personalMemberId = null,
                inviterName = null,
            ),
            invite,
        )
    }

    @Test
    fun `GIVEN a group whose creator deleted the account WHEN mapping the invite THEN has no creator name`() {
        // Given
        val dto = inviteDto(creatorName = null)

        // When
        val invite = mapper.toDomain("aB3dE5gH", dto)

        // Then
        assertNull(invite.creatorName)
    }

    @Test
    fun `GIVEN a group this account already joined WHEN mapping the invite THEN it is a member`() {
        // Given
        val dto = inviteDto(isMember = true)

        // When
        val invite = mapper.toDomain("aB3dE5gH", dto)

        // Then
        assertTrue(invite.isMember)
    }

    @Test
    fun `GIVEN a group without members WHEN mapping the invite THEN it has none`() {
        // Given
        val dto = inviteDto(members = emptyList())

        // When
        val invite = mapper.toDomain("aB3dE5gH", dto)

        // Then
        assertEquals(emptyList(), invite.members)
    }

    @Test
    fun `GIVEN a type this version does not know WHEN mapping the invite THEN reads it as other`() {
        // Given
        val dto = inviteDto(type = "wedding")

        // When
        val invite = mapper.toDomain("aB3dE5gH", dto)

        // Then
        assertEquals(GroupType.OTHER, invite.groupType)
    }

    @Test
    fun `GIVEN a group color this version does not know WHEN mapping the invite THEN reads it as the default color`() {
        // Given
        val dto = inviteDto(color = "magenta")

        // When
        val invite = mapper.toDomain("aB3dE5gH", dto)

        // Then
        assertEquals(AvatarColor.default, invite.groupColor)
    }

    @Test
    fun `GIVEN a currency this version does not know WHEN mapping the invite THEN reads it as dollars`() {
        // Given
        val dto = inviteDto(currency = "XYZ")

        // When
        val invite = mapper.toDomain("aB3dE5gH", dto)

        // Then
        assertEquals(GroupCurrency.fallback, invite.currency)
    }

    @Test
    fun `GIVEN a member color this version does not know WHEN mapping the invite THEN reads it as the default color`() {
        // Given
        val dto = inviteDto(members = listOf(memberDto(avatarColor = "magenta")))

        // When
        val invite = mapper.toDomain("aB3dE5gH", dto)

        // Then
        assertEquals(AvatarColor.default, invite.members.single().avatarColor)
    }

    @Test
    fun `GIVEN a member nobody joined as WHEN mapping the invite THEN the member has not joined`() {
        // Given
        val dto = inviteDto(members = listOf(memberDto(hasJoined = false)))

        // When
        val invite = mapper.toDomain("aB3dE5gH", dto)

        // Then
        assertFalse(invite.members.single().hasJoined)
    }

    @Test
    fun `GIVEN the server joined the group WHEN mapping the answer THEN is joined to that group`() {
        // Given
        val dto = JoinGroupResultDto(
            status = "joined",
            groupId = "group-1",
            memberId = "member-9",
        )

        // When
        val outcome = mapper.toDomain(dto)

        // Then
        assertEquals(JoinOutcome.Joined("group-1"), outcome)
    }

    @Test
    fun `GIVEN the account had joined before WHEN mapping the answer THEN is already a member of that group`() {
        // Given
        val dto = JoinGroupResultDto(
            status = "already_member",
            groupId = "group-1",
            memberId = "member-9",
        )

        // When
        val outcome = mapper.toDomain(dto)

        // Then
        assertEquals(JoinOutcome.AlreadyMember("group-1", memberId = "member-9"), outcome)
    }

    @Test
    fun `GIVEN someone else joined as that member first WHEN mapping the answer THEN the member is taken`() {
        // Given
        val dto = JoinGroupResultDto(
            status = "taken",
            groupId = "group-1",
            memberId = null,
        )

        // When
        val outcome = mapper.toDomain(dto)

        // Then
        assertEquals(JoinOutcome.MemberTaken, outcome)
    }

    @Test
    fun `GIVEN that member was removed WHEN mapping the answer THEN the member is gone`() {
        // Given
        val dto = JoinGroupResultDto(
            status = "member_gone",
            groupId = "group-1",
            memberId = null,
        )

        // When
        val outcome = mapper.toDomain(dto)

        // Then
        assertEquals(JoinOutcome.MemberGone, outcome)
    }

    @Test
    fun `GIVEN a member already has the new name WHEN mapping the answer THEN the name is taken`() {
        // Given
        val dto = JoinGroupResultDto(
            status = "name_taken",
            groupId = "group-1",
            memberId = null,
        )

        // When
        val outcome = mapper.toDomain(dto)

        // Then
        assertEquals(JoinOutcome.NameTaken, outcome)
    }

    @Test
    fun `GIVEN a code no group has WHEN mapping the answer THEN the link is invalid`() {
        // Given
        val dto = JoinGroupResultDto(
            status = "invalid",
            groupId = null,
            memberId = null,
        )

        // When
        val outcome = mapper.toDomain(dto)

        // Then
        assertEquals(JoinOutcome.InvalidLink, outcome)
    }

    @Test
    fun `GIVEN a status of a newer server WHEN mapping the answer THEN the link is invalid`() {
        // Given
        val dto = JoinGroupResultDto(
            status = "group_full",
            groupId = "group-1",
            memberId = null,
        )

        // When
        val outcome = mapper.toDomain(dto)

        // Then
        assertEquals(JoinOutcome.InvalidLink, outcome)
    }

    private fun inviteDto(
        type: String = "home",
        color: String = "lilac",
        currency: String = "EUR",
        creatorName: String? = "Pierre",
        isMember: Boolean = false,
        members: List<InviteMemberDto> = listOf(memberDto()),
    ): InviteDto = InviteDto(
        groupId = "group-1",
        name = "Viagem Floripa",
        type = type,
        color = color,
        currency = currency,
        creatorName = creatorName,
        isMember = isMember,
        members = members,
        personalMemberId = null,
        inviterName = null,
    )

    private fun memberDto(
        id: String = "m-1",
        name: String = "Pierre",
        avatarColor: String = "blue",
        hasJoined: Boolean = true,
    ): InviteMemberDto = InviteMemberDto(
        id = id,
        name = name,
        avatarColor = avatarColor,
        hasJoined = hasJoined,
    )
}
