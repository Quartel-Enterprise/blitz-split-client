package com.quare.blitzsplit.feature.activity.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.Category
import com.quare.blitzsplit.core.model.GroupCurrency

/*
 * Why: one side of "antes → depois", as the field needs it: an amount in the bill's own currency, a text, a moment, a
 * code (a split mode, a group type, a color, a currency) or the members it names, whom each UI joins in its language.
 */
data class ActivityChangeValue(
    val minor: Long?,
    val currency: GroupCurrency?,
    val text: String?,
    val millis: Long?,
    // Why: a bill's "Quando" without a time (`SpentDay.hasTime`), which shows its date alone.
    val hasTime: Boolean,
    val code: String?,
    val names: List<String>,
    // Why: a member no longer in the group, whom each UI writes "Ex-membro".
    val hasFormerMember: Boolean,
    // Why: a bill's category, resolved in every group's categories (Outros for none); null for any other field.
    val category: Category?,
)
