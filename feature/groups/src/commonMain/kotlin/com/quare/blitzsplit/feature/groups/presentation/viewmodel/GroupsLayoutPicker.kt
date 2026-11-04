package com.quare.blitzsplit.feature.groups.presentation.viewmodel

import com.quare.blitzsplit.core.model.GroupGridSize
import com.quare.blitzsplit.core.model.GroupsDisplay
import com.quare.blitzsplit.core.model.GroupsLayout
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateGroupsLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

// Why: Grupos' "Lista ou grade" (5.5, 5.6), kept per device and never synced.
internal class GroupsLayoutPicker(
    private val updateGroupsLayout: UpdateGroupsLayout,
    private val scope: CoroutineScope,
    private val current: () -> GroupsLayout,
    private val canPickGridSize: () -> Boolean,
    private val show: (GroupsLayout) -> Unit,
) {
    // Why: the groups wait for it, so a grid picked on this device never shows as a list first.
    val isRead: StateFlow<Boolean>
        field = MutableStateFlow(false)

    fun watch(layouts: Flow<GroupsLayout>) {
        layouts
            .onEach { layout ->
                show(layout)
                isRead.value = true
            }.launchIn(scope)
    }

    fun changeDisplay(display: GroupsDisplay) {
        save(current().copy(display = display))
    }

    /*
     * Why: the size is disabled while the list is picked, so a late tap on it doesn't change the grid unseen. Says
     * whether the grid took another size, which Analytics counts.
     */
    fun changeGridSize(size: GroupGridSize): Boolean {
        if (!canPickGridSize() || current().gridSize == size) return false
        save(current().copy(gridSize = size))
        return true
    }

    /*
     * Why: the state takes the pick at once, so a second pick before the device stored the first (the Mac's slider
     * dragged back and forth) builds on it instead of on the stale layout; the stored layout then confirms it.
     */
    private fun save(layout: GroupsLayout) {
        if (layout == current()) return
        show(layout)
        scope.launch { updateGroupsLayout(layout) }
    }
}
