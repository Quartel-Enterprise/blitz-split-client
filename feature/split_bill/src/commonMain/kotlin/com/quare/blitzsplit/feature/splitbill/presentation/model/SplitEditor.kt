package com.quare.blitzsplit.feature.splitbill.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.AmountInput
import com.quare.blitzsplit.core.expenses.domain.model.BillSplit
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SplitEntry
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.model.GroupCurrency

// Why: how the bill splits, the one "Rachar conta" saves and the one "Dividir" edits until "Pronto".
data class SplitEditor(
    val mode: SplitMode,
    val rows: List<SplitRow>,
    // Why: the bill's amount, 0 while the field holds no valid amount.
    val totalMinor: Long,
    // Why: who paid the bill, who keep the cents a split can't share evenly while they are in it.
    val payerIds: List<String>,
    val currency: GroupCurrency,
) {
    /*
     * Why: what each row puts in the split in this mode; a row at 0 is out of it. Worked out once per state, like
     * shares, since every UI reads them many times on each keystroke.
     */
    val entries: List<SplitEntry> = rows.map { row ->
        val value = when (mode) {
            SplitMode.EQUAL -> if (row.isIncluded) 1L else 0L
            SplitMode.AMOUNT -> AmountInput.parse(row.valueText, currency) ?: 0L
            SplitMode.PERCENT -> AmountInput.parseWhole(row.valueText) ?: 0L
            SplitMode.SHARES -> row.parts
        }
        SplitEntry(
            memberId = row.person.id,
            value = value,
        )
    }

    // Why: each member's part, null while the split doesn't add up to the total or has nobody in it.
    val shares: List<ExpenseShare>? = BillSplit.split(
        totalMinor = totalMinor,
        mode = mode,
        entries = entries,
        payerIds = payerIds,
    )

    // Why: "Pronto" only once it adds up.
    val addsUp: Boolean
        get() = shares != null

    val included: List<BillPerson>
        get() = entries.zip(rows).filter { (entry, _) -> entry.value > 0 }.map { (_, row) -> row.person }

    // Why: "Diego não participa".
    val excluded: List<BillPerson>
        get() = entries.zip(rows).filter { (entry, _) -> entry.value <= 0 }.map { (_, row) -> row.person }

    val includedCount: Int
        get() = included.size

    // Why: "Desmarcar todos" once everyone is in, "Marcar todos" otherwise.
    val isEveryoneIncluded: Boolean
        get() = rows.all { it.isIncluded }

    // Why: "R$ 62,40 para cada um", rounded down; the cents left over go to whoever paid.
    val perPersonMinor: Long
        get() = if (includedCount == 0) 0L else totalMinor / includedCount

    // Why: "Já foram R$ 558,00 de R$ 642,00".
    val assignedMinor: Long
        get() = if (mode == SplitMode.AMOUNT) entries.sumOf { it.value } else totalMinor

    // Why: "Faltam R$ 84,00" while positive, too much while negative.
    val remainingMinor: Long
        get() = totalMinor - assignedMinor

    // Why: "Elisa está sem valor", the rows left blank or at 0 in "Valores".
    val withoutValue: List<BillPerson>
        get() = if (mode == SplitMode.AMOUNT) excluded else emptyList()

    val percentSum: Long
        get() = if (mode == SplitMode.PERCENT) entries.sumOf { it.value } else BillSplit.FULL_PERCENT

    // Why: "Faltam 15%" while positive, too much while negative.
    val remainingPercent: Long
        get() = BillSplit.FULL_PERCENT - percentSum

    // Why: "6 partes de R$ 52,00".
    val totalParts: Long
        get() = if (mode == SplitMode.SHARES) entries.sumOf { it.value } else 0L

    val partValueMinor: Long
        get() = if (totalParts == 0L) 0L else totalMinor / totalParts

    // Why: the first line of Dividir's status, what matters most in this mode.
    val statusTitle: SplitFact
        get() = when (mode) {
            SplitMode.EQUAL -> if (includedCount == 0) SplitFact.Nobody else SplitFact.Each(perPersonMinor)

            SplitMode.AMOUNT -> amountLeftOrNull ?: SplitFact.AmountAddsUp(totalMinor)

            SplitMode.PERCENT -> percentLeftOrNull ?: SplitFact.PercentAddsUp

            SplitMode.SHARES -> if (totalParts == 0L) {
                SplitFact.Nobody
            } else {
                SplitFact.Parts(
                    count = totalParts,
                    partMinor = partValueMinor,
                )
            }
        }

    // Why: the sentences under the status's first line.
    val statusDetails: List<SplitFact>
        get() = when (mode) {
            SplitMode.EQUAL -> if (includedCount == 0) {
                listOf(SplitFact.NobodyChecked(totalMinor))
            } else {
                listOfNotNull(among, out)
            }

            SplitMode.AMOUNT -> if (remainingMinor == 0L) {
                listOf(SplitFact.PartsMatchTotal)
            } else {
                listOfNotNull(
                    SplitFact.AmountSoFar(
                        assignedMinor = assignedMinor,
                        totalMinor = totalMinor,
                    ),
                    withoutValue.takeIf { it.isNotEmpty() }?.let(SplitFact::WithoutValue),
                )
            }

            SplitMode.PERCENT -> listOfNotNull(among, out)

            SplitMode.SHARES -> sharesDetails
        }

    /*
     * Why: "R$ 62,40 cada · Diego não participa" under Rachar conta's "Divisão". A split that doesn't add up says what
     * is missing, as the status does, so the disabled Salvar has its reason in sight.
     */
    val summary: List<SplitFact>
        get() {
            val progress = when (mode) {
                SplitMode.EQUAL -> SplitFact.Each(perPersonMinor).takeIf { includedCount > 0 && totalMinor > 0 }
                SplitMode.AMOUNT -> amountLeftOrNull
                SplitMode.PERCENT -> percentLeftOrNull
                SplitMode.SHARES -> null
            }
            return listOfNotNull(progress, out)
        }

    private val amountLeftOrNull: SplitFact?
        get() = when {
            remainingMinor > 0 -> SplitFact.AmountLeft(remainingMinor)
            remainingMinor < 0 -> SplitFact.AmountOver(-remainingMinor)
            else -> null
        }

    private val percentLeftOrNull: SplitFact?
        get() = when {
            remainingPercent > 0 -> SplitFact.PercentLeft(remainingPercent)
            remainingPercent < 0 -> SplitFact.PercentOver(-remainingPercent)
            else -> null
        }

    private val among: SplitFact
        get() = SplitFact.Among(
            totalMinor = totalMinor,
            count = includedCount,
        )

    private val out: SplitFact?
        get() = excluded.takeIf { it.isNotEmpty() }?.let(SplitFact::Out)

    // Why: "Ana tem 2 partes. Diego não participa.", or who it splits among when nobody has more than one part.
    private val sharesDetails: List<SplitFact>
        get() {
            if (totalParts == 0L) return listOf(SplitFact.NobodyHasParts(totalMinor))
            val severalParts = rows
                .filter { it.parts > 1 }
                .map { row ->
                    SplitFact.PersonParts(
                        person = row.person,
                        parts = row.parts,
                    )
                }
            return (severalParts + listOfNotNull(out)).ifEmpty { listOf(among) }
        }
}
