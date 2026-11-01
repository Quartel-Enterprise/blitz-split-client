package com.quare.blitzsplit.feature.activity.presentation.mapper

import com.quare.blitzsplit.core.activity.domain.model.ActivityChange
import com.quare.blitzsplit.core.activity.domain.model.ActivityEvent
import com.quare.blitzsplit.core.activity.domain.model.ActivityFeed
import com.quare.blitzsplit.core.activity.domain.model.ActivityKind
import com.quare.blitzsplit.core.activity.domain.model.ActivityType
import com.quare.blitzsplit.core.activity.domain.model.ActivityValue
import com.quare.blitzsplit.core.activity.domain.model.QueuedChange
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityActor
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityChangeRow
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityChangeValue
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityDay
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityDeletion
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityItem
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityTarget
import kotlinx.datetime.TimeZone

// Why: the events whose targetId is a member: someone joined, renamed or removed, and whoever a charge went to.
private val subjectMemberTypes = setOf(ActivityType.PEOPLE_AND_GROUPS, ActivityType.CHARGES)

/*
 * Why: the feed as rows: the changes queued on this device first (they go on top of Hoje), then the events, the most
 * recent first. Names, colors and the currency come from the group as it is now; once a group or a member is gone,
 * from what the server wrote down when it happened.
 */
internal fun entriesOf(
    feed: ActivityFeed,
    groups: List<Group>,
): List<FeedEntry> {
    val groupsById = groups.associateBy { it.id }
    val deletions = DeletionIndex(feed, groupsById)
    val queued = feed.queued
        .sortedByDescending { it.savedAt }
        .mapNotNull { change -> groupsById[change.groupId]?.let { queuedEntryOf(change, it, deletions) } }
    val events = feed.events.map { event ->
        eventEntryOf(
            event = event,
            group = groupsById[event.groupId],
            isUnread = feed.readState.isUnread(event, feed.myUserId),
            myUserId = feed.myUserId,
            deletions = deletions,
        )
    }
    return queued + events
}

// Why: "Hoje", "Ontem" and the dates; the queued changes stay on top of Hoje whenever they were saved.
internal fun daysOf(
    items: List<ActivityItem>,
    nowMillis: Long,
    timeZone: TimeZone,
): List<ActivityDay> {
    val today = SpentDay.dateOf(nowMillis, timeZone)
    return items
        .groupBy { item -> if (item.isPending) today else SpentDay.dateOf(item.occurredAt, timeZone) }
        .map { (date, dayItems) ->
            ActivityDay(
                dayStartMillis = SpentDay.startOf(date, timeZone),
                relativeDay = SpentDay.relativeDayOf(date, nowMillis, timeZone),
                items = dayItems,
            )
        }.sortedByDescending { it.dayStartMillis }
}

// Why: the name a person filter shows: the member with that key in any group, as Saldos and Contatos name them.
internal fun personNameOf(
    personKey: String,
    groups: List<Group>,
): String? = groups
    .asSequence()
    .flatMap { it.members }
    .firstOrNull { PeopleBalances.keyOf(it) == personKey }
    ?.name

private fun queuedEntryOf(
    change: QueuedChange,
    group: Group,
    deletions: DeletionIndex,
): FeedEntry {
    val me = group.members.firstOrNull { it.isMe }
    val from = change.fromMemberId?.let { id -> group.members.firstOrNull { it.id == id } }
    val to = change.toMemberId?.let { id -> group.members.firstOrNull { it.id == id } }
    val targetId = change.id.takeIf { change.kind.isAboutBill }
    val item = ActivityItem(
        id = change.id,
        kind = change.kind,
        actor = actorOf(
            member = me,
            fallbackName = me?.name,
            isMe = true,
        ),
        groupId = group.id,
        groupName = group.name,
        groupColor = group.color,
        groupType = group.type,
        occurredAt = change.savedAt,
        subject = change.subject,
        amountMinor = change.amountMinor,
        currency = group.currency,
        code = change.code,
        fromName = from?.name,
        fromIsMe = from?.isMe == true,
        toName = to?.name,
        toIsMe = to?.isMe == true,
        changes = emptyList(),
        isPending = true,
        isUnread = false,
        target = targetOf(change.kind),
        deletion = if (change.kind == ActivityKind.BILL_DELETED) deletions.ofQueuedBill(change, group) else null,
        targetId = targetId,
    )
    return FeedEntry(item, keysOf(me, from, to))
}

private fun eventEntryOf(
    event: ActivityEvent,
    group: Group?,
    isUnread: Boolean,
    myUserId: String?,
    deletions: DeletionIndex,
): FeedEntry {
    val members = group?.members.orEmpty()
    val actorMember = event.actorMemberId?.let { id -> members.firstOrNull { it.id == id } }
        ?: event.actorUserId?.let { userId -> members.firstOrNull { it.userId == userId } }
    val isMe = actorMember?.isMe ?: (myUserId != null && event.actorUserId == myUserId)
    val from = event.fromMemberId?.let { id -> members.firstOrNull { it.id == id } }
    val to = event.toMemberId?.let { id -> members.firstOrNull { it.id == id } }
    val subjectMember = event.targetId
        ?.takeIf { event.kind.type in subjectMemberTypes }
        ?.let { id -> members.firstOrNull { it.id == id } }
    val item = ActivityItem(
        id = event.id,
        kind = event.kind,
        actor = actorOf(
            member = actorMember,
            fallbackName = event.actorName,
            isMe = isMe,
        ),
        groupId = event.groupId,
        groupName = group?.name ?: event.groupName,
        groupColor = group?.color,
        groupType = group?.type,
        occurredAt = event.occurredAt,
        subject = event.subject,
        amountMinor = event.amountMinor,
        currency = group?.currency,
        code = event.code,
        fromName = from?.name,
        fromIsMe = from?.isMe == true,
        toName = to?.name,
        toIsMe = to?.isMe == true,
        changes = event.changes.map { rowOf(it, group) },
        isPending = event.isPending,
        isUnread = isUnread,
        target = targetOf(event.kind),
        deletion = deletions.of(event, group),
        targetId = event.targetId,
    )
    val actorKey = actorMember?.let(PeopleBalances::keyOf) ?: event.actorUserId?.let { "user:$it" }
    return FeedEntry(item, keysOf(from, to, subjectMember) + setOfNotNull(actorKey))
}

private fun actorOf(
    member: GroupMember?,
    fallbackName: String?,
    isMe: Boolean,
): ActivityActor {
    val name = member?.name ?: fallbackName
    return ActivityActor(
        name = name,
        isMe = isMe,
        initials = name?.let { Initials.ofMember(it, isMe) }.orEmpty(),
        color = member?.avatarColor ?: AvatarColor.default,
    )
}

private fun keysOf(vararg members: GroupMember?): Set<String> = members
    .filterNotNull()
    .map(PeopleBalances::keyOf)
    .toSet()

private fun targetOf(kind: ActivityKind): ActivityTarget = when (kind) {
    ActivityKind.BILL_CREATED,
    ActivityKind.BILL_EDITED,
    ActivityKind.BILL_DELETED,
    -> ActivityTarget.BILL

    // Why: a payment shows in Despesas as an "acerto", where it is undone.
    ActivityKind.PAYMENT_RECORDED,
    ActivityKind.PAYMENT_UNDONE,
    ActivityKind.GROUP_CREATED,
    ActivityKind.GROUP_EDITED,
    ActivityKind.GROUP_DELETED,
    -> ActivityTarget.GROUP

    ActivityKind.INVITE_LINK_CHANGED,
    ActivityKind.MEMBER_ADDED,
    ActivityKind.MEMBER_JOINED,
    ActivityKind.MEMBER_LEFT,
    ActivityKind.MEMBER_REMOVED,
    ActivityKind.MEMBER_RELEASED,
    ActivityKind.MEMBER_RENAMED,
    -> ActivityTarget.MEMBERS

    // Why: what is still owed in the group, until Contatos (page 11) opens the person.
    ActivityKind.CHARGE_SENT -> ActivityTarget.BALANCES
}

private fun rowOf(
    change: ActivityChange,
    group: Group?,
): ActivityChangeRow = ActivityChangeRow(
    field = change.field,
    before = valueOf(change.before, group),
    after = valueOf(change.after, group),
)

private fun valueOf(
    value: ActivityValue,
    group: Group?,
): ActivityChangeValue {
    val names = value.memberIds.map { id -> group?.members?.firstOrNull { it.id == id }?.name }
    return ActivityChangeValue(
        minor = value.minor,
        currency = group?.currency,
        text = value.text,
        millis = value.millis,
        code = value.code,
        names = names.filterNotNull(),
        hasFormerMember = null in names,
    )
}

/*
 * Why: who deleted what an item is about. A group gone from the device was deleted by its group_deleted event, a bill
 * by its bill_deleted event or by this device's tombstone not up yet; older than the device keeps, nobody is named.
 */
private class DeletionIndex(
    private val feed: ActivityFeed,
    private val groupsById: Map<String, Group>,
) {
    private val groupDeletions = feed.events
        .filter { it.kind == ActivityKind.GROUP_DELETED }
        .associateBy { it.groupId }
    private val billDeletions = feed.events
        .filter { it.kind == ActivityKind.BILL_DELETED && it.targetId != null }
        .associateBy { it.targetId }
    private val queuedBillDeletions = feed.queued
        .filter { it.kind == ActivityKind.BILL_DELETED }
        .associateBy { it.id }

    fun of(
        event: ActivityEvent,
        group: Group?,
    ): ActivityDeletion? {
        if (event.groupId !in feed.targets.groupIds) {
            val deleted = groupDeletions[event.groupId]
            return ActivityDeletion(
                isGroup = true,
                subject = group?.name ?: deleted?.groupName ?: event.groupName,
                byName = deleted?.actorName,
                byMe = deleted != null && deleted.actorUserId == feed.myUserId,
                atMillis = deleted?.occurredAt,
            )
        }
        val billId = event.targetId?.takeIf { event.kind.isAboutBill } ?: return null
        if (billId in feed.targets.billIds) return null
        queuedBillDeletions[billId]?.let { queued -> return ofQueuedBill(queued, group) }
        val deleted = billDeletions[billId]
        val deleter = deleted?.let { found ->
            group?.members?.firstOrNull { it.id == found.actorMemberId }
                ?: group?.members?.firstOrNull { found.actorUserId != null && it.userId == found.actorUserId }
        }
        return ActivityDeletion(
            isGroup = false,
            subject = deleted?.subject ?: event.subject,
            byName = deleter?.name ?: deleted?.actorName,
            byMe = deleter?.isMe ?: (deleted != null && deleted.actorUserId == feed.myUserId),
            atMillis = deleted?.occurredAt,
        )
    }

    fun ofQueuedBill(
        change: QueuedChange,
        group: Group?,
    ): ActivityDeletion = ActivityDeletion(
        isGroup = false,
        subject = change.subject,
        byName = (group ?: groupsById[change.groupId])?.members?.firstOrNull { it.isMe }?.name,
        byMe = true,
        atMillis = change.savedAt,
    )
}
