package com.quare.blitzsplit.core.sync.domain.model

import com.quare.blitzsplit.core.activity.domain.model.ActivityKind

// Why: what an event of Atividade heard in real time reaches on the device, so RemotePull knows what to bring down.
enum class RemoteReach {
    ACTIVITY,
    GROUP,
    MEMBERS,
    COMMENTS,
    ATTACHMENTS,
    EVERYTHING,
    ;

    companion object {
        // Why: the reaches that bring their group's rows down.
        val groupReaches: Set<RemoteReach> = setOf(GROUP, MEMBERS)

        // Why: every kind says what it reaches, so a kind added to ActivityKind has to say it too.
        fun of(kind: ActivityKind?): RemoteReach = when (kind) {
            ActivityKind.BILL_CREATED,
            ActivityKind.BILL_EDITED,
            ActivityKind.BILL_DELETED,
            ActivityKind.PAYMENT_RECORDED,
            ActivityKind.PAYMENT_UNDONE,
            ActivityKind.GROUP_CREATED,
            ActivityKind.GROUP_EDITED,
            ActivityKind.INVITE_LINK_CHANGED,
            ActivityKind.MEMBER_RENAMED,
            ActivityKind.SIMPLIFY_DEBTS_CHANGED,
            -> GROUP

            /*
             * Why: a name added, or an account added straight in, which brings the group to that account's devices
             * (Atividade backfills it) with the faces and keys of the people in it.
             */
            ActivityKind.MEMBER_ADDED -> MEMBERS

            ActivityKind.COMMENT_ADDED -> COMMENTS

            ActivityKind.ATTACHMENT_ADDED, ActivityKind.ATTACHMENT_REMOVED -> ATTACHMENTS

            // Why: a charge and a bill's share link live only in Atividade (the link is read online, never stored).
            ActivityKind.CHARGE_SENT, ActivityKind.BILL_LINK_CHANGED -> ACTIVITY

            /*
             * Why: someone's account in or out of a group changes who is in it, their keys and photos, or the group
             * itself on this device; a guest merged into an account moves bills and drops a member. A kind a newer
             * server records may do any of it.
             */
            ActivityKind.GROUP_DELETED,
            ActivityKind.MEMBER_JOINED,
            ActivityKind.MEMBER_LEFT,
            ActivityKind.MEMBER_REMOVED,
            ActivityKind.MEMBER_RELEASED,
            null,
            -> EVERYTHING
        }
    }
}
