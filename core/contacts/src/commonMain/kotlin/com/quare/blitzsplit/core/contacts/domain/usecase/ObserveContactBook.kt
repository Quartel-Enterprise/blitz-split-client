package com.quare.blitzsplit.core.contacts.domain.usecase

import com.quare.blitzsplit.core.contacts.domain.model.ContactBookSnapshot
import kotlinx.coroutines.flow.Flow

fun interface ObserveContactBook {
    operator fun invoke(): Flow<ContactBookSnapshot>
}
