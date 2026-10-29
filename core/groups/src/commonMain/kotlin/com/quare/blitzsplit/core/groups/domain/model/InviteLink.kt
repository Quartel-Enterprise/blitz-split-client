package com.quare.blitzsplit.core.groups.domain.model

// Why: the group's link, whoever opens it picks who they are in the group (flow 3).
object InviteLink {
    private const val BASE_URL = "https://borarachar.com/g/"

    fun of(inviteCode: String): String = BASE_URL + inviteCode
}
