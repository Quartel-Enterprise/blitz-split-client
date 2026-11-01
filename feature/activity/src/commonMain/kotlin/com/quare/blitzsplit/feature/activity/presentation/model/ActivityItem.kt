package com.quare.blitzsplit.feature.activity.presentation.model

import com.quare.blitzsplit.core.activity.domain.model.ActivityKind
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType

/*
 * Why: one row of the feed, with what each UI needs to write "Bruno editou Jantar no Ostradamus" and its detail line in
 * its own words: the kind, who, the group, the subject and amount, a payment's two people and an edit's changes.
 */
data class ActivityItem(
    // Why: the event's id, or the queued bill's, payment's or group's.
    val id: String,
    val kind: ActivityKind,
    val actor: ActivityActor,
    val groupId: String,
    // Why: as the group is called now, or was when it was deleted.
    val groupName: String?,
    val groupColor: AvatarColor?,
    val groupType: GroupType?,
    val occurredAt: Long,
    // Why: the bill's description, or the group's or member's name.
    val subject: String?,
    val amountMinor: Long?,
    val currency: GroupCurrency?,
    // Why: a bill's split mode, a group's type, a payment's method, by their codes.
    val code: String?,
    // Why: a payment's payer and receiver, null for anything else; "Ex-membro" when null with a payment.
    val fromName: String?,
    val fromIsMe: Boolean,
    val toName: String?,
    val toIsMe: Boolean,
    val changes: List<ActivityChangeRow>,
    // Why: saved on this device and not up yet: "Aguardando envio" with the orange dot.
    val isPending: Boolean,
    val isUnread: Boolean,
    val target: ActivityTarget,
    // Why: the item's bill or group is gone: the row keeps its trash icon and tapping it says who deleted it.
    val deletion: ActivityDeletion?,
    // Why: the bill or member it is about, which "Mais em Jantar" gathers the other events of.
    val targetId: String?,
) {
    // Why: "Valor R$ 612,00 → R$ 642,00 · mais 2 mudanças", the first change and how many others.
    val moreChangesCount: Int
        get() = (changes.size - 1).coerceAtLeast(0)

    // Why: "Ver mudanças" only for a bill's edit, whose rows are the bill's history.
    val hasChangesToShow: Boolean
        get() = kind == ActivityKind.BILL_EDITED && changes.isNotEmpty()

    val isTargetDeleted: Boolean
        get() = deletion != null
}
