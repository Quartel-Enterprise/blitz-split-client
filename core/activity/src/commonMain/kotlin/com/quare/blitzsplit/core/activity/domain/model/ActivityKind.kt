package com.quare.blitzsplit.core.activity.domain.model

import com.quare.blitzsplit.core.model.NotificationType

/*
 * Why: what happened, by the server's code of activity_events.kind, and the type "Filtrar" groups it under. A bill's
 * attachments and share link only make its history (7.3): Atividade leaves them out (isInFeed).
 */
enum class ActivityKind(
    val code: String,
    val type: ActivityType,
    val isInFeed: Boolean = true,
    // Why: the row of Notifications whose In app toggle marks it new, its filter type's unless a kind says otherwise.
    val notificationType: NotificationType = type.notificationType,
) {
    BILL_CREATED("bill_created", ActivityType.BILLS),
    BILL_EDITED("bill_edited", ActivityType.BILLS),
    BILL_DELETED("bill_deleted", ActivityType.BILLS),
    PAYMENT_RECORDED("payment_recorded", ActivityType.PAYMENTS),
    PAYMENT_UNDONE("payment_undone", ActivityType.PAYMENTS),
    GROUP_CREATED("group_created", ActivityType.PEOPLE_AND_GROUPS),
    GROUP_EDITED("group_edited", ActivityType.PEOPLE_AND_GROUPS),
    GROUP_DELETED("group_deleted", ActivityType.PEOPLE_AND_GROUPS),
    INVITE_LINK_CHANGED("invite_link_changed", ActivityType.PEOPLE_AND_GROUPS),
    MEMBER_ADDED("member_added", ActivityType.PEOPLE_AND_GROUPS),
    MEMBER_JOINED("member_joined", ActivityType.PEOPLE_AND_GROUPS),
    MEMBER_LEFT("member_left", ActivityType.PEOPLE_AND_GROUPS),
    MEMBER_REMOVED("member_removed", ActivityType.PEOPLE_AND_GROUPS),
    MEMBER_RELEASED("member_released", ActivityType.PEOPLE_AND_GROUPS),
    MEMBER_RENAMED("member_renamed", ActivityType.PEOPLE_AND_GROUPS),

    // Why: "Ana ligou Simplificar dívidas em Viagem Floripa" (13.7), or desligou, by code ON or OFF.
    // Why: it changes who pays whom, so its bell (and its push) sit with the payments rather than its filter's type.
    SIMPLIFY_DEBTS_CHANGED(
        "simplify_debts_changed",
        ActivityType.PEOPLE_AND_GROUPS,
        notificationType = NotificationType.PAYMENTS,
    ),
    CHARGE_SENT("charge_sent", ActivityType.CHARGES),
    COMMENT_ADDED("comment_added", ActivityType.COMMENTS),
    ATTACHMENT_ADDED("attachment_added", ActivityType.BILLS, isInFeed = false),
    ATTACHMENT_REMOVED("attachment_removed", ActivityType.BILLS, isInFeed = false),
    BILL_LINK_CHANGED("bill_link_changed", ActivityType.BILLS, isInFeed = false),
    ;

    // Why: its target is a bill, which a comment's is too.
    val isAboutBill: Boolean
        get() = type == ActivityType.BILLS || type == ActivityType.COMMENTS

    companion object {
        // Why: a kind a newer server records stays out of the feed of this version.
        fun fromCodeOrNull(code: String): ActivityKind? = entries.firstOrNull { it.code == code }
    }
}
