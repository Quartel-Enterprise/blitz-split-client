package com.quare.blitzsplit.core.groups.domain.model

// Why: who someone joins a group as: a member nobody joined as yet, or someone new ("Não estou na lista").
sealed interface JoinChoice {
    data class AsMember(
        val memberId: String,
    ) : JoinChoice

    data class AsNewMember(
        val name: String,
    ) : JoinChoice
}
