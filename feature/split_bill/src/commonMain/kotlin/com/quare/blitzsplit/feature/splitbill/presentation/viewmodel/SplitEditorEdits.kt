package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.AmountInput
import com.quare.blitzsplit.core.expenses.domain.model.BillSplit
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.SplitEntry
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.groups.domain.model.GroupDefaultSplit
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillDefaultSplit
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillPerson
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitEditor
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitRow

// Why: the modes whose rows keep no text: Igual's checkbox, Partes' stepper and Itens, which the items give.
private val modesWithoutText = setOf(SplitMode.EQUAL, SplitMode.SHARES, SplitMode.ITEMS)

// Why: "Editar conta" opens the split as it was typed: who was in it, each one's value, percent or parts.
internal fun splitEditorOf(
    expense: Expense,
    people: List<BillPerson>,
    currency: GroupCurrency,
    decimalSeparator: Char,
): SplitEditor {
    val weights = expense.shares.associate { it.memberId to it.weight }
    val rows = people.map { person ->
        val weight = weights[person.id] ?: 0L
        SplitRow(
            person = person,
            isIncluded = weight > 0,
            valueText = when {
                weight <= 0 || expense.splitMode in modesWithoutText -> ""

                expense.splitMode == SplitMode.AMOUNT -> AmountInput.format(
                    amountMinor = weight,
                    currency = currency,
                    decimalSeparator = decimalSeparator,
                )

                else -> weight.toString()
            },
            parts = if (expense.splitMode == SplitMode.SHARES) weight else 0L,
            shareMinor = null,
        )
    }
    return SplitEditor(
        mode = expense.splitMode,
        rows = rows,
        totalMinor = expense.amountMinor,
        payerIds = expense.payers.map { it.memberId },
        currency = currency,
        items = expense.items?.toItemsAmong(people, isWholeGroupAtTable = false),
    )
}

internal fun GroupMember.toBillPerson(): BillPerson = BillPerson(
    id = id,
    name = name,
    initials = Initials.ofMember(name, isMe),
    avatarColor = avatarColor,
    isMe = isMe,
    hasJoined = hasJoined,
)

/*
 * Why: the people of the group as they are now: a new member comes in "Igual" and stays out of the other modes,
 * unless newcomerRows gives them their row (the group's default's, while the split still follows it). A bill being
 * edited keeps its split, so saving it never adds whoever joined meanwhile.
 */
internal fun SplitEditor.withPeople(
    people: List<BillPerson>,
    currency: GroupCurrency,
    payerIds: List<String>,
    includesNewcomers: Boolean,
    newcomerRows: Map<String, SplitRow> = emptyMap(),
): SplitEditor {
    val byId = rows.associateBy { it.person.id }
    val rows = people.map { person ->
        byId[person.id]?.copy(person = person) ?: newcomerRows[person.id]?.copy(person = person) ?: SplitRow(
            person = person,
            isIncluded = includesNewcomers,
            valueText = "",
            parts = 0L,
            shareMinor = null,
        )
    }
    return copy(
        rows = rows,
        currency = currency,
        payerIds = payerIds,
        items = items?.toItemsAmong(people, isWholeGroupAtTable = includesNewcomers),
    ).withShares()
}

/*
 * Why: "Mudar o total refaz a divisão por valor proporcionalmente", from the parts of "Valores" last set in "Dividir"
 * with the total they were set for (the anchor).
 */
internal fun SplitEditor.withTotal(
    totalMinor: Long,
    anchor: Pair<Long, List<SplitEntry>>?,
    decimalSeparator: Char,
): SplitEditor {
    if (totalMinor == this.totalMinor) return this
    if (mode != SplitMode.AMOUNT || anchor == null || totalMinor == 0L) {
        return copy(totalMinor = totalMinor).withShares()
    }
    val (anchorTotal, anchorEntries) = anchor
    val scaled = BillSplit
        .calculateRescaledAmounts(
            entries = anchorEntries,
            oldTotalMinor = anchorTotal,
            newTotalMinor = totalMinor,
            payerIds = payerIds,
        ).associate { it.memberId to it.value }
    return copy(totalMinor = totalMinor)
        .withRows { row ->
            val value = scaled[row.person.id] ?: 0L
            row.copy(valueText = if (value > 0) formatAmount(value, decimalSeparator) else "")
        }.withShares()
}

/*
 * Why: a new mode starts from who is in the split now, everyone when nobody is: "Valores" with the equal parts,
 * "%" with 100 shared as evenly as whole numbers allow and "Partes" with one each.
 */
internal fun SplitEditor.toMode(
    mode: SplitMode,
    decimalSeparator: Char,
): SplitEditor {
    if (mode == this.mode) return this
    val inSplit = entries
        .filter { it.value > 0 }
        .map { it.memberId }
        .toSet()
        .ifEmpty { rows.map { it.person.id }.toSet() }
    val equalEntries = rows.map { row ->
        val isIn = row.person.id in inSplit
        SplitEntry(
            memberId = row.person.id,
            value = if (isIn) 1L else 0L,
        )
    }
    val values: Map<String, Long> = when (mode) {
        SplitMode.EQUAL, SplitMode.SHARES -> equalEntries.associate { it.memberId to it.value }

        SplitMode.AMOUNT -> splitEvenly(totalMinor, equalEntries)

        SplitMode.PERCENT -> splitEvenly(BillSplit.FULL_PERCENT, equalEntries)

        // Why: Itens opens "Itens da conta" instead, whose "Pronto" sets the split.
        SplitMode.ITEMS -> return this
    }
    return copy(
        mode = mode,
        items = null,
    ).withRows { row ->
        val value = values[row.person.id] ?: 0L
        row.copy(
            isIncluded = row.person.id in inSplit,
            valueText = when {
                value <= 0 -> ""
                mode == SplitMode.AMOUNT -> formatAmount(value, decimalSeparator)
                else -> value.toString()
            },
            parts = if (mode == SplitMode.SHARES) value else row.parts,
        )
    }
}

internal fun SplitEditor.withRows(change: (SplitRow) -> SplitRow): SplitEditor = copy(rows = rows.map(change))

/*
 * Why: Dividir under "Itens da conta" for a bill split by its items, by "Valores" with each one's total, so going
 * back to it from the items starts from what they give.
 */
internal fun SplitEditor.toAmountsOfItems(decimalSeparator: Char): SplitEditor {
    if (mode != SplitMode.ITEMS) return this
    val totals = entries.associate { it.memberId to it.value }
    return copy(
        mode = SplitMode.AMOUNT,
        items = null,
    ).withRows { row ->
        val value = totals[row.person.id] ?: 0L
        row.copy(
            isIncluded = value > 0,
            valueText = if (value > 0) formatAmount(value, decimalSeparator) else "",
        )
    }.withShares()
}

// Why: each row's part, as "Dividir" shows it under or beside the name, while the split adds up.
internal fun SplitEditor.withShares(): SplitEditor {
    val byMember = shares?.associate { it.memberId to it.amountMinor }
    return withRows { row -> row.copy(shareMinor = byMember?.get(row.person.id)) }
}

internal fun SplitEditor.formatAmount(
    amountMinor: Long,
    decimalSeparator: Char,
): String = AmountInput.format(
    amountMinor = amountMinor,
    currency = currency,
    decimalSeparator = decimalSeparator,
)

private fun SplitEditor.splitEvenly(
    total: Long,
    entries: List<SplitEntry>,
): Map<String, Long> = BillSplit
    .split(
        totalMinor = total,
        mode = SplitMode.EQUAL,
        entries = entries,
        payerIds = payerIds,
    )?.associate { it.memberId to it.amountMinor }
    .orEmpty()

/*
 * Why: the group's "Divisão padrão" (6.7) as a new bill's split among its people now: who joined after it was saved
 * comes in as the rules start them (in, 1 part, 0%). Percentages that no longer add up to 100, once someone holding
 * part of them left the group, start the bill equally among everyone instead, as a group without one does.
 */
internal fun billDefaultSplitOf(
    default: GroupDefaultSplit?,
    people: List<BillPerson>,
    currency: GroupCurrency,
    payerIds: List<String>,
    totalMinor: Long,
    exceptionCategoryId: String?,
): BillDefaultSplit? {
    if (default == null || people.isEmpty()) return null
    val weights = people.associate { it.id to default.weightOf(it.id) }
    val mode = SplitMode.of(default.mode)
    val isValid = weights.values.any { it > 0 } &&
        (mode != SplitMode.PERCENT || weights.values.sum() == BillSplit.FULL_PERCENT)
    if (!isValid) return null
    val editor = SplitEditor(
        mode = mode,
        rows = people.map { person ->
            val weight = weights.getValue(person.id)
            SplitRow(
                person = person,
                isIncluded = weight > 0,
                valueText = if (mode == SplitMode.PERCENT && weight > 0) weight.toString() else "",
                parts = if (mode == SplitMode.SHARES) weight else 0L,
                shareMinor = null,
            )
        },
        totalMinor = totalMinor,
        payerIds = payerIds,
        currency = currency,
        items = null,
    ).withShares()
    return BillDefaultSplit(
        mode = default.mode,
        editor = editor,
        values = people.mapNotNull { person -> weights.getValue(person.id).takeIf { it > 0 } },
        excluded = people.filter { weights.getValue(it.id) <= 0 },
        exceptionCategoryId = exceptionCategoryId,
    )
}

/*
 * Why: the bill's split and Dividir's among the group's people as they are now. A new bill follows the group's
 * "Divisão padrão" (everyone equally without one) until "Pronto" picks its own; one being edited keeps its split, so
 * saving it never adds whoever joined meanwhile. Someone who joins while Dividir still follows the default gets the
 * default's weight (1 part for Partes), so its notice still says "Padrão" though the people changed.
 */
internal fun SplitBillUiState.withSplitAmong(
    people: List<BillPerson>,
    defaultSplit: GroupDefaultSplit?,
    followsDefault: Boolean,
): SplitBillUiState {
    // Why: the bill's category picks which split it opens with, its exception (6.7f) or the group's default.
    val exception = defaultSplit?.exceptionFor(listOf(category.id, categories.mainOf(category).id))
    val billDefault = if (isEditing) {
        null
    } else {
        billDefaultSplitOf(
            default = exception?.asDefault ?: defaultSplit,
            people = people,
            currency = currency,
            payerIds = payerIds,
            totalMinor = split.totalMinor,
            exceptionCategoryId = exception?.categoryId,
        )
    }
    val bill = if (followsDefault) {
        billDefault?.editor
            ?: createEmptySplit(currency).withPeople(people, currency, payerIds, includesNewcomers = true)
    } else {
        split.withPeople(people, currency, payerIds, !isEditing)
    }
    return copy(
        split = bill,
        splitEditor = splitEditor?.let { editor ->
            val followsDefault = this.defaultSplit?.isFollowedBy(editor) == true
            editor.withPeople(
                people = people,
                currency = currency,
                payerIds = payerIds,
                includesNewcomers = !isEditing,
                newcomerRows = billDefault
                    ?.editor
                    ?.rows
                    ?.associateBy { it.person.id }
                    ?.takeIf { followsDefault }
                    .orEmpty(),
            )
        },
        defaultSplit = billDefault,
    )
}

// Why: "Voltar ao padrão" only puts Dividir's split back; the bill's own changes on "Pronto", as any change there.
internal fun SplitBillUiState.withSplitEditorReset(): SplitBillUiState {
    val default = defaultSplit ?: return this
    val editor = splitEditor ?: return this
    return copy(
        splitEditor = default.editor
            .copy(
                totalMinor = editor.totalMinor,
                payerIds = editor.payerIds,
            ).withShares(),
    )
}
