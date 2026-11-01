package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/*
 * Why: "Ocultar de Contatos", one row per person (PeopleBalances.keyOf). "Mostrar" keeps the row with isHidden off, so
 * the change reaches the user's other devices.
 */
@Entity(tableName = "hidden_contact")
data class HiddenContactEntity(
    @PrimaryKey
    val personKey: String,
    val isHidden: Boolean,
    val hiddenAt: Long,
    val updatedAt: Long,
    val pendingSync: Boolean,
    // Why: the user whose list this is, as PaymentKeyEntity.ownerUserId, so a sync under another user drops it.
    val ownerUserId: String?,
)
