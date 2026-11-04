package com.quare.blitzsplit.core.groups.domain.model

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.ProfilePhoto

data class Group(
    val id: String,
    val name: String,
    val type: GroupType,
    val color: AvatarColor,
    // Why: "Ícone do grupo" (6.4d); only Android draws it.
    val shape: GroupShape,
    // Why: drawn in place of the color and the initials; a 512x512 JPEG like a profile's photo, null for none.
    val photo: ProfilePhoto?,
    val currency: GroupCurrency,
    // Why: in the order they were added, so the creator comes first.
    val members: List<GroupMember>,
    // Why: created or changed on this device and not up on the server yet.
    val isPending: Boolean,
    // Why: the code of the invite link, null until the group's first sync brings the one the server made.
    val inviteCode: String?,
    /*
     * Why: only the group's creator may release a member someone joined as ("Liberar nome") or change the
     * invite link ("Trocar link").
     */
    val isCreatedByMe: Boolean,
    // Why: "Divisão padrão" (6.7), how its new bills split; null while it splits equally among everyone.
    val defaultSplit: GroupDefaultSplit?,
    // Why: when the group itself last changed, in epoch millis: its last activity on the home until it has a bill.
    val updatedAt: Long,
    /*
     * Why: "Simplificar dívidas" (13.1): its pairs become the fewest payments that keep each one's total, on this
     * device at once, offline too.
     */
    val simplifiesDebts: Boolean,
    // Why: changed here and not up yet: "Ligado neste aparelho · aguardando envio" (13.1d).
    val isSimplifyDebtsPending: Boolean,
    // Why: the account that last changed it, "Ana simplificou as dívidas agora há pouco" (13.5c); null for nobody yet.
    val simplifyDebtsChangedBy: String?,
    // Why: someone changed it after this device did, which lost (13.1e); null once a screen said so.
    val simplifyDebtsConflictBy: String?,
)
