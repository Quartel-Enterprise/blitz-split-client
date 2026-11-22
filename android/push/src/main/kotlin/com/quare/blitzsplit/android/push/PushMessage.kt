package com.quare.blitzsplit.android.push

import android.content.Context
import androidx.annotation.StringRes
import org.json.JSONArray

/*
 * Why: the server sends each message's kind and the words that change in it, never a sentence, so the device words
 * it in its own language, as Atividade's feed is worded. A kind this version doesn't know isn't shown.
 */
enum class PushMessage(
    val kind: String,
    @param:StringRes val textRes: Int,
    // Why: the channel of its row in Notifications, one of PushChannelIds.
    val channelId: String,
) {
    BILL_CREATED("bill_created", R.string.push_bill_created, PushChannelIds.NEW_EXPENSE),
    BILL_EDITED("bill_edited", R.string.push_bill_edited, PushChannelIds.NEW_EXPENSE),
    BILL_DELETED("bill_deleted", R.string.push_bill_deleted, PushChannelIds.NEW_EXPENSE),
    COMMENT_ADDED("comment_added", R.string.push_comment_added, PushChannelIds.COMMENTS),
    PAYMENT_PAID_YOU("payment_paid_you", R.string.push_payment_paid_you, PushChannelIds.PAYMENT_MARKED),
    PAYMENT_TO_YOU("payment_to_you", R.string.push_payment_to_you, PushChannelIds.PAYMENT_MARKED),
    PAYMENT_RECEIVED_YOURS(
        "payment_received_yours",
        R.string.push_payment_received_yours,
        PushChannelIds.PAYMENT_MARKED,
    ),
    PAYMENT_FROM_YOU("payment_from_you", R.string.push_payment_from_you, PushChannelIds.PAYMENT_MARKED),
    PAYMENT_UNDONE("payment_undone", R.string.push_payment_undone, PushChannelIds.PAYMENT_MARKED),
    CHARGE_SENT("charge_sent", R.string.push_charge_sent, PushChannelIds.PAYMENT_REQUESTS),
    SETTLE_REMINDER("settle_reminder", R.string.push_settle_reminder, PushChannelIds.SETTLE_REMINDERS),
    SETTLE_REMINDERS("settle_reminders", R.string.push_settle_reminders, PushChannelIds.SETTLE_REMINDERS),
    ADDED_TO_GROUP("added_to_group", R.string.push_added_to_group, PushChannelIds.INVITES),
    MEMBER_JOINED("member_joined", R.string.push_member_joined, PushChannelIds.INVITES),

    // Why: "Simplificar dívidas" turned on or off (13.7), who now pays you, whom you now pay or neither.
    DEBTS_SIMPLIFIED_TO_YOU(
        "debts_simplified_to_you",
        R.string.push_debts_simplified_to_you,
        PushChannelIds.PAYMENT_MARKED,
    ),
    DEBTS_SIMPLIFIED_FROM_YOU(
        "debts_simplified_from_you",
        R.string.push_debts_simplified_from_you,
        PushChannelIds.PAYMENT_MARKED,
    ),
    DEBTS_SIMPLIFIED_OTHER(
        "debts_simplified_other",
        R.string.push_debts_simplified_other,
        PushChannelIds.PAYMENT_MARKED,
    ),
    DEBTS_UNSIMPLIFIED_TO_YOU(
        "debts_unsimplified_to_you",
        R.string.push_debts_unsimplified_to_you,
        PushChannelIds.PAYMENT_MARKED,
    ),
    DEBTS_UNSIMPLIFIED_FROM_YOU(
        "debts_unsimplified_from_you",
        R.string.push_debts_unsimplified_from_you,
        PushChannelIds.PAYMENT_MARKED,
    ),
    DEBTS_UNSIMPLIFIED_OTHER(
        "debts_unsimplified_other",
        R.string.push_debts_unsimplified_other,
        PushChannelIds.PAYMENT_MARKED,
    ),
    ;

    // Why: a reminder alone comes without a group to name, so it has a title of its own.
    val isReminder: Boolean
        get() = channelId == PushChannelIds.SETTLE_REMINDERS

    /*
     * Why: the sentence in the device's language from the server's words, a JSON array of strings; null when they
     * don't fit this version's sentence (a newer server's), which drops the notification, never the app.
     */
    fun toText(
        context: Context,
        args: String?,
    ): String? = runCatching {
        val words = args?.let(::JSONArray)?.let { array -> List(array.length()) { array.getString(it) } }.orEmpty()
        context.getString(textRes, *words.toTypedArray())
    }.getOrNull()

    companion object {
        fun ofKind(kind: String?): PushMessage? = entries.firstOrNull { it.kind == kind }
    }
}
