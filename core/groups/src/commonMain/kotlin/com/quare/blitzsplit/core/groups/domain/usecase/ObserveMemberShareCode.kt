package com.quare.blitzsplit.core.groups.domain.usecase

import kotlinx.coroutines.flow.Flow

/*
 * Why: the code of a contact's profile link (blitzsplit.com/u/<code>), which their profile shares; null for an
 * account whose code no sync brought yet.
 */
fun interface ObserveMemberShareCode {
    operator fun invoke(userId: String): Flow<String?>
}
