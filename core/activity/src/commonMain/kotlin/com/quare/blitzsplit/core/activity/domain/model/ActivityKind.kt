package com.quare.blitzsplit.core.activity.domain.model

// Why: what happened, by the server's code of activity_events.kind, and the type "Filtrar" groups it under.
enum class ActivityKind(
    val code: String,
    val type: ActivityType,
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
    CHARGE_SENT("charge_sent", ActivityType.CHARGES),
    ;

    val isAboutBill: Boolean
        get() = type == ActivityType.BILLS

    companion object {
        // Why: a kind a newer server records stays out of the feed of this version.
        fun fromCodeOrNull(code: String): ActivityKind? = entries.firstOrNull { it.code == code }
    }
}
