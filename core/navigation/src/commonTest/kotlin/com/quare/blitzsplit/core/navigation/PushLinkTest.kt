package com.quare.blitzsplit.core.navigation

import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.HomeTab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PushLinkTest {
    private val billRoute = Route.BillDetail(GROUP_ID, TARGET_ID)

    @Test
    fun `GIVEN a link with every parameter WHEN reading it THEN keeps the kind event group and target`() {
        // Given
        val url = "https://blitzsplit.com/n?k=bill_created&e=$EVENT_ID&g=$GROUP_ID&t=$TARGET_ID"

        // When
        val link = PushLink.of(url)

        // Then
        assertEquals(PushLink("bill_created", EVENT_ID, GROUP_ID, TARGET_ID), link)
    }

    @Test
    fun `GIVEN the parameters in another order WHEN reading the link THEN keeps each one`() {
        // Given
        val url = "https://blitzsplit.com/n?t=$TARGET_ID&g=$GROUP_ID&e=$EVENT_ID&k=bill_edited"

        // When
        val link = PushLink.of(url)

        // Then
        assertEquals(PushLink("bill_edited", EVENT_ID, GROUP_ID, TARGET_ID), link)
    }

    @Test
    fun `GIVEN the path with a trailing slash WHEN reading the link THEN reads it`() {
        // Given
        val url = "https://blitzsplit.com/n/?k=charge_sent&g=$GROUP_ID"

        // When
        val link = PushLink.of(url)

        // Then
        assertEquals(
            PushLink(
                "charge_sent",
                eventId = null,
                GROUP_ID,
                targetId = null,
            ),
            link,
        )
    }

    @Test
    fun `GIVEN an http link WHEN reading it THEN reads it`() {
        // Given
        val url = "http://blitzsplit.com/n?k=member_joined&g=$GROUP_ID"

        // When
        val link = PushLink.of(url)

        // Then
        assertEquals(
            PushLink(
                "member_joined",
                eventId = null,
                GROUP_ID,
                targetId = null,
            ),
            link,
        )
    }

    @Test
    fun `GIVEN a link on localhost WHEN reading it THEN reads it as on blitzsplit dot com`() {
        // Given
        val url = "http://localhost:5173/n?k=payment_undone&g=$GROUP_ID"

        // When
        val link = PushLink.of(url)

        // Then
        assertEquals(
            PushLink(
                "payment_undone",
                eventId = null,
                GROUP_ID,
                targetId = null,
            ),
            link,
        )
    }

    @Test
    fun `GIVEN a link on a preview host WHEN reading it THEN reads it`() {
        // Given
        val url = "https://preview-42.blitzsplit.pages.dev/n?k=added_to_group&g=$GROUP_ID"

        // When
        val link = PushLink.of(url)

        // Then
        assertEquals(
            PushLink(
                "added_to_group",
                eventId = null,
                GROUP_ID,
                targetId = null,
            ),
            link,
        )
    }

    @Test
    fun `GIVEN spaces and a line break around the link WHEN reading it THEN ignores them`() {
        // Given
        val url = "  https://blitzsplit.com/n?k=comment_added&g=$GROUP_ID&t=$TARGET_ID\n"

        // When
        val link = PushLink.of(url)

        // Then
        assertEquals(PushLink("comment_added", eventId = null, GROUP_ID, TARGET_ID), link)
    }

    @Test
    fun `GIVEN only the kind WHEN reading the link THEN the event group and target are null`() {
        // Given
        val url = "https://blitzsplit.com/n?k=settle_reminders"

        // When
        val link = PushLink.of(url)

        // Then
        assertEquals(
            PushLink(
                "settle_reminders",
                eventId = null,
                groupId = null,
                targetId = null,
            ),
            link,
        )
    }

    @Test
    fun `GIVEN empty event group and target WHEN reading the link THEN they count as missing`() {
        // Given
        val url = "https://blitzsplit.com/n?k=bill_created&e=&g=&t="

        // When
        val link = PushLink.of(url)

        // Then
        assertEquals(
            PushLink(
                "bill_created",
                eventId = null,
                groupId = null,
                targetId = null,
            ),
            link,
        )
    }

    @Test
    fun `GIVEN a fragment after the parameters WHEN reading the link THEN the fragment is not part of the last one`() {
        // Given
        val url = "https://blitzsplit.com/n?k=bill_created&g=$GROUP_ID&t=$TARGET_ID#top"

        // When
        val link = PushLink.of(url)

        // Then
        assertEquals(PushLink("bill_created", eventId = null, GROUP_ID, TARGET_ID), link)
    }

    @Test
    fun `GIVEN a link without the kind WHEN reading it THEN is not a push link`() {
        // Given
        val url = "https://blitzsplit.com/n?e=$EVENT_ID&g=$GROUP_ID&t=$TARGET_ID"

        // When
        val link = PushLink.of(url)

        // Then
        assertNull(link)
    }

    @Test
    fun `GIVEN an empty kind WHEN reading the link THEN is not a push link`() {
        // Given
        val url = "https://blitzsplit.com/n?k=&g=$GROUP_ID"

        // When
        val link = PushLink.of(url)

        // Then
        assertNull(link)
    }

    @Test
    fun `GIVEN the path without parameters WHEN reading the link THEN is not a push link`() {
        // Given
        val url = "https://blitzsplit.com/n"

        // When
        val link = PushLink.of(url)

        // Then
        assertNull(link)
    }

    @Test
    fun `GIVEN an invite link WHEN reading it as a push link THEN is null`() {
        // Given
        val url = "https://blitzsplit.com/g/K7QM4XPA"

        // When
        val link = PushLink.of(url)

        // Then
        assertNull(link)
    }

    @Test
    fun `GIVEN a bill link WHEN reading it as a push link THEN is null`() {
        // Given
        val url = "https://blitzsplit.com/c/K7QM4XPA"

        // When
        val link = PushLink.of(url)

        // Then
        assertNull(link)
    }

    @Test
    fun `GIVEN the sign in callback WHEN reading it as a push link THEN is null`() {
        // Given
        val url = "borarachar://auth-callback?k=bill_created&g=$GROUP_ID"

        // When
        val link = PushLink.of(url)

        // Then
        assertNull(link)
    }

    @Test
    fun `GIVEN a path starting with n WHEN reading it as a push link THEN is null`() {
        // Given
        val url = "https://blitzsplit.com/nx?k=bill_created&g=$GROUP_ID"

        // When
        val link = PushLink.of(url)

        // Then
        assertNull(link)
    }

    @Test
    fun `GIVEN a path under n WHEN reading it as a push link THEN is null`() {
        // Given
        val url = "https://blitzsplit.com/n/other?k=bill_created&g=$GROUP_ID"

        // When
        val link = PushLink.of(url)

        // Then
        assertNull(link)
    }

    @Test
    fun `GIVEN one group settle up reminder WHEN asking its home tab THEN is Grupos`() {
        // Given
        val link = pushLink(kind = "settle_reminder")

        // When
        val tab = link.homeTab

        // Then
        assertEquals(HomeTab.GROUPS, tab)
    }

    @Test
    fun `GIVEN several groups settle up reminder WHEN asking its home tab THEN is Saldos`() {
        // Given
        val link = pushLink(
            kind = "settle_reminders",
            groupId = null,
            targetId = null,
        )

        // When
        val tab = link.homeTab

        // Then
        assertEquals(HomeTab.BALANCES, tab)
    }

    @Test
    fun `GIVEN a bill created WHEN asking its home tab THEN is Atividade`() {
        // Given
        val link = pushLink(kind = "bill_created")

        // When
        val tab = link.homeTab

        // Then
        assertEquals(HomeTab.ACTIVITY, tab)
    }

    @Test
    fun `GIVEN a kind a newer server adds WHEN asking its home tab THEN is Atividade`() {
        // Given
        val link = pushLink(kind = "group_archived")

        // When
        val tab = link.homeTab

        // Then
        assertEquals(HomeTab.ACTIVITY, tab)
    }

    @Test
    fun `GIVEN a bill created without a group WHEN asking what opens above THEN is nothing`() {
        // Given
        val link = pushLink(
            kind = "bill_created",
            groupId = null,
        )

        // When
        val routes = link.routesAbove

        // Then
        assertEquals(emptyList(), routes)
    }

    @Test
    fun `GIVEN several groups settle up reminder WHEN asking what opens above THEN is nothing`() {
        // Given
        val link = pushLink(
            kind = "settle_reminders",
            groupId = null,
            targetId = null,
        )

        // When
        val routes = link.routesAbove

        // Then
        assertEquals(emptyList(), routes)
    }

    @Test
    fun `GIVEN a bill created WHEN asking what opens above THEN is the group Despesas then the bill Conta`() {
        // Given
        val link = pushLink(kind = "bill_created")

        // When
        val routes = link.routesAbove

        // Then
        assertEquals(listOf(groupRoute(GroupTab.EXPENSES), billRoute), routes)
    }

    @Test
    fun `GIVEN a bill edited WHEN asking what opens above THEN is the group Despesas then the bill Conta`() {
        // Given
        val link = pushLink(kind = "bill_edited")

        // When
        val routes = link.routesAbove

        // Then
        assertEquals(listOf(groupRoute(GroupTab.EXPENSES), billRoute), routes)
    }

    @Test
    fun `GIVEN a comment added WHEN asking what opens above THEN is the group Despesas then the bill Conta`() {
        // Given
        val link = pushLink(kind = "comment_added")

        // When
        val routes = link.routesAbove

        // Then
        assertEquals(listOf(groupRoute(GroupTab.EXPENSES), billRoute), routes)
    }

    @Test
    fun `GIVEN a bill created without a target WHEN asking what opens above THEN is only the group Despesas`() {
        // Given
        val link = pushLink(
            kind = "bill_created",
            targetId = null,
        )

        // When
        val routes = link.routesAbove

        // Then
        assertEquals(listOf(groupRoute(GroupTab.EXPENSES)), routes)
    }

    @Test
    fun `GIVEN a charge sent WHEN asking what opens above THEN is the group Saldos`() {
        // Given
        val link = pushLink(kind = "charge_sent")

        // When
        val routes = link.routesAbove

        // Then
        assertEquals(listOf(groupRoute(GroupTab.BALANCES)), routes)
    }

    @Test
    fun `GIVEN one group settle up reminder WHEN asking what opens above THEN is the group Saldos`() {
        // Given
        val link = pushLink(
            kind = "settle_reminder",
            targetId = null,
        )

        // When
        val routes = link.routesAbove

        // Then
        assertEquals(listOf(groupRoute(GroupTab.BALANCES)), routes)
    }

    @Test
    fun `GIVEN added to a group WHEN asking what opens above THEN is the group Membros`() {
        // Given
        val link = pushLink(kind = "added_to_group")

        // When
        val routes = link.routesAbove

        // Then
        assertEquals(listOf(groupRoute(GroupTab.MEMBERS)), routes)
    }

    @Test
    fun `GIVEN a member joined WHEN asking what opens above THEN is the group Membros`() {
        // Given
        val link = pushLink(kind = "member_joined")

        // When
        val routes = link.routesAbove

        // Then
        assertEquals(listOf(groupRoute(GroupTab.MEMBERS)), routes)
    }

    @Test
    fun `GIVEN a bill deleted WHEN asking what opens above THEN is the group Despesas without the bill`() {
        // Given
        val link = pushLink(kind = "bill_deleted")

        // When
        val routes = link.routesAbove

        // Then
        assertEquals(listOf(groupRoute(GroupTab.EXPENSES)), routes)
    }

    @Test
    fun `GIVEN someone paid you WHEN asking what opens above THEN is the group Despesas alone`() {
        // Given
        val link = pushLink(kind = "payment_paid_you")

        // When
        val routes = link.routesAbove

        // Then
        assertEquals(listOf(groupRoute(GroupTab.EXPENSES)), routes)
    }

    @Test
    fun `GIVEN a payment to you WHEN asking what opens above THEN is the group Despesas alone`() {
        // Given
        val link = pushLink(kind = "payment_to_you")

        // When
        val routes = link.routesAbove

        // Then
        assertEquals(listOf(groupRoute(GroupTab.EXPENSES)), routes)
    }

    @Test
    fun `GIVEN your payment received WHEN asking what opens above THEN is the group Despesas alone`() {
        // Given
        val link = pushLink(kind = "payment_received_yours")

        // When
        val routes = link.routesAbove

        // Then
        assertEquals(listOf(groupRoute(GroupTab.EXPENSES)), routes)
    }

    @Test
    fun `GIVEN a payment from you WHEN asking what opens above THEN is the group Despesas alone`() {
        // Given
        val link = pushLink(kind = "payment_from_you")

        // When
        val routes = link.routesAbove

        // Then
        assertEquals(listOf(groupRoute(GroupTab.EXPENSES)), routes)
    }

    @Test
    fun `GIVEN a payment undone WHEN asking what opens above THEN is the group Despesas alone`() {
        // Given
        val link = pushLink(kind = "payment_undone")

        // When
        val routes = link.routesAbove

        // Then
        assertEquals(listOf(groupRoute(GroupTab.EXPENSES)), routes)
    }

    @Test
    fun `GIVEN a kind a newer server adds WHEN asking what opens above THEN is the group Despesas alone`() {
        // Given
        val link = pushLink(kind = "group_archived")

        // When
        val routes = link.routesAbove

        // Then
        assertEquals(listOf(groupRoute(GroupTab.EXPENSES)), routes)
    }

    private fun groupRoute(tab: GroupTab): Route = Route.GroupDetail(
        GROUP_ID,
        joinedAs = null,
        tab = tab,
    )

    private fun pushLink(
        kind: String,
        groupId: String? = GROUP_ID,
        targetId: String? = TARGET_ID,
    ): PushLink = PushLink(kind, EVENT_ID, groupId, targetId)

    private companion object {
        const val EVENT_ID = "event-1"
        const val GROUP_ID = "group-1"
        const val TARGET_ID = "expense-1"
    }
}
