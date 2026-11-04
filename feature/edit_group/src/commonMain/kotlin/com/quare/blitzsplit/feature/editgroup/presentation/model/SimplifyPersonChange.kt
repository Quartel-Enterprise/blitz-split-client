package com.quare.blitzsplit.feature.editgroup.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

/*
 * Why: one row of "O que muda" (13.3), which each UI words from the four lists in this order: "Não paga mais R$ 118,00
 * a Bruno. Paga R$ 29,50 a você. Não recebe mais de Carla. Recebe de Carla, Diego e Elisa."
 */
data class SimplifyPersonChange(
    val memberId: String,
    val person: SimplifyName,
    val initials: String,
    val avatarColor: AvatarColor,
    val photoUrl: String?,
    // Why: "Diego · convidado … Não tem o app: avise por Cobrar." (13.6b), a name nobody joined as.
    val isGuest: Boolean,
    val stopsPaying: List<SimplifyAmount>,
    val pays: List<SimplifyAmount>,
    val stopsReceivingFrom: List<SimplifyName>,
    val receivesFrom: List<SimplifyName>,
)
