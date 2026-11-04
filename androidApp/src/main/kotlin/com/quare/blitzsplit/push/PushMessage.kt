package com.quare.blitzsplit.push

import androidx.annotation.StringRes
import com.quare.blitzsplit.R

/*
 * Why: the server sends each message's kind and the words that change in it, never a sentence, so the device words
 * it in its own language, as Atividade's feed is worded. A kind this version doesn't know isn't shown.
 */
enum class PushMessage(
    val kind: String,
    @param:StringRes val textRes: Int,
    val channel: PushChannel,
) {
    BILL_CREATED("bill_created", R.string.push_bill_created, PushChannel.NEW_EXPENSE),
    BILL_EDITED("bill_edited", R.string.push_bill_edited, PushChannel.NEW_EXPENSE),
    BILL_DELETED("bill_deleted", R.string.push_bill_deleted, PushChannel.NEW_EXPENSE),
    COMMENT_ADDED("comment_added", R.string.push_comment_added, PushChannel.NEW_EXPENSE),
    PAYMENT_PAID_YOU("payment_paid_you", R.string.push_payment_paid_you, PushChannel.PAYMENT_MARKED),
    PAYMENT_TO_YOU("payment_to_you", R.string.push_payment_to_you, PushChannel.PAYMENT_MARKED),
    PAYMENT_RECEIVED_YOURS("payment_received_yours", R.string.push_payment_received_yours, PushChannel.PAYMENT_MARKED),
    PAYMENT_FROM_YOU("payment_from_you", R.string.push_payment_from_you, PushChannel.PAYMENT_MARKED),
    PAYMENT_UNDONE("payment_undone", R.string.push_payment_undone, PushChannel.PAYMENT_MARKED),
    CHARGE_SENT("charge_sent", R.string.push_charge_sent, PushChannel.SETTLE_REMINDERS),
    SETTLE_REMINDER("settle_reminder", R.string.push_settle_reminder, PushChannel.SETTLE_REMINDERS),
    SETTLE_REMINDERS("settle_reminders", R.string.push_settle_reminders, PushChannel.SETTLE_REMINDERS),
    ADDED_TO_GROUP("added_to_group", R.string.push_added_to_group, PushChannel.INVITES),
    MEMBER_JOINED("member_joined", R.string.push_member_joined, PushChannel.INVITES),

    // Why: "Simplificar dívidas" turned on or off (13.7), who now pays you, whom you now pay or neither.
    DEBTS_SIMPLIFIED_TO_YOU(
        "debts_simplified_to_you",
        R.string.push_debts_simplified_to_you,
        PushChannel.SETTLE_REMINDERS,
    ),
    DEBTS_SIMPLIFIED_FROM_YOU(
        "debts_simplified_from_you",
        R.string.push_debts_simplified_from_you,
        PushChannel.SETTLE_REMINDERS,
    ),
    DEBTS_SIMPLIFIED_OTHER(
        "debts_simplified_other",
        R.string.push_debts_simplified_other,
        PushChannel.SETTLE_REMINDERS,
    ),
    DEBTS_UNSIMPLIFIED_TO_YOU(
        "debts_unsimplified_to_you",
        R.string.push_debts_unsimplified_to_you,
        PushChannel.SETTLE_REMINDERS,
    ),
    DEBTS_UNSIMPLIFIED_FROM_YOU(
        "debts_unsimplified_from_you",
        R.string.push_debts_unsimplified_from_you,
        PushChannel.SETTLE_REMINDERS,
    ),
    DEBTS_UNSIMPLIFIED_OTHER(
        "debts_unsimplified_other",
        R.string.push_debts_unsimplified_other,
        PushChannel.SETTLE_REMINDERS,
    ),
    ;

    companion object {
        fun ofKind(kind: String?): PushMessage? = entries.firstOrNull { it.kind == kind }
    }
}
