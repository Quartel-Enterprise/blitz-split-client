package com.quare.blitzsplit.core.activity.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
        )

        // When
        val kinds = codes.map(ActivityKind::fromCodeOrNull)

        // Then
        assertEquals(ActivityKind.entries.toList(), kinds)
    }

    @Test
    fun `GIVEN a kind a newer server records WHEN reading it THEN it is unknown`() {
        // Given
        val code = "comment_added"

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
}
