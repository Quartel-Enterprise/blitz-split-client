package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.DeletedBillNotice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeletedBillNoticeTest {
    private val notice = DeletedBillNotice()

    @Test
    fun `GIVEN no bill was deleted WHEN a group screen reads the notice THEN there is none`() {
        // Given
        val freshNotice = notice

        // When
        val groupId = freshNotice.groupId.value

        // Then
        assertNull(groupId)
    }

    @Test
    fun `GIVEN an open bill of a group WHEN its screen posts that it was deleted THEN that group owes the notice`() {
        // Given
        val groupId = GROUP_1

        // When
        notice.post(groupId)

        // Then
        assertEquals(GROUP_1, notice.groupId.value)
    }

    @Test
    fun `GIVEN a notice for a group WHEN that group shows it THEN it is cleared so it shows only once`() {
        // Given
        notice.post(GROUP_1)

        // When
        notice.consume(GROUP_1)

        // Then
        assertNull(notice.groupId.value)
    }

    @Test
    fun `GIVEN a notice for a group WHEN another group shows its screen THEN the notice keeps waiting`() {
        // Given
        notice.post(GROUP_1)

        // When
        notice.consume(GROUP_2)

        // Then
        assertEquals(GROUP_1, notice.groupId.value)
    }

    @Test
    fun `GIVEN a notice for a group WHEN a bill of another group is deleted THEN only the latest group owes it`() {
        // Given
        notice.post(GROUP_1)

        // When
        notice.post(GROUP_2)

        // Then
        assertEquals(GROUP_2, notice.groupId.value)
    }

    private companion object {
        const val GROUP_1 = "group-1"
        const val GROUP_2 = "group-2"
    }
}
