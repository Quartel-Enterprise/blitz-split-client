package com.quare.blitzsplit.core.groups.domain.model

// Why: the group's invite a link opens, and for a personal link the member it joins as and who shared it.
data class InviteTarget(
    val code: String,
    val memberCode: String?,
    val inviterCode: String?,
)
