package com.quare.blitzsplit.core.groups.domain.usecase

/*
 * Why: "Trocar link", by the group's creator and online only. The old link and QR code stop working, and so does
 * every personal link not used yet, since they all carry the group's code; whoever already joined stays.
 */
fun interface ChangeInviteLink {
    suspend operator fun invoke(groupId: String): Result<Unit>
}
