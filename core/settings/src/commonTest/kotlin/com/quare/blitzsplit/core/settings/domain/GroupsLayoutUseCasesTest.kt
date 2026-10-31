package com.quare.blitzsplit.core.settings.domain

import com.quare.blitzsplit.core.model.GroupGridSize
import com.quare.blitzsplit.core.model.GroupsDisplay
import com.quare.blitzsplit.core.model.GroupsLayout
import com.quare.blitzsplit.core.settings.domain.usecase.impl.ObserveGroupsLayoutUseCase
import com.quare.blitzsplit.core.settings.domain.usecase.impl.UpdateGroupsLayoutUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GroupsLayoutUseCasesTest {
    private lateinit var repository: FakeDeviceSettingsRepository
    private lateinit var observeGroupsLayout: ObserveGroupsLayoutUseCase
    private lateinit var updateGroupsLayout: UpdateGroupsLayoutUseCase

    @Test
    fun `GIVEN the large grid picked on this device WHEN observing the layout THEN is the large grid`() = runTest {
        // Given
        prepareScenario()
        repository.layout.value = GroupsLayout(GroupsDisplay.GRID, GroupGridSize.LARGE)

        // When
        val layout = observeGroupsLayout().first()

        // Then
        assertEquals(GroupsLayout(GroupsDisplay.GRID, GroupGridSize.LARGE), layout)
    }

    @Test
    fun `GIVEN the list WHEN picking the compact grid THEN stores it`() = runTest {
        // Given
        prepareScenario()
        val compactGrid = GroupsLayout(GroupsDisplay.GRID, GroupGridSize.COMPACT)

        // When
        updateGroupsLayout(compactGrid)

        // Then
        assertEquals(listOf(compactGrid), repository.updatedLayouts)
        assertEquals(compactGrid, repository.layout.value)
    }

    private fun prepareScenario() {
        repository = FakeDeviceSettingsRepository()
        observeGroupsLayout = ObserveGroupsLayoutUseCase(repository)
        updateGroupsLayout = UpdateGroupsLayoutUseCase(repository)
    }
}
