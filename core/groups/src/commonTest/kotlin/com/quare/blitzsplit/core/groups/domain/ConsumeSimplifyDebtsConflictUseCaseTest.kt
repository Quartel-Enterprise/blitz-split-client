package com.quare.blitzsplit.core.groups.domain

import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ConsumeSimplifyDebtsConflictUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConsumeSimplifyDebtsConflictUseCaseTest {
    private val lostSwitch = Group(
        id = "group-1",
        name = "Apê 302",
        type = GroupType.HOME,
        color = AvatarColor.BLUE,
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
        simplifyDebtsChangedBy = "user-2",
        simplifyDebtsConflictBy = "user-2",
    )
    private val repository = FakeGroupRepository(listOf(lostSwitch))
    private val useCase = ConsumeSimplifyDebtsConflictUseCase(repository)

    @Test
    fun `GIVEN a group whose switch lost to someone else WHEN consuming the notice THEN the group no longer has it`() =
        runTest {
            // Given
            val groupId = lostSwitch.id

            // When
            val result = useCase(groupId)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf("group-1"), repository.consumedSimplifyConflictGroupIds)
            assertNull(
                repository.groups.value
                    .single()
                    .simplifyDebtsConflictBy,
            )
        }
}
