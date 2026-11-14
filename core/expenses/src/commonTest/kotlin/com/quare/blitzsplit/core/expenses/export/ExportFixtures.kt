package com.quare.blitzsplit.core.expenses.export

import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethod
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone

// Test data the export tests share: a group, its bills and payments, the UI's words, and the export they make.

internal const val EXPORT_GROUP_ID = "group-1"
internal const val PIERRE = "pierre"
internal const val ANA = "ana"
internal const val BRUNO = "bruno"

// Why: ids a bill or a payment still holds of someone who left the group, whose name the device no longer has.
internal const val CARLA_LEFT = "carla-left"
internal const val DIEGO_LEFT = "diego-left"

// Why: noon UTC of each day, the same day in UTC and in UTC-3.
internal const val NOV_10_NOON = 1_794_312_000_000L
internal const val NOV_11_NOON = 1_794_398_400_000L
internal const val NOV_12_NOON = 1_794_484_800_000L
internal const val NOV_13_NOON = 1_794_571_200_000L

// Why: 2026-11-14 02:30 UTC, which is still 2026-11-13 in São Paulo (UTC-3).
internal const val NOV_14_0230_UTC = 1_794_623_400_000L

// Why: 2026-11-14 15:00 UTC, the same day everywhere the tests look.
internal const val NOV_14_AFTERNOON = 1_794_668_400_000L

internal val saoPaulo: TimeZone = UtcOffset(hours = -3).asTimeZone()

internal fun exportWords(
    bills: String = "Bills",
    payments: String = "Payments",
    balances: String = "Balances",
): GroupExportWords = GroupExportWords(
    bills = bills,
    payments = payments,
    balances = balances,
    date = "Date",
    billDescription = "Description",
    category = "Category",
    amount = "Amount",
    currency = "Currency",
    paid = "Paid",
    share = "Share",
    from = "From",
    to = "To",
    member = "Member",
    balance = "Balance",
    formerMember = "Former member",
    appCategoryNames = mapOf(
        "food" to "Food",
        "food.restaurant" to "Restaurant",
        "other" to "Other",
    ),
    paymentMethod = "Payment method",
    payingMethodTypeNames = mapOf(
        "credit_card" to "Credit card",
        "cash" to "Cash",
    ),
)

internal fun exportMember(
    id: String,
    name: String = id.replaceFirstChar { it.uppercase() },
): GroupMember = GroupMember(
    id = id,
    name = name,
    avatarColor = AvatarColor.BLUE,
    isMe = id == PIERRE,
    hasJoined = id != BRUNO,
    userId = null,
    inviteCode = null,
    photoUrl = null,
)

internal fun exportGroup(
    name: String = "Trip to Rio",
    id: String = EXPORT_GROUP_ID,
    currency: GroupCurrency = GroupCurrency.BRL,
    members: List<GroupMember> = listOf(exportMember(PIERRE), exportMember(ANA), exportMember(BRUNO)),
): Group = Group(
    id = id,
    name = name,
    type = GroupType.TRIP,
    color = AvatarColor.GREEN,
    shape = GroupShape.SQUARE,
    photo = null,
    currency = currency,
    members = members,
    isPending = false,
    inviteCode = null,
    isCreatedByMe = true,
    defaultSplit = null,
    updatedAt = 0L,
    simplifiesDebts = false,
    isSimplifyDebtsPending = false,
    simplifyDebtsChangedBy = null,
    simplifyDebtsConflictBy = null,
)

internal fun exportBill(
    id: String,
    amountMinor: Long,
    payers: Map<String, Long> = mapOf(PIERRE to amountMinor),
    shares: Map<String, Long> = mapOf(ANA to amountMinor),
    spentAt: Long = NOV_10_NOON,
    description: String = "Dinner",
    categoryId: String? = "food",
    currency: GroupCurrency = GroupCurrency.BRL,
    groupId: String = EXPORT_GROUP_ID,
): Expense = Expense(
    id = id,
    groupId = groupId,
    description = description,
    amountMinor = amountMinor,
    currency = currency,
    splitMode = SplitMode.AMOUNT,
    payers = payers.map { (memberId, amount) ->
        ExpensePayer(
            memberId = memberId,
            amountMinor = amount,
        )
    },
    shares = shares.map { (memberId, amount) ->
        ExpenseShare(
            memberId = memberId,
            amountMinor = amount,
            weight = amount,
        )
    },
    items = null,
    categoryId = categoryId,
    spentAt = spentAt,
    isPending = false,
    updatedAt = spentAt,
    updatedByMemberId = null,
)

internal fun exportPayment(
    id: String,
    amountMinor: Long,
    from: String = ANA,
    to: String = PIERRE,
    paidAt: Long = NOV_11_NOON,
    currency: GroupCurrency = GroupCurrency.BRL,
    method: SettlementMethod = SettlementMethod.PIX,
    groupId: String = EXPORT_GROUP_ID,
): Settlement = Settlement(
    id = id,
    groupId = groupId,
    fromMemberId = from,
    toMemberId = to,
    amountMinor = amountMinor,
    currency = currency,
    method = method,
    paidAt = paidAt,
    isPending = false,
    updatedAt = paidAt,
)

internal fun groupCategory(
    id: String,
    name: String,
    parentId: String? = null,
    isDeleted: Boolean = false,
): GroupCategory = GroupCategory(
    id = id,
    groupId = EXPORT_GROUP_ID,
    parentId = parentId,
    name = name,
    icon = null,
    color = null,
    createdByMemberId = PIERRE,
    isDeleted = isDeleted,
    isPending = false,
)

internal fun exportOf(
    group: Group = exportGroup(),
    bills: List<Expense> = emptyList(),
    payments: List<Settlement> = emptyList(),
    groupCategories: List<GroupCategory> = emptyList(),
    payingMethods: Map<String, PayingMethod> = emptyMap(),
    timeZone: TimeZone = TimeZone.UTC,
    nowMillis: Long = NOV_14_AFTERNOON,
): GroupExport = GroupExport.of(
    group = group,
    expenses = bills,
    settlements = payments,
    categoryTree = CategoryTree.of(groupCategories),
    payingMethods = payingMethods,
    timeZone = timeZone,
    nowMillis = nowMillis,
)

// Why: a cell by its column's header, so a test keeps passing when a section gains a column.
internal fun ExportTable.cellAt(
    row: Int,
    header: String,
): ExportCell {
    val column = headers.indexOf(header)
    check(column >= 0) { "No column \"$header\" in $headers" }
    return rows[row][column]
}
