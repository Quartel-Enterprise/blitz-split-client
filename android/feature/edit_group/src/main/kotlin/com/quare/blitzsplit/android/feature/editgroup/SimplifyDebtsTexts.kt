package com.quare.blitzsplit.android.feature.editgroup

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.nameListOf
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyAmount
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyDebtsConfirmation
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyName
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyPersonChange
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyQueuedPayment
import com.quare.blitzsplit.android.designsystem.R as DesignR

// Why: the bold name of a row of "O que muda": "Você", "Diego · convidado", or "Ex-membro" for a name gone.
@Composable
internal fun changeNameOf(change: SimplifyPersonChange): String {
    val person = change.person
    if (person.isMe) return stringResource(DesignR.string.designsystem_you)
    val name = person.name ?: stringResource(DesignR.string.designsystem_former_member)
    return if (change.isGuest) stringResource(R.string.simplify_change_guest, name) else name
}

/*
 * Why: "Não paga mais R$ 118,00 a Bruno. Paga R$ 29,50 a você. Não recebe mais de Carla. Recebe de Carla, Diego e
 * Elisa.", each sentence only while its list has someone, in this order, and the guest's reminder last.
 */
@Composable
internal fun changeTextOf(
    change: SimplifyPersonChange,
    money: MoneyFormatter,
): String {
    val isMe = change.person.isMe
    return listOfNotNull(
        amountsSentenceOrNull(
            amounts = change.stopsPaying,
            money = money,
            sentence = if (isMe) R.string.simplify_change_stops_paying_me else R.string.simplify_change_stops_paying,
        ),
        amountsSentenceOrNull(
            amounts = change.pays,
            money = money,
            sentence = if (isMe) R.string.simplify_change_pays_me else R.string.simplify_change_pays,
        ),
        namesSentenceOrNull(
            names = change.stopsReceivingFrom,
            sentence = if (isMe) {
                R.string.simplify_change_stops_receiving_me
            } else {
                R.string.simplify_change_stops_receiving
            },
        ),
        namesSentenceOrNull(
            names = change.receivesFrom,
            sentence = if (isMe) R.string.simplify_change_receives_me else R.string.simplify_change_receives,
        ),
        stringResource(R.string.simplify_change_guest_note).takeIf { change.isGuest },
    ).joinToString(" ")
}

@Composable
private fun amountsSentenceOrNull(
    amounts: List<SimplifyAmount>,
    money: MoneyFormatter,
    @StringRes sentence: Int,
): String? {
    if (amounts.isEmpty()) return null
    val parts = amounts.map { amount ->
        val text = money.toText(amount.amountMinor)
        if (amount.to.isMe) {
            stringResource(R.string.simplify_change_amount_to_you, text)
        } else {
            stringResource(R.string.simplify_change_amount_to, text, lowercaseNameOf(amount.to))
        }
    }
    return stringResource(sentence, nameListOf(parts))
}

@Composable
private fun namesSentenceOrNull(
    names: List<SimplifyName>,
    @StringRes sentence: Int,
): String? {
    if (names.isEmpty()) return null
    return stringResource(sentence, nameListOf(names.map { lowercaseNameOf(it) }))
}

// Why: "você" inside a sentence, "Ex-membro" for a name gone.
@Composable
private fun lowercaseNameOf(name: SimplifyName): String = if (name.isMe) {
    stringResource(R.string.simplify_you_lowercase)
} else {
    name.name ?: stringResource(DesignR.string.designsystem_former_member)
}

/*
 * Why: "Você tem 1 pagamento na fila: Elisa te pagou R$ 50,00. A simplificação conta com ele…" (13.3d), or how many
 * once there are several.
 */
@Composable
internal fun queuedTextOf(
    queued: List<SimplifyQueuedPayment>,
    money: MoneyFormatter,
): String {
    val payment = queued.singleOrNull()
        ?: return pluralStringResource(R.plurals.simplify_queued_several, queued.size, queued.size)
    val amount = money.toText(payment.amountMinor)
    val clause = when {
        payment.to.isMe -> stringResource(R.string.simplify_queued_paid_you, lowercaseNameOf(payment.from), amount)

        payment.from.isMe -> stringResource(R.string.simplify_queued_you_paid, amount, lowercaseNameOf(payment.to))

        else -> stringResource(
            R.string.simplify_queued_paid,
            lowercaseNameOf(payment.from),
            amount,
            lowercaseNameOf(payment.to),
        )
    }
    return stringResource(R.string.simplify_queued_one, clause)
}

@Composable
internal fun confirmationTitleOf(confirmation: SimplifyDebtsConfirmation): String = stringResource(
    if (confirmation.isTurningOn) R.string.simplify_confirm_title_on else R.string.simplify_confirm_title_off,
)

@Composable
internal fun confirmationBodyOf(confirmation: SimplifyDebtsConfirmation): String = when {
    confirmation.isAlreadyMinimal -> stringResource(R.string.simplify_confirm_body_minimal, confirmation.groupName)
    confirmation.isTurningOn -> stringResource(R.string.simplify_confirm_body_on)
    else -> stringResource(R.string.simplify_confirm_body_off)
}

@Composable
internal fun confirmationFootOf(confirmation: SimplifyDebtsConfirmation): String = when {
    confirmation.isAlreadyMinimal -> stringResource(R.string.simplify_confirm_foot_minimal)

    confirmation.isTurningOn -> stringResource(R.string.simplify_confirm_foot_on)

    confirmation.othersChangingCount == 0 -> stringResource(R.string.simplify_confirm_foot_off_none)

    else -> pluralStringResource(
        R.plurals.simplify_confirm_foot_off,
        confirmation.othersChangingCount,
        confirmation.othersChangingCount,
    )
}

@Composable
internal fun confirmLabelOf(confirmation: SimplifyDebtsConfirmation): String = stringResource(
    when {
        confirmation.isAlreadyMinimal -> R.string.simplify_confirm_minimal
        confirmation.isTurningOn -> R.string.simplify_confirm_on
        else -> R.string.simplify_confirm_off
    },
)

@Composable
internal fun dismissLabelOf(confirmation: SimplifyDebtsConfirmation): String = stringResource(
    if (confirmation.isTurningOn) R.string.simplify_confirm_not_now else R.string.simplify_confirm_cancel,
)
