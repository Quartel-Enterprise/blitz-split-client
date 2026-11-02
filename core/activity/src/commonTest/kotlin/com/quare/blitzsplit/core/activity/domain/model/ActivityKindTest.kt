package com.quare.blitzsplit.core.activity.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ActivityKindTest {
    @Test
    fun `GIVEN every code the server records WHEN reading it THEN each is a known kind`() {
        // Given
        val codes = listOf(
            "bill_created",
            "bill_edited",
            "bill_deleted",
            "payment_recorded",
            "payment_undone",
            "group_created",
            "group_edited",
            "group_deleted",
            "invite_link_changed",
            "member_added",
            "member_joined",
            "member_left",
            "member_removed",
            "member_released",
            "member_renamed",
            "charge_sent",
            "comment_added",
            "attachment_added",
            "attachment_removed",
            "bill_link_changed",
        )

        // When
        val kinds = codes.map(ActivityKind::fromCodeOrNull)

        // Then
        assertEquals(ActivityKind.entries.toList(), kinds)
    }

    @Test
    fun `GIVEN a kind a newer server records WHEN reading it THEN it is unknown`() {
        // Given
        val code = "poll_created"

        // When
        val kind = ActivityKind.fromCodeOrNull(code)

        // Then
        assertNull(kind)
    }

    @Test
    fun `GIVEN a charge sent WHEN reading its type THEN it is under Cobrancas`() {
        // Given
        val kind = ActivityKind.CHARGE_SENT

        // When
        val type = kind.type

        // Then
        assertEquals(ActivityType.CHARGES, type)
    }

    @Test
    fun `GIVEN a comment added WHEN reading its kind THEN it is under Comentarios in the feed and about a bill`() {
        // Given
        val code = "comment_added"

        // When
        val kind = ActivityKind.fromCodeOrNull(code)

        // Then
        assertEquals(ActivityKind.COMMENT_ADDED, kind)
        assertEquals(ActivityType.COMMENTS, kind?.type)
        assertEquals(true, kind?.isInFeed)
        assertEquals(true, kind?.isAboutBill)
    }

    @Test
    fun `GIVEN photos and the share link of a bill WHEN reading their kinds THEN only its history shows them`() {
        // Given
        val kinds = listOf(
            ActivityKind.ATTACHMENT_ADDED,
            ActivityKind.ATTACHMENT_REMOVED,
            ActivityKind.BILL_LINK_CHANGED,
        )

        // When
        val shownInFeed = kinds.filter { it.isInFeed }

        // Then
        assertTrue(shownInFeed.isEmpty())
        assertTrue(kinds.all { it.type == ActivityType.BILLS && it.isAboutBill })
    }

    @Test
    fun `GIVEN every kind but photos and the share link WHEN reading whether the feed shows it THEN it does`() {
        // Given
        val historyOnly = setOf(
            ActivityKind.ATTACHMENT_ADDED,
            ActivityKind.ATTACHMENT_REMOVED,
            ActivityKind.BILL_LINK_CHANGED,
        )

        // When
        val inFeed = ActivityKind.entries.filter { it.isInFeed }

        // Then
        assertEquals(ActivityKind.entries - historyOnly, inFeed)
    }

    @Test
    fun `GIVEN payments people groups and charges WHEN reading whether they are about a bill THEN none is`() {
        // Given
        val types = setOf(ActivityType.PAYMENTS, ActivityType.PEOPLE_AND_GROUPS, ActivityType.CHARGES)
        val kinds = ActivityKind.entries.filter { it.type in types }

        // When
        val aboutBill = kinds.filter { it.isAboutBill }

        // Then
        assertTrue(kinds.isNotEmpty())
        assertTrue(aboutBill.isEmpty())
    }

    @Test
    fun `GIVEN a bill created edited or deleted WHEN reading whether it is about a bill THEN it is`() {
        // Given
        val kinds = listOf(ActivityKind.BILL_CREATED, ActivityKind.BILL_EDITED, ActivityKind.BILL_DELETED)

        // When
        val notAboutBill = kinds.filterNot { it.isAboutBill }

        // Then
        assertTrue(notAboutBill.isEmpty())
        assertFalse(kinds.any { it.type != ActivityType.BILLS })
    }
}
