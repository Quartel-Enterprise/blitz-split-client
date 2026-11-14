package com.quare.blitzsplit.core.expenses.export

/*
 * Why: the three sections of CSV, Excel and the text, with the UI's words: Bills (date, description, category, amount,
 * currency, then "Paid · Ana" and "Share · Ana" for each member), Payments (date, from, to, amount, currency) and
 * Balances (member, currency, balance). A new column is one entry in its list.
 */
internal object GroupExportTables {
    fun of(
        export: GroupExport,
        words: GroupExportWords,
    ): List<ExportTable> {
        val names = namesOf(export, words)
        return listOf(
            tableOf(words.bills, buildBillColumns(export, words, names), export.bills),
            tableOf(words.payments, buildPaymentColumns(words, names), export.payments),
            tableOf(words.balances, buildBalanceColumns(words, names), buildBalanceRows(export)),
        )
    }

    // Why: a member's name, or "Former member" (numbered from the second on) for whoever left the group.
    fun namesOf(
        export: GroupExport,
        words: GroupExportWords,
    ): Map<String, String> {
        val formerIds = export.members.filter { it.name == null }.map { it.id }
        return export.members.associate { member ->
            val formerIndex = formerIds.indexOf(member.id)
            member.id to when {
                member.name != null -> member.name
                formerIndex == 0 -> words.formerMember
                else -> "${words.formerMember} ${formerIndex + 1}"
            }
        }
    }

    fun categoryNameOf(
        bill: ExportBill,
        words: GroupExportWords,
    ): String = bill.category.app
        ?.let { words.appCategoryNames[it.key] ?: it.key }
        ?: bill.category.name.orEmpty()

    private fun buildBillColumns(
        export: GroupExport,
        words: GroupExportWords,
        names: Map<String, String>,
    ): List<ExportColumn<ExportBill>> = listOf(
        ExportColumn<ExportBill>(words.date) { ExportCell.Day(it.date) },
        ExportColumn(words.billDescription) { ExportCell.Text(it.description) },
        ExportColumn(words.category) { ExportCell.Text(categoryNameOf(it, words)) },
        ExportColumn(words.amount) { ExportCell.Amount(it.amountMinor, it.currency) },
        ExportColumn(words.currency) { ExportCell.Text(it.currency.code) },
    ) + export.members.flatMap { member ->
        val name = names.getValue(member.id)
        listOf(
            ExportColumn<ExportBill>("${words.paid} · $name") { bill ->
                bill.findPaidMinor(member.id)?.let { ExportCell.Amount(it, bill.currency) } ?: ExportCell.Empty
            },
            ExportColumn("${words.share} · $name") { bill ->
                bill.findShareMinor(member.id)?.let { ExportCell.Amount(it, bill.currency) } ?: ExportCell.Empty
            },
        )
    }

    private fun buildPaymentColumns(
        words: GroupExportWords,
        names: Map<String, String>,
    ): List<ExportColumn<ExportPayment>> = listOf(
        ExportColumn(words.date) { ExportCell.Day(it.date) },
        ExportColumn(words.from) { ExportCell.Text(names.getValue(it.fromMemberId)) },
        ExportColumn(words.to) { ExportCell.Text(names.getValue(it.toMemberId)) },
        ExportColumn(words.amount) { ExportCell.Amount(it.amountMinor, it.currency) },
        ExportColumn(words.currency) { ExportCell.Text(it.currency.code) },
    )

    private class BalanceRow(
        val balances: ExportCurrencyBalances,
        val member: ExportAmount,
    )

    private fun buildBalanceRows(export: GroupExport): List<BalanceRow> = export.balances.flatMap { balances ->
        balances.members.map { BalanceRow(balances, it) }
    }

    private fun buildBalanceColumns(
        words: GroupExportWords,
        names: Map<String, String>,
    ): List<ExportColumn<BalanceRow>> = listOf(
        ExportColumn(words.member) { ExportCell.Text(names.getValue(it.member.memberId)) },
        ExportColumn(words.currency) { ExportCell.Text(it.balances.currency.code) },
        ExportColumn(words.balance) { ExportCell.Amount(it.member.amountMinor, it.balances.currency) },
    )
}
