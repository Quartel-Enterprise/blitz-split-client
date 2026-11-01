package com.quare.blitzsplit.core.groups.domain.usecase

import kotlinx.coroutines.flow.Flow

// Why: the groups "Fixar na Início" put first in Início's "Seus grupos".
fun interface ObservePinnedGroupIds {
    operator fun invoke(): Flow<Set<String>>
}
