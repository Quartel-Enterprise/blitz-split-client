package com.quare.blitzsplit.core.settings.domain.usecase.impl

import com.quare.blitzsplit.core.model.GroupsLayout
import com.quare.blitzsplit.core.settings.domain.repository.DeviceSettingsRepository
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveGroupsLayout
import kotlinx.coroutines.flow.Flow

class ObserveGroupsLayoutUseCase(
    private val repository: DeviceSettingsRepository,
) : ObserveGroupsLayout {
    override fun invoke(): Flow<GroupsLayout> = repository.observeGroupsLayout()
}
