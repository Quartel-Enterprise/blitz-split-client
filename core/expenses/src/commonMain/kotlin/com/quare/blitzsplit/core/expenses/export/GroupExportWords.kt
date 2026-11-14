package com.quare.blitzsplit.core.expenses.export

/*
 * Why: the export's column headers and section names, in the app's language. The shared code never writes copy, so
 * each UI hands its own words over with the format picked, as it names the categories on screen. JSON keeps its keys in
 * English and takes only the category names from here.
 */
data class GroupExportWords(
    val bills: String,
    val payments: String,
    val balances: String,
    val date: String,
    val billDescription: String,
    val category: String,
    val amount: String,
    val currency: String,
    // Why: "Paid · Ana" and "Share · Ana", one pair of columns per member on the bills.
    val paid: String,
    val share: String,
    val from: String,
    val to: String,
    val member: String,
    val balance: String,
    // Why: who paid or was in a bill and left the group since, whose name the device no longer has.
    val formerMember: String,
    // Why: the name of each app category by its key (`AppCategory.key`); a group's own category has its own name.
    val appCategoryNames: Map<String, String>,
)
