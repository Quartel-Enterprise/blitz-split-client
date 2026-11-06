package com.quare.blitzsplit.feature.join

import com.quare.blitzsplit.feature.join.presentation.model.NewMemberDraft
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NewMemberDraftTest {
    @Test
    fun `GIVEN a free name WHEN checking the draft THEN can join`() {
        // Given
        val draft = NewMemberDraft(
            name = "Daniel",
        )

        // When
        val canJoin = draft.canJoin

        // Then
        assertTrue(canJoin)
    }

    @Test
    fun `GIVEN a single letter WHEN checking the draft THEN cannot join`() {
        // Given
        val draft = NewMemberDraft(
            name = " J ",
        )

        // When
        val canJoin = draft.canJoin

        // Then
        assertFalse(canJoin)
    }

    @Test
    fun `GIVEN an empty name WHEN checking the draft THEN cannot join`() {
        // Given
        val draft = NewMemberDraft(
            name = "",
        )

        // When
        val canJoin = draft.canJoin

        // Then
        assertFalse(canJoin)
    }

    @Test
    fun `GIVEN only spaces WHEN checking the draft THEN cannot join`() {
        // Given
        val draft = NewMemberDraft(
            name = "   ",
        )

        // When
        val canJoin = draft.canJoin

        // Then
        assertFalse(canJoin)
    }
}
