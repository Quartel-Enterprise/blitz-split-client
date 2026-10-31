package com.quare.blitzsplit.core.settings.domain.usecase.impl

import com.quare.blitzsplit.core.model.GroupsLayout
import com.quare.blitzsplit.core.settings.domain.repository.DeviceSettingsRepository
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateGroupsLayout

class UpdateGroupsLayoutUseCase(
    private val repository: DeviceSettingsRepository,
) : UpdateGroupsLayout {
    override suspend fun invoke(layout: GroupsLayout) {
        repository.updateGroupsLayout(layout)
    }
}
