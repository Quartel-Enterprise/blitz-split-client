package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.groups.domain.model.Group
import kotlinx.coroutines.flow.Flow

fun interface ObserveGroupsByActivity {
    // Why: the groups on the device as the home orders them, the most recently used first ("Rachar em qual grupo?").
    operator fun invoke(): Flow<List<Group>>
}
