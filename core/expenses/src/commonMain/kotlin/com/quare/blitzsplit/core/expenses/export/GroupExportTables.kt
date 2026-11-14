package com.quare.blitzsplit.core.expenses.export

/*
 * Why: the three sections of CSV, Excel and the text, with the UI's words: Bills (date, description, category, amount,
 * currency, the exporter's payment method while any bill has one, then "Paid · Ana" and "Share · Ana" for each member), Payments (date, from, to, amount, currency) and
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

    /*
     * Why: a member's name, or "Former member" for whoever left the group; a label two members would share gets a
     * number from the second on ("Ana", "Ana 2", "Former member", "Former member 2"), so no two columns or rows read
     * the same.
     */
    fun namesOf(
        export: GroupExport,
        words: GroupExportWords,
    ): Map<String, String> {
        val counts = mutableMapOf<String, Int>()
        val used = mutableSetOf<String>()
        return export.members.associate { member ->
            val base = member.name ?: words.formerMember
            var count = counts.getOrElse(base) { 0 } + 1
            var label = if (count == 1) base else "$base $count"
            while (!used.add(label)) {
                count++
                label = "$base $count"
            }
            counts[base] = count
            member.id to label
        }
    }

    fun categoryNameOf(
        bill: ExportBill,
        words: GroupExportWords,
    ): String = bill.category.app
        ?.let { words.appCategoryNames[it.key] ?: it.key }
        ?: bill.category.name.orEmpty()

    // Why: "Nubank · Credit card", or the type's name alone for a method with no name; empty for an unmarked bill.
    fun payingMethodNameOf(
        bill: ExportBill,
        words: GroupExportWords,
    ): String {
        val method = bill.payingMethod ?: return ""
        val typeName = words.payingMethodTypeNames[method.type.code] ?: method.type.code
        return if (method.name.isBlank()) typeName else "${method.name} · $typeName"
    }

    private fun buildBillColumns(
        export: GroupExport,
        words: GroupExportWords,
        names: Map<String, String>,
    ): List<ExportColumn<ExportBill>> = listOfNotNull(
        ExportColumn<ExportBill>(words.date) { ExportCell.Day(it.date) },
        ExportColumn(words.billDescription) { ExportCell.Text(it.description) },
        ExportColumn(words.category) { ExportCell.Text(categoryNameOf(it, words)) },
        ExportColumn(words.amount) { ExportCell.Amount(it.amountMinor, it.currency) },
        ExportColumn(words.currency) { ExportCell.Text(it.currency.code) },
        // Why: only while the exporter marked a bill, so an export without "Paid with" keeps its columns as they were.
        ExportColumn<ExportBill>(words.paymentMethod) { bill ->
            payingMethodNameOf(bill, words).takeIf { it.isNotEmpty() }?.let(ExportCell::Text) ?: ExportCell.Empty
        }.takeIf { export.bills.any { it.payingMethod != null } },
    ) + export.members.flatMap { member ->
        val name = names.getValue(member.id)
        listOf(
            ExportColumn<ExportBill>("${words.paid} · $name") { bill ->
                bill.findPaidMinorOrNull(member.id)?.let { ExportCell.Amount(it, bill.currency) } ?: ExportCell.Empty
            },
            ExportColumn("${words.share} · $name") { bill ->
                bill.findShareMinorOrNull(member.id)?.let { ExportCell.Amount(it, bill.currency) } ?: ExportCell.Empty
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
