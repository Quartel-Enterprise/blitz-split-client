package com.quare.blitzsplit.core.settings.domain.usecase

import com.quare.blitzsplit.core.model.GroupsLayout
import kotlinx.coroutines.flow.Flow

fun interface ObserveGroupsLayout {
    operator fun invoke(): Flow<GroupsLayout>
}
