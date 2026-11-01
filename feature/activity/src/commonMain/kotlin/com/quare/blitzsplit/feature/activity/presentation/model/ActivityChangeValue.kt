package com.quare.blitzsplit.feature.activity.presentation.model

import com.quare.blitzsplit.core.model.GroupCurrency

/*
 * Why: one side of "antes → depois", as the field needs it: an amount in the group's currency, a text, a moment, a
 * code (a split mode, a group type, a color, a currency) or the members it names, whom each UI joins in its language.
 */
data class ActivityChangeValue(
    val minor: Long?,
    val currency: GroupCurrency?,
    val text: String?,
    val millis: Long?,
    val code: String?,
    val names: List<String>,
    // Why: a member no longer in the group, whom each UI writes "Ex-membro".
    val hasFormerMember: Boolean,
)
