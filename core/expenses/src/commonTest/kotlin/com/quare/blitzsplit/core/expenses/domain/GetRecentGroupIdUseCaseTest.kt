package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.usecase.impl.GetRecentGroupIdUseCase
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetRecentGroupIdUseCaseTest {
    @Test
    fun `GIVEN no group WHEN reading the recent group THEN there is none`() = runTest {
        // Given
        val useCase = GetRecentGroupIdUseCase { flowOf(emptyList()) }

        // When
        val groupId = useCase()

        // Then
        assertNull(groupId)
    }

    @Test
    fun `GIVEN groups in the order Home lists them WHEN reading the recent group THEN it is the first`() = runTest {
        // Given
        val useCase = GetRecentGroupIdUseCase { flowOf(listOf(group("home"), group("trip"))) }

        // When
        val groupId = useCase()

        // Then
        assertEquals("home", groupId)
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
