package com.quare.blitzsplit.core.expenses.domain.usecase

import kotlinx.coroutines.flow.Flow

fun interface ObserveRecentGroupId {
    /*
     * Why: the group a home tab's "Rachar conta" marks in "Rachar em qual grupo?" (7.1i), the most recently used one;
     * null while the device has no group, which also hides "Buscar".
     */
    operator fun invoke(): Flow<String?>
}
