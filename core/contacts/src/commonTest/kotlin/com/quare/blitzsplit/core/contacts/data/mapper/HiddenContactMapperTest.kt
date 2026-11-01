package com.quare.blitzsplit.core.contacts.data.mapper

import com.quare.blitzsplit.core.contacts.data.dto.GuestMergeDto
import com.quare.blitzsplit.core.contacts.data.dto.HiddenContactDto
import com.quare.blitzsplit.core.contacts.domain.model.GuestMerge
import com.quare.blitzsplit.core.contacts.domain.model.HiddenContact
import com.quare.blitzsplit.core.provider.room.entity.HiddenContactEntity
import kotlin.test.Test
import kotlin.test.assertEquals

class HiddenContactMapperTest {
    @Test
    fun `GIVEN a stored row WHEN reading it THEN keeps who when and whether hidden`() {
        // Given
        val entity = entity()

        // When
        val hidden = HiddenContactMapper.toDomain(entity)

        // Then
        assertEquals(
            HiddenContact(
                "user:carla",
                isHidden = true,
                hiddenAt = 40L,
            ),
            hidden,
        )
    }

    @Test
    fun `GIVEN a stored row WHEN sending it up THEN goes as the given user with its version`() {
        // Given
        val entity = entity(ownerUserId = null)

        // When
        val dto = HiddenContactMapper.toDto(entity, userId = "user-1")

        // Then
        assertEquals(
            HiddenContactDto(
                userId = "user-1",
                personKey = "user:carla",
                isHidden = true,
                hiddenAt = 40L,
                updatedAt = 50L,
            ),
            dto,
        )
    }

    @Test
    fun `GIVEN a row from the server WHEN storing it THEN it is synced and belongs to the given user`() {
        // Given
        val dto = HiddenContactDto(
            userId = "user-1",
            personKey = "user:carla",
            isHidden = true,
            hiddenAt = 40L,
            updatedAt = 50L,
        )

        // When
        val stored = HiddenContactMapper.toEntity(dto, ownerUserId = "user-1")

        // Then
        assertEquals(
            entity(
                pendingSync = false,
                ownerUserId = "user-1",
            ),
            stored,
        )
    }

    @Test
    fun `GIVEN a merge request from the server WHEN reading it THEN carries the guest personal link`() {
        // Given
        val dto = GuestMergeDto(
            id = "merge-1",
            memberId = "diego-ape",
            memberIds = listOf("diego-ape", "diego-futebol"),
            groupId = "ape",
            inviteCode = "Code1234",
            memberInviteCode = "dg1234",
            requestedAt = 100L,
        )

        // When
        val merge = HiddenContactMapper.toDomain(dto)

        // Then
        assertEquals(
            GuestMerge(
                id = "merge-1",
                memberIds = listOf("diego-ape", "diego-futebol"),
                link = "https://blitzsplit.com/g/Code1234/dg1234",
                requestedAt = 100L,
            ),
            merge,
        )
    }

    private fun entity(
        pendingSync: Boolean = true,
        ownerUserId: String? = "user-1",
    ): HiddenContactEntity = HiddenContactEntity(
        personKey = "user:carla",
        isHidden = true,
        hiddenAt = 40L,
        updatedAt = 50L,
        pendingSync = pendingSync,
        ownerUserId = ownerUserId,
    )
}
