package com.quare.blitzsplit.core.contacts.domain.model

/*
 * Why: "Juntar com a conta dele", waiting for the guest to open their personal link ("Aguardando Diego aceitar"). Once
 * they join through it, everything in the guest's name passes to their account, in every group.
 */
data class GuestMerge(
    val id: String,
    // Why: the guest's members it claims, the one whose personal link carries it first.
    val memberIds: List<String>,
    // Why: blitzsplit.com/g/<groupInviteCode>/<memberInviteCode>, single use.
    val link: String,
    // Why: "Convite pessoal enviado hoje, 14:20", in epoch millis.
    val requestedAt: Long,
)
