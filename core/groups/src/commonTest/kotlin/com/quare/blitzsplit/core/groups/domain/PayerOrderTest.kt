package com.quare.blitzsplit.core.groups.domain

import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.PayerOrder
import com.quare.blitzsplit.core.model.AvatarColor
import kotlin.test.Test
import kotlin.test.assertEquals

class PayerOrderTest {
    private val ana = member(id = "ana")
    private val me = member(
        id = "me",
        isMe = true,
    )
    private val bruno = member(id = "bruno")
    private val carla = member(id = "carla")

    // Why: the device user is second in the group, so "first" can only come from isMe, not from the group's order.
    private val order = PayerOrder(listOf(ana, me, bruno, carla))

    @Test
    fun `GIVEN the device user paid after others WHEN ordering THEN names the device user first then group order`() {
        // Given
        val payerIds = listOf("carla", "ana", "me")

        // When
        val sorted = order.sort(payerIds)

        // Then
        assertEquals(listOf("me", "ana", "carla"), sorted)
    }

    @Test
    fun `GIVEN the device user did not pay WHEN ordering THEN follows the order they were added to the group`() {
        // Given
        val payerIds = listOf("carla", "bruno", "ana")

        // When
        val sorted = order.sort(payerIds)

        // Then
        assertEquals(listOf("ana", "bruno", "carla"), sorted)
    }

    @Test
    fun `GIVEN payers who left the group WHEN ordering THEN names them after the members in the bill order`() {
        // Given
        val payerIds = listOf("zelia", "carla", "me", "yuri")

        // When
        val sorted = order.sort(payerIds)

        // Then
        assertEquals(listOf("me", "carla", "zelia", "yuri"), sorted)
    }

    @Test
    fun `GIVEN every payer left the group WHEN ordering the payers THEN keeps the bill order`() {
        // Given
        val payerIds = listOf("zelia", "xavier", "yuri")

        // When
        val sorted = order.sort(payerIds)

        // Then
        assertEquals(listOf("zelia", "xavier", "yuri"), sorted)
    }

    @Test
    fun `GIVEN the bill lists a member twice WHEN ordering the payers THEN names each one once`() {
        // Given
        val payerIds = listOf("ana", "me", "ana", "me")

        // When
        val sorted = order.sort(payerIds)

        // Then
        assertEquals(listOf("me", "ana"), sorted)
    }

    @Test
    fun `GIVEN someone who left listed twice WHEN ordering THEN names them once where they first appear`() {
        // Given
        val payerIds = listOf("zelia", "yuri", "zelia")

        // When
        val sorted = order.sort(payerIds)

        // Then
        assertEquals(listOf("zelia", "yuri"), sorted)
    }

    @Test
    fun `GIVEN no device user in the group WHEN ordering THEN follows the group order then whoever left`() {
        // Given
        val withoutMe = PayerOrder(listOf(ana, bruno, carla))
        val payerIds = listOf("yuri", "carla", "ana")

        // When
        val sorted = withoutMe.sort(payerIds)

        // Then
        assertEquals(listOf("ana", "carla", "yuri"), sorted)
    }

    @Test
    fun `GIVEN a group with no members WHEN ordering the payers THEN keeps the bill order`() {
        // Given
        val noMembers = PayerOrder(emptyList())
        val payerIds = listOf("carla", "ana")

        // When
        val sorted = noMembers.sort(payerIds)

        // Then
        assertEquals(listOf("carla", "ana"), sorted)
    }

    @Test
    fun `GIVEN no payers WHEN ordering the payers THEN gives none`() {
        // Given
        val payerIds = emptyList<String>()

        // When
        val sorted = order.sort(payerIds)

        // Then
        assertEquals(emptyList(), sorted)
    }

    @Test
    fun `GIVEN a single payer WHEN ordering the payers THEN gives that payer`() {
        // Given
        val payerIds = listOf("bruno")

        // When
        val sorted = order.sort(payerIds)

        // Then
        assertEquals(listOf("bruno"), sorted)
    }

    @Test
    fun `GIVEN a single payer who left the group WHEN ordering the payers THEN gives that payer`() {
        // Given
        val payerIds = listOf("zelia")

        // When
        val sorted = order.sort(payerIds)

        // Then
        assertEquals(listOf("zelia"), sorted)
    }

    private fun member(
        id: String,
        isMe: Boolean = false,
    ): GroupMember = GroupMember(
        id = id,
        name = id.replaceFirstChar { it.uppercase() },
        avatarColor = AvatarColor.BLUE,
        isMe = isMe,
        hasJoined = true,
        userId = null,
        inviteCode = null,
    )
}
