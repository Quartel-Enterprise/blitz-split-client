package com.quare.blitzsplit.feature.billdetail.presentation.mapper

import com.quare.blitzsplit.core.activity.domain.model.ActivityChange
import com.quare.blitzsplit.core.activity.domain.model.ActivityEvent
import com.quare.blitzsplit.core.activity.domain.model.ActivityField
import com.quare.blitzsplit.core.activity.domain.model.ActivityKind
import com.quare.blitzsplit.core.activity.domain.model.ActivityValue
import com.quare.blitzsplit.core.expenses.domain.model.BillLinkVisibility
import com.quare.blitzsplit.core.expenses.domain.model.BillSplit
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitEntry
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillHistoryChange
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillHistoryDay
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillHistoryEntry
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillHistoryKind
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillHistoryPart
import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.minutes

/*
 * Why: photos one person adds within attachmentBurst of the run's latest one read as one line, "anexou 2 fotos";
 * undoableFields are what "Desfazer" puts back, so an edit of anything else can't be undone from here.
 */
private val attachmentBurst = 10.minutes
private const val RECEIPT = "receipt"
private val undoableFields = setOf(
    ActivityField.AMOUNT,
    ActivityField.DESCRIPTION,
    ActivityField.SPENT_AT,
    ActivityField.CATEGORY,
    ActivityField.PAYERS,
    ActivityField.SPLIT,
)

/*
 * Why: "Histórico" (7.3), the most recent first, by day: what the server recorded about the bill, and on top, while it
 * waits to go up, what this device did ("criou a conta sem internet"). Names and colors come from the group as it is
 * now, else from what the server wrote down.
 */
internal fun historyOf(
    events: List<ActivityEvent>,
    expense: Expense,
    group: Group,
    nowMillis: Long,
    timeZone: TimeZone,
): List<BillHistoryDay> {
    val undoId = events.firstOrNull()?.takeIf { undoOf(it, expense, group) != null }?.id
    val payersAfter = payersAfterEachOf(events, expense)
    val entries = listOfNotNull(pendingEntryOf(events, expense, group, nowMillis)) +
        buildAttachmentRuns(events).mapNotNull { (event, count) ->
            entryOf(event, count, group, expense, undoId, payersAfter.getValue(event.id))
        }
    return entries
        .groupBy { SpentDay.dateOf(it.occurredAt, timeZone) }
        .map { (date, dayEntries) ->
            BillHistoryDay(
                dayStartMillis = SpentDay.startOf(date, timeZone),
                relativeDay = SpentDay.relativeDayOf(date, nowMillis, timeZone),
                entries = dayEntries,
            )
        }.sortedByDescending { it.dayStartMillis }
}

/*
 * Why: what "Desfazer" saves for the latest edit, or null when it can't: the bill moved on since (another edit, one
 * waiting to go up), it is split by its items, the edit touched something it doesn't put back, or putting it back
 * would bring in someone who left the group, whose balance Sair do grupo had settled.
 */
internal fun undoOf(
    event: ActivityEvent,
    expense: Expense,
    group: Group,
): BillUndo? {
    if (event.kind != ActivityKind.BILL_EDITED || event.changes.isEmpty()) return null
    if (expense.isPending || expense.items != null) return null
    if (event.changes.any { it.field !in undoableFields || !expense.matches(it) }) return null
    // Why: a split by items is stored as Valores, so putting one back would lose its items.
    if (event.changes.any { it.field == ActivityField.SPLIT && it.before.code == SplitMode.AMOUNT.code }) return null
    val memberIds = group.members.mapTo(HashSet()) { it.id }
    if (event.changes.any { change -> change.before.memberIds.any { it !in memberIds } }) return null
    val changes = event.changes.associateBy { it.field }
    val amountMinor = changes[ActivityField.AMOUNT]?.before?.minor ?: expense.amountMinor
    val payers = changes[ActivityField.PAYERS]
        ?.before
        ?.memberValues
        ?.map { ExpensePayer(it.memberId, it.value) }
        ?: expense.payers
    if (payers.sumOf { it.amountMinor } != amountMinor) return null
    val split = changes[ActivityField.SPLIT]?.before
    val splitMode = split?.code?.let(SplitMode::fromCodeOrNull) ?: expense.splitMode
    val entries = split
        ?.memberValues
        ?.map { SplitEntry(it.memberId, it.value) }
        ?: expense.shares.map { SplitEntry(it.memberId, it.weight) }
    val shares = BillSplit.split(
        totalMinor = amountMinor,
        mode = splitMode,
        entries = entries,
        payerIds = payers.map { it.memberId },
    ) ?: return null
    return BillUndo(
        description = changes[ActivityField.DESCRIPTION]?.before?.text ?: expense.description,
        amountMinor = amountMinor,
        payers = payers,
        splitMode = splitMode,
        shares = shares,
        // Why: a "before" without a category (Outros) is put back too, so no fallback to the bill's.
        categoryId = changes[ActivityField.CATEGORY]?.before?.code.takeIf { ActivityField.CATEGORY in changes }
            ?: expense.categoryId.takeIf { ActivityField.CATEGORY !in changes },
        spentAt = changes[ActivityField.SPENT_AT]?.before?.millis ?: expense.spentAt,
    )
}

// Why: the bill is still as the edit left it, so putting "before" back undoes only that edit.
private fun Expense.matches(change: ActivityChange): Boolean {
    val after = change.after
    return when (change.field) {
        ActivityField.AMOUNT -> amountMinor == after.minor

        ActivityField.DESCRIPTION -> description == after.text

        ActivityField.SPENT_AT -> spentAt == after.millis

        ActivityField.CATEGORY -> categoryId == after.code

        ActivityField.PAYERS -> payers.sortedBy { it.memberId }.map { it.memberId to it.amountMinor } ==
            after.memberValues.sortedBy { it.memberId }.map { it.memberId to it.value }

        ActivityField.SPLIT -> SplitMode.storedCodeOf(splitMode) == after.code &&
            shares.sortedBy { it.memberId }.map { it.memberId to it.weight } ==
            after.memberValues.sortedBy { it.memberId }.map { it.memberId to it.value }

        else -> false
    }
}

// Why: a bill not up yet has no event for what this device did to it last; one never up reads as created.
private fun pendingEntryOf(
    events: List<ActivityEvent>,
    expense: Expense,
    group: Group,
    nowMillis: Long,
): BillHistoryEntry? {
    if (!expense.isPending) return null
    val me = group.members.firstOrNull { it.isMe }
    // Why: any event about it means the server has it, also once its creation is older than the device keeps.
    val isNew = events.isEmpty()
    val actor = actorOf(me, me?.name, isMe = true)
    return BillHistoryEntry(
        id = "pending:${expense.id}",
        kind = if (isNew) BillHistoryKind.CREATED else BillHistoryKind.EDITED,
        actorName = actor.name,
        actorInitials = actor.initials,
        actorColor = actor.color,
        actorPhotoUrl = actor.photoUrl,
        actorIsMe = true,
        actorMemberId = me?.id,
        occurredAt = expense.updatedAt.coerceAtMost(nowMillis),
        changes = emptyList(),
        amountMinor = expense.amountMinor,
        splitMode = expense.splitMode,
        sharesCount = expense.shares.count { it.weight > 0 },
        attachmentCount = 0,
        isReceipt = false,
        linkVisibility = null,
        isPending = true,
        canUndo = false,
    )
}

// Why: the photos one person added one after the other, the most recent first, as one line with their count.
private fun buildAttachmentRuns(events: List<ActivityEvent>): List<Pair<ActivityEvent, Int>> {
    val gathered = mutableListOf<Pair<ActivityEvent, Int>>()
    events.forEach { event ->
        val last = gathered.lastOrNull()
        val joinsLast = last != null &&
            event.kind == ActivityKind.ATTACHMENT_ADDED &&
            last.first.kind == ActivityKind.ATTACHMENT_ADDED &&
            event.code != RECEIPT &&
            last.first.code != RECEIPT &&
            event.actorMemberId == last.first.actorMemberId &&
            event.actorUserId == last.first.actorUserId &&
            last.first.occurredAt - event.occurredAt <= attachmentBurst.inWholeMilliseconds
        if (joinsLast) {
            gathered[gathered.lastIndex] = last.first to last.second + 1
        } else {
            gathered += event to 1
        }
    }
    return gathered
}

/*
 * Why: who paid right after each event, from the bill as it is now back through each change of the payers, so an old
 * split's cents go to the payers the bill had then.
 */
private fun payersAfterEachOf(
    events: List<ActivityEvent>,
    expense: Expense,
): Map<String, List<String>> {
    var payerIds = expense.payers.map { it.memberId }
    return events.associate { event ->
        val after = payerIds
        event.changes.firstOrNull { it.field == ActivityField.PAYERS }?.let { payerIds = it.before.memberIds }
        event.id to after
    }
}

private fun entryOf(
    event: ActivityEvent,
    attachmentCount: Int,
    group: Group,
    expense: Expense,
    undoId: String?,
    payerIds: List<String>,
): BillHistoryEntry? {
    val kind = when (event.kind) {
        ActivityKind.BILL_CREATED -> BillHistoryKind.CREATED
        ActivityKind.BILL_EDITED -> BillHistoryKind.EDITED
        ActivityKind.ATTACHMENT_ADDED -> BillHistoryKind.ATTACHMENTS_ADDED
        ActivityKind.ATTACHMENT_REMOVED -> BillHistoryKind.ATTACHMENT_REMOVED
        ActivityKind.BILL_LINK_CHANGED -> BillHistoryKind.LINK_CHANGED
        else -> return null
    }
    val members = group.members
    val actorMember = event.actorMemberId?.let { id -> members.firstOrNull { it.id == id } }
        ?: event.actorUserId?.let { userId -> members.firstOrNull { it.userId == userId } }
    val actor = actorOf(actorMember, event.actorName, isMe = actorMember?.isMe == true)
    return BillHistoryEntry(
        id = event.id,
        kind = kind,
        actorName = actor.name,
        actorInitials = actor.initials,
        actorColor = actor.color,
        actorPhotoUrl = actor.photoUrl,
        actorIsMe = actorMember?.isMe == true,
        actorMemberId = actorMember?.id,
        occurredAt = event.occurredAt,
        changes = changesOf(event, group, expense, payerIds),
        amountMinor = event.amountMinor.takeIf { kind == BillHistoryKind.CREATED },
        splitMode = event.code?.takeIf { kind == BillHistoryKind.CREATED }?.let(SplitMode::fromCodeOrNull),
        sharesCount = event.sharesCount.takeIf { kind == BillHistoryKind.CREATED },
        attachmentCount = attachmentCount,
        isReceipt = event.code == RECEIPT && kind != BillHistoryKind.LINK_CHANGED,
        linkVisibility = BillLinkVisibility.fromCodeOrNull(event.code).takeIf { kind == BillHistoryKind.LINK_CHANGED },
        isPending = event.isPending,
        canUndo = event.id == undoId,
    )
}

private fun changesOf(
    event: ActivityEvent,
    group: Group,
    expense: Expense,
    payerIds: List<String>,
): List<BillHistoryChange> {
    if (event.kind != ActivityKind.BILL_EDITED) return emptyList()
    val byField = event.changes.associateBy { it.field }
    return event.changes.mapNotNull { change ->
        when (change.field) {
            ActivityField.AMOUNT -> BillHistoryChange.Amount(change.before.minor, change.after.minor)
            ActivityField.DESCRIPTION -> BillHistoryChange.Description(change.before.text, change.after.text)
            ActivityField.SPENT_AT -> BillHistoryChange.SpentAt(change.before.millis, change.after.millis)
            ActivityField.CATEGORY -> BillHistoryChange.Category(change.before.code, change.after.code)
            ActivityField.PAYERS -> payersChangeOf(change, group)
            ActivityField.SPLIT -> splitChangeOf(change, byField, event, group, expense, payerIds)
            else -> null
        }
    }
}

/*
 * Why: other payers are named. The same payers paying other amounts list each amount that moved, except one payer,
 * whose amount is the total's change, already listed.
 */
private fun payersChangeOf(
    change: ActivityChange,
    group: Group,
): BillHistoryChange.Payers? {
    val isSamePayers = change.before.memberIds.toSet() == change.after.memberIds.toSet()
    val parts = if (isSamePayers) partsOf(change.before, change.after, group) else emptyList()
    if (isSamePayers && (change.after.memberIds.size < 2 || parts.isEmpty())) return null
    return BillHistoryChange.Payers(
        before = namesOf(change.before, group),
        after = namesOf(change.after, group),
        parts = parts,
    )
}

/*
 * Why: each part that moved, as each side's split gave it out of each side's total, the leftover cents to the payers
 * of that side as Rachar conta does. A side that doesn't add up names the modes only.
 */
private fun splitChangeOf(
    change: ActivityChange,
    byField: Map<ActivityField, ActivityChange>,
    event: ActivityEvent,
    group: Group,
    expense: Expense,
    payerIds: List<String>,
): BillHistoryChange.Split {
    val afterTotal = byField[ActivityField.AMOUNT]?.after?.minor ?: event.amountMinor ?: expense.amountMinor
    val beforeTotal = byField[ActivityField.AMOUNT]?.before?.minor ?: afterTotal
    val payers = byField[ActivityField.PAYERS]
    val before = partsOf(
        side = change.before,
        totalMinor = beforeTotal,
        payerIds = payers?.before?.memberValues?.map { it.memberId } ?: payerIds,
    )
    val after = partsOf(
        side = change.after,
        totalMinor = afterTotal,
        payerIds = payers?.after?.memberValues?.map { it.memberId } ?: payerIds,
    )
    val parts = movedPartsOf(before, after, group)
    return BillHistoryChange.Split(
        beforeMode = change.before.code?.let(SplitMode::fromCodeOrNull),
        afterMode = change.after.code?.let(SplitMode::fromCodeOrNull),
        parts = parts.takeIf { before.isNotEmpty() && after.isNotEmpty() }.orEmpty(),
    )
}

private fun partsOf(
    before: ActivityValue,
    after: ActivityValue,
    group: Group,
): List<BillHistoryPart> = movedPartsOf(
    before = before.memberValues.associate { it.memberId to it.value },
    after = after.memberValues.associate { it.memberId to it.value },
    group = group,
)

// Why: in the group's order, someone who left last; a side without someone is null there.
private fun movedPartsOf(
    before: Map<String, Long>,
    after: Map<String, Long>,
    group: Group,
): List<BillHistoryPart> {
    val membersById = group.members.associateBy { it.id }
    val order = group.members.map { it.id }
    return (before.keys + after.keys)
        .distinct()
        .sortedBy { id -> order.indexOf(id).takeIf { it >= 0 } ?: order.size }
        .filter { before[it] != after[it] }
        .map { id ->
            val member = membersById[id]
            BillHistoryPart(
                name = member?.name,
                isMe = member?.isMe == true,
                beforeMinor = before[id],
                afterMinor = after[id],
            )
        }
}

private fun partsOf(
    side: ActivityValue,
    totalMinor: Long,
    payerIds: List<String>,
): Map<String, Long> {
    val mode = side.code?.let(SplitMode::fromCodeOrNull) ?: return emptyMap()
    return BillSplit
        .split(
            totalMinor = totalMinor,
            mode = mode,
            entries = side.memberValues.map { SplitEntry(it.memberId, it.value) },
            payerIds = payerIds,
        ).orEmpty()
        .associate { it.memberId to it.amountMinor }
}

private fun namesOf(
    value: ActivityValue,
    group: Group,
): List<String?> = value.memberIds.map { id -> group.members.firstOrNull { it.id == id }?.name }

private fun actorOf(
    member: GroupMember?,
    fallbackName: String?,
    isMe: Boolean,
): HistoryActor {
    val name = member?.name ?: fallbackName
    return HistoryActor(
        name = name,
        initials = name?.let { Initials.ofMember(it, isMe) }.orEmpty(),
        color = member?.avatarColor ?: AvatarColor.default,
        photoUrl = member?.photoUrl,
    )
}
