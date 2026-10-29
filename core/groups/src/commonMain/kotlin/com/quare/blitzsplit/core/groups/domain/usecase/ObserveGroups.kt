package com.quare.blitzsplit.core.groups.domain.usecase

import com.quare.blitzsplit.core.groups.domain.model.Group
import kotlinx.coroutines.flow.Flow

fun interface ObserveGroups {
    operator fun invoke(): Flow<List<Group>>
}
