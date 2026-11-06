package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveRecentGroupIdUseCase
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObserveRecentGroupIdUseCaseTest {
    private val byActivity = MutableStateFlow<List<Group>>(emptyList())
    private val useCase = ObserveRecentGroupIdUseCase { byActivity }

    @Test
    fun `GIVEN no group WHEN observing THEN there is no recent group`() = runTest {
        // Given
        byActivity.value = emptyList()

        // When
        val groupId = useCase().first()

        // Then
        assertNull(groupId)
    }

    @Test
    fun `GIVEN the groups reordered behind the first WHEN observing THEN emits only when the first changes`() =
        runTest(UnconfinedTestDispatcher()) {
            // Given
            byActivity.value = listOf(group("trip"), group("home"))
            val emitted = mutableListOf<String?>()
            val job = launch { useCase().take(2).toList(emitted) }

            // When
            byActivity.value = listOf(group("trip"), group("home"), group("soccer"))
            byActivity.value = listOf(group("home"), group("trip"), group("soccer"))
            job.join()

            // Then
            assertEquals(listOf<String?>("trip", "home"), emitted)
        }

    private fun group(id: String): Group = Group(
        id = id,
        name = id,
        type = GroupType.TRIP,
        color = AvatarColor.GREEN,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.BRL,
        members = emptyList(),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
        simplifiesDebts = false,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = null,
        simplifyDebtsConflictBy = null,
    )
}
