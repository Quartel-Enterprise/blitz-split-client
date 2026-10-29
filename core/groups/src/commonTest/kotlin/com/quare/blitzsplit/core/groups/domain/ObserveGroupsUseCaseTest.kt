package com.quare.blitzsplit.core.groups.domain

import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupsUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveGroupsUseCaseTest {
    private val repository = FakeGroupRepository()
    private val useCase = ObserveGroupsUseCase(repository)

    @Test
    fun `GIVEN a stored group WHEN observing the groups THEN emits it`() = runTest {
        // Given
        repository.groups.value = listOf(trip)

        // When
        val groups = useCase().first()

        // Then
        assertEquals(listOf(trip), groups)
    }

    private val trip = Group(
        id = "group-1",
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.BLUE,
        currency = GroupCurrency.BRL,
        members = emptyList(),
        isPending = false,
    )
}
