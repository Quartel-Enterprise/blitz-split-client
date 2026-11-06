package com.quare.blitzsplit.android.feature.activity

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.quare.blitzsplit.android.designsystem.component.labelRes
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.categoryName
import com.quare.blitzsplit.android.designsystem.format.groupCurrencyLabel
import com.quare.blitzsplit.android.designsystem.format.isYouSubjectUi
import com.quare.blitzsplit.android.designsystem.format.labelRes
import com.quare.blitzsplit.android.designsystem.format.nameListOf
import com.quare.blitzsplit.android.designsystem.format.rememberDateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.templateForActorOf
import com.quare.blitzsplit.core.activity.domain.model.ActivityField
import com.quare.blitzsplit.core.activity.domain.model.ActivityKind
import com.quare.blitzsplit.core.expenses.domain.model.BillLinkVisibility
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityChangeValue
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityDeletion
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityItem
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityTarget

// Why: the kinds whose sentence names the group itself.
private val groupObjectKinds = setOf(
    ActivityKind.GROUP_CREATED,
    ActivityKind.GROUP_EDITED,
    ActivityKind.GROUP_DELETED,
    ActivityKind.INVITE_LINK_CHANGED,
    ActivityKind.MEMBER_JOINED,
    ActivityKind.MEMBER_LEFT,
    ActivityKind.SIMPLIFY_DEBTS_CHANGED,
)

@StringRes
private fun sentenceOf(item: ActivityItem): Int = when (item.kind) {
    ActivityKind.BILL_CREATED -> R.string.activity_kind_bill_created

    ActivityKind.BILL_EDITED -> R.string.activity_kind_bill_edited

    ActivityKind.BILL_DELETED -> R.string.activity_kind_bill_deleted

    ActivityKind.PAYMENT_RECORDED -> R.string.activity_kind_payment_recorded

    ActivityKind.PAYMENT_UNDONE -> R.string.activity_kind_payment_undone

    ActivityKind.GROUP_CREATED -> R.string.activity_kind_group_created

    ActivityKind.GROUP_EDITED -> R.string.activity_kind_group_edited

    ActivityKind.GROUP_DELETED -> R.string.activity_kind_group_deleted

    ActivityKind.INVITE_LINK_CHANGED -> R.string.activity_kind_invite_link_changed

    ActivityKind.MEMBER_ADDED -> R.string.activity_kind_member_added

    ActivityKind.MEMBER_JOINED -> R.string.activity_kind_member_joined

    ActivityKind.MEMBER_LEFT -> R.string.activity_kind_member_left

    ActivityKind.MEMBER_REMOVED -> R.string.activity_kind_member_removed

    ActivityKind.MEMBER_RELEASED -> R.string.activity_kind_member_released

    ActivityKind.MEMBER_RENAMED -> R.string.activity_kind_member_renamed

    ActivityKind.CHARGE_SENT -> R.string.activity_kind_charge_sent

    ActivityKind.SIMPLIFY_DEBTS_CHANGED -> if (item.isSimplifyTurnedOn) {
        R.string.activity_kind_simplify_on
    } else {
        R.string.activity_kind_simplify_off
    }

    // Why: "Ana apagou um comentário em Jantar" once the comment is gone, with no text under it.
    ActivityKind.COMMENT_ADDED -> if (item.isCommentDeleted) {
        R.string.activity_kind_comment_deleted
    } else {
        R.string.activity_kind_comment_added
    }

    // Why: a bill's attachments and share link only make its history (isInFeed), so the feed never draws them.
    ActivityKind.ATTACHMENT_ADDED,
    ActivityKind.ATTACHMENT_REMOVED,
    ActivityKind.BILL_LINK_CHANGED,
    -> R.string.activity_kind_bill_edited
}

// Why: what the sentence's second name is: the bill, the group itself, or the member the event is about.
private fun objectOf(item: ActivityItem): String? = if (item.kind in groupObjectKinds) {
    item.groupName ?: item.subject
} else {
    item.subject
}

@Composable
internal fun actorNameOf(item: ActivityItem): String = if (item.actor.isMe) {
    stringResource(R.string.activity_you)
} else {
    item.actor.name ?: stringResource(R.string.activity_former_member)
}

/*
 * Why: "Bruno editou Jantar no Ostradamus", the two names in bold, bolder while unread, as the design writes each
 * kind of event.
 */
@Composable
internal fun sentenceTextOf(item: ActivityItem): AnnotatedString {
    val template = templateForActorOf(stringResource(sentenceOf(item)), item.actor.isMe)
    val names = listOf(actorNameOf(item), objectOf(item).orEmpty())
    val weight = if (item.isUnread) FontWeight.Bold else FontWeight.SemiBold
    return buildAnnotatedString {
        var rest = template
        while (rest.isNotEmpty()) {
            val index = rest.indexOf('%')
            if (index < 0 || index + 3 >= rest.length) {
                append(rest)
                break
            }
            append(rest.substring(0, index))
            val position = rest[index + 1].digitToIntOrNull()
            if (position == null) {
                append(rest.substring(index, index + 1))
                rest = rest.substring(index + 1)
                continue
            }
            withStyle(SpanStyle(fontWeight = weight)) { append(names.getOrElse(position - 1) { "" }) }
            rest = rest.substring(index + 4)
        }
    }
}

// Why: the line under the sentence, as the design writes it for each kind; null when there is nothing to add.
@Composable
internal fun detailTextOf(item: ActivityItem): String? {
    val money = rememberMoneyFormatter(item.currency ?: GroupCurrency.fallback)
    val amount = item.amountMinor?.let(money::toText)
    return when (item.kind) {
        ActivityKind.BILL_CREATED -> amount?.let { text ->
            item.code?.let(SplitMode::fromCodeOrNull)?.let {
                stringResource(R.string.activity_detail_amount_split, text, splitLabelOf(it))
            } ?: text
        }

        ActivityKind.BILL_DELETED -> amount?.let { stringResource(R.string.activity_detail_bill_gone, it) }

        ActivityKind.BILL_EDITED,
        ActivityKind.GROUP_EDITED,
        ActivityKind.MEMBER_RENAMED,
        -> firstChangeTextOf(item)

        ActivityKind.PAYMENT_RECORDED,
        ActivityKind.PAYMENT_UNDONE,
        -> stringResource(
            R.string.activity_detail_payment,
            personNameOf(item.fromName, item.fromIsMe),
            personNameOf(item.toName, item.toIsMe),
            amount.orEmpty(),
            stringResource(SettlementMethod.fromCode(item.code.orEmpty()).labelRes),
        )

        ActivityKind.GROUP_CREATED -> item.code?.let(GroupType::fromCodeOrNull)?.let { stringResource(it.labelRes) }

        ActivityKind.GROUP_DELETED -> stringResource(R.string.activity_detail_group_gone)

        ActivityKind.MEMBER_JOINED -> stringResource(R.string.activity_detail_joined)

        ActivityKind.INVITE_LINK_CHANGED -> stringResource(R.string.activity_detail_link_changed)

        ActivityKind.MEMBER_RELEASED -> stringResource(R.string.activity_detail_released)

        ActivityKind.CHARGE_SENT -> amount?.let { stringResource(R.string.activity_detail_charge, it) }

        // Why: a change still in the queue has no pairs yet; its time already says "Aguardando envio".
        ActivityKind.SIMPLIFY_DEBTS_CHANGED -> item.simplification?.let { simplificationDetailOf(it) }

        // Why: "“A cerveja que ficou comigo foi do Bruno…”", the comment under "Carla comentou em Jantar".
        ActivityKind.COMMENT_ADDED ->
            item.body
                ?.takeUnless { item.isCommentDeleted }
                ?.let { stringResource(R.string.activity_detail_comment, it) }

        ActivityKind.MEMBER_ADDED,
        ActivityKind.MEMBER_LEFT,
        ActivityKind.MEMBER_REMOVED,
        ActivityKind.ATTACHMENT_ADDED,
        ActivityKind.ATTACHMENT_REMOVED,
        ActivityKind.BILL_LINK_CHANGED,
        -> null
    }
}

// Why: "Valor R$ 612,00 → R$ 642,00 · mais 2 mudanças".
@Composable
private fun firstChangeTextOf(item: ActivityItem): String? {
    val first = item.changes.firstOrNull() ?: return null
    val change = stringResource(
        R.string.activity_detail_change,
        fieldLabelOf(first.field),
        changeValueTextOf(first.field, first.before),
        changeValueTextOf(first.field, first.after),
    )
    val more = item.moreChangesCount
    if (more == 0) return change
    return "$change · " + pluralStringResource(R.plurals.activity_more_changes, more, more)
}

@Composable
internal fun personNameOf(
    name: String?,
    isMe: Boolean,
): String = if (isMe) {
    stringResource(R.string.activity_person_you)
} else {
    name ?: stringResource(R.string.activity_former_member)
}

@Composable
internal fun fieldLabelOf(field: ActivityField): String = stringResource(
    when (field) {
        ActivityField.AMOUNT -> R.string.activity_field_amount
        ActivityField.DESCRIPTION -> R.string.activity_field_description
        ActivityField.SPENT_AT -> R.string.activity_field_spent_at
        ActivityField.PAYERS -> R.string.activity_field_payers
        ActivityField.SPLIT -> R.string.activity_field_split
        ActivityField.NAME -> R.string.activity_field_name
        ActivityField.TYPE -> R.string.activity_field_type
        ActivityField.COLOR -> R.string.activity_field_color
        ActivityField.SHAPE -> R.string.activity_field_shape
        ActivityField.PHOTO -> R.string.activity_field_photo
        ActivityField.CURRENCY -> R.string.activity_field_currency
        ActivityField.DEFAULT_SPLIT -> R.string.activity_field_default_split
        ActivityField.CATEGORY -> R.string.activity_field_category
        ActivityField.VISIBILITY -> R.string.activity_field_visibility
    },
)

// Why: one side of a change in the field's own words: an amount, a moment, a name list, a mode, a type, a color.
@Composable
internal fun changeValueTextOf(
    field: ActivityField,
    value: ActivityChangeValue,
): String {
    val empty = stringResource(R.string.activity_empty_value)
    val names =
        value.names +
            if (value.hasFormerMember) listOf(stringResource(R.string.activity_former_member)) else emptyList()
    return when (field) {
        ActivityField.AMOUNT -> value.minor?.let {
            rememberMoneyFormatter(
                value.currency ?: GroupCurrency.fallback,
            ).toText(it)
        }

        ActivityField.DESCRIPTION,
        ActivityField.NAME,
        -> value.text

        ActivityField.SPENT_AT -> value.millis?.let {
            val dates = rememberDateTimeTextFormatter()
            dates.toDateText(it) + " · " + dates.toTimeText(it)
        }

        ActivityField.PAYERS -> names.takeIf { it.isNotEmpty() }?.let { nameListOf(it) }

        ActivityField.SPLIT -> value.code?.let(SplitMode::fromCodeOrNull)?.let { mode ->
            val label = splitLabelOf(mode)
            if (names.isEmpty()) label else "$label · ${nameListOf(names)}"
        }

        ActivityField.TYPE -> value.code?.let(GroupType::fromCodeOrNull)?.let { stringResource(it.labelRes) }

        ActivityField.COLOR -> value.code?.let(AvatarColor::fromCodeOrNull)?.let { stringResource(it.labelRes) }

        ActivityField.SHAPE -> value.code?.let(GroupShape::fromCodeOrNull)?.let { stringResource(it.labelRes) }

        ActivityField.PHOTO -> stringResource(
            when (value.code) {
                ActivityField.PHOTO_PREVIOUS -> R.string.activity_photo_previous
                ActivityField.PHOTO_NEW -> R.string.activity_photo_new
                else -> R.string.activity_photo_none
            },
        )

        // Why: a code this version doesn't know, which the platform may not name either, shows by its raw code.
        ActivityField.CURRENCY -> value.code?.let { code ->
            val currency = GroupCurrency.fromCode(code)
            if (currency.isKnown) groupCurrencyLabel(currency, LocalLocale.current.platformLocale) else code
        }

        ActivityField.DEFAULT_SPLIT -> value.code?.let(DefaultSplitMode::fromCodeOrNull)?.let {
            stringResource(it.labelRes)
        }

        // Why: resolved in every group's categories, so a group's own is named too; none is "Outros".
        ActivityField.CATEGORY -> value.category?.let { categoryName(it) }

        ActivityField.VISIBILITY -> stringResource(
            when (BillLinkVisibility.fromCodeOrNull(value.code)) {
                BillLinkVisibility.MEMBERS -> R.string.activity_visibility_members
                BillLinkVisibility.ANYONE -> R.string.activity_visibility_anyone
                null -> R.string.activity_visibility_off
            },
        )
    } ?: empty
}

// Why: a split by items is stored as its amounts, so the server never sends "items".
@Composable
private fun splitLabelOf(mode: SplitMode): String = stringResource(
    when (mode) {
        SplitMode.EQUAL -> R.string.activity_split_equal
        SplitMode.AMOUNT, SplitMode.ITEMS -> R.string.activity_split_amount
        SplitMode.PERCENT -> R.string.activity_split_percent
        SplitMode.SHARES -> R.string.activity_split_shares
    },
)

// Why: "Abrir conta", "Ver pagamento"…, or "Conta apagada" once it is gone.
@Composable
internal fun openLabelOf(item: ActivityItem): String {
    val deletion = item.deletion
    if (deletion != null) {
        return stringResource(if (deletion.isGroup) R.string.activity_group_deleted else R.string.activity_bill_deleted)
    }
    val isPayment = item.kind == ActivityKind.PAYMENT_RECORDED || item.kind == ActivityKind.PAYMENT_UNDONE
    if (item.kind == ActivityKind.SIMPLIFY_DEBTS_CHANGED) return stringResource(R.string.activity_open_group_balances)
    return stringResource(
        when (item.target) {
            ActivityTarget.BILL -> R.string.activity_open_bill
            ActivityTarget.GROUP -> if (isPayment) R.string.activity_open_payment else R.string.activity_open_group
            ActivityTarget.MEMBERS -> R.string.activity_open_members
            ActivityTarget.BALANCES -> R.string.activity_open_balances
        },
    )
}

// Why: "Pastel na praia foi apagada por Bruno em sexta, 30 de outubro. O registro fica no feed, mas a conta não abre."
@Composable
internal fun deletedNoticeTextOf(
    deletion: ActivityDeletion,
    dates: DateTimeTextFormatter,
): String {
    val subject = deletion.subject
        ?: stringResource(if (deletion.isGroup) R.string.activity_this_group else R.string.activity_this_bill)
    val atMillis = deletion.atMillis
    // Why: in Spanish and French the sentence starts with who did it, "Tú eliminaste Pastel en la playa…".
    val isYouSubject = deletion.byMe && isYouSubjectUi()
    val byName = when {
        isYouSubject -> stringResource(R.string.activity_you)
        deletion.byMe -> stringResource(R.string.activity_you_lowercase)
        else -> deletion.byName
    }
    if (atMillis == null || byName == null) {
        return stringResource(
            if (deletion.isGroup) {
                R.string.activity_deleted_notice_group_unknown
            } else {
                R.string.activity_deleted_notice_bill_unknown
            },
            subject,
        )
    }
    val noticeRes = if (deletion.isGroup) {
        R.string.activity_deleted_notice_group
    } else {
        R.string.activity_deleted_notice_bill
    }
    val date = dates.toWeekdayDateText(atMillis)
    if (!isYouSubject) return stringResource(noticeRes, subject, byName, date)
    return templateForActorOf(
        stringResource(noticeRes),
        isMe = true,
        actorPlaceholder = "%2\$s",
    ).format(LocalLocale.current.platformLocale, subject, byName, date)
}

// Why: "Hoje", "Ontem", or "Sexta, 30 de outubro" over each day.
@Composable
internal fun dayTitleOf(
    dayStartMillis: Long,
    relativeDay: RelativeDay,
    dates: DateTimeTextFormatter,
): String = when (relativeDay) {
    RelativeDay.TODAY -> stringResource(R.string.activity_today)
    RelativeDay.YESTERDAY -> stringResource(R.string.activity_yesterday)
    RelativeDay.OTHER -> dates.toWeekdayDateText(dayStartMillis).replaceFirstChar { it.uppercase() }
}

// Why: "Viagem Floripa · 21:40", or "Aguardando envio" for a change not up yet.
@Composable
internal fun groupAndTimeTextOf(
    item: ActivityItem,
    dates: DateTimeTextFormatter,
): String {
    val time = if (item.isPending) stringResource(R.string.activity_pending) else dates.toTimeText(item.occurredAt)
    return item.groupName?.let { stringResource(R.string.activity_group_and_time, it, time) } ?: time
}
