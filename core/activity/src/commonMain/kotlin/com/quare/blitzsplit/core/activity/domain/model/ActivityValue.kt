package com.quare.blitzsplit.core.activity.domain.model

/*
 * Why: one side of a change, as raw as the server keeps it: an amount in minor units, a text, a moment, a code (a split
 * mode, a group type, a color, a currency) or the members it names. Each UI writes it in its language.
 */
data class ActivityValue(
    val minor: Long?,
    val text: String?,
    val millis: Long?,
    val code: String?,
    val memberIds: List<String>,
) {
    companion object {
        val none: ActivityValue = ActivityValue(
            minor = null,
            text = null,
            millis = null,
            code = null,
            memberIds = emptyList(),
        )
    }
}
