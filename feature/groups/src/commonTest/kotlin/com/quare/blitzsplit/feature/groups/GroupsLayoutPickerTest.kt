package com.quare.blitzsplit.feature.groups

import com.quare.blitzsplit.core.model.GroupGridSize
import com.quare.blitzsplit.core.model.GroupsDisplay
import com.quare.blitzsplit.core.model.GroupsLayout
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateGroupsLayout
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.GroupsLayoutPicker
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Why: Grupos' "Lista ou grade" on its own: the groups wait until the device's layout is read (isRead).
class GroupsLayoutPickerTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var storedLayouts: MutableSharedFlow<GroupsLayout>
    private lateinit var picker: GroupsLayoutPicker
    private var shown = GroupsLayout.initial

    @Test
    fun `GIVEN the stored layout not read yet WHEN the picker watches it THEN it is not read`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val isRead = picker.isRead.value

            // Then
            assertFalse(isRead)
        }

    @Test
    fun `GIVEN the picker watching WHEN the first stored layout arrives THEN it is read and shows it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            storedLayouts.emit(largeGrid)

            // Then
            assertTrue(picker.isRead.value)
            assertEquals(largeGrid, shown)
        }

    @Test
    fun `GIVEN the large grid read WHEN the stored layout comes back as the list THEN shows the list`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            storedLayouts.emit(largeGrid)

            // When
            storedLayouts.emit(GroupsLayout(GroupsDisplay.LIST, GroupGridSize.LARGE))

            // Then
            assertEquals(GroupsLayout(GroupsDisplay.LIST, GroupGridSize.LARGE), shown)
            assertTrue(picker.isRead.value)
        }

    private val largeGrid = GroupsLayout(GroupsDisplay.GRID, GroupGridSize.LARGE)

    private fun TestScope.prepareScenario() {
        storedLayouts = MutableSharedFlow()
        shown = GroupsLayout.initial
        picker = GroupsLayoutPicker(
            updateGroupsLayout = UpdateGroupsLayout {},
            scope = backgroundScope,
            current = { shown },
            canPickGridSize = { shown.display == GroupsDisplay.GRID },
            show = { layout -> shown = layout },
        )
        picker.watch(storedLayouts)
    }
}
