package com.quare.blitzsplit.shared.storescreenshots

import androidx.room3.executeSQL
import androidx.room3.useWriterConnection
import com.quare.blitzsplit.core.expenses.domain.model.BillItemsSplit
import com.quare.blitzsplit.core.expenses.domain.model.BillSplit
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.SplitEntry
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.ConsumeRecordedPayment
import com.quare.blitzsplit.core.expenses.domain.usecase.CreateExpense
import com.quare.blitzsplit.core.expenses.domain.usecase.HoldPickedReceipt
import com.quare.blitzsplit.core.expenses.domain.usecase.RecordSettlement
import com.quare.blitzsplit.core.groups.domain.model.NewGroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.CreateGroup
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.SetGroupPinned
import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.profile.domain.usecase.SaveProfile
import com.quare.blitzsplit.core.provider.room.db.AppDatabase
import com.quare.blitzsplit.core.provider.room.entity.SINGLE_ROW_ID
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateAppLanguage
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateLastNotificationPromptAt
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateThemePreference
import com.quare.blitzsplit.shared.bridge.BlitzSplitApple
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.todayIn
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import platform.Foundation.NSData
import platform.Foundation.base64EncodedStringWithOptions
import platform.Foundation.dataWithContentsOfFile
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

/*
 * Why: writes the sample trip (SampleTrips) into a fresh Room the way a synced device holds it: everyone joined, with
 * a face, nothing waiting to go up, the invite link and the feed already downloaded. The use cases write it, so it is
 * what the app itself would store; SQL only does what a sync would (clear the pending flags, the server's fields). The
 * receipt's photo waits for Split a bill as Início's "Scan a receipt" leaves it.
 */
internal class StoreScreenshotsSeed(
    private val trip: SampleTrip,
    private val assetsDirectory: String?,
) : KoinComponent {
    private val database: AppDatabase = get()
    private val now = Clock.System.now()
    private val feedDelay = 5.minutes
    private val statements = mutableListOf<String>()

    /*
     * Why: the day the story calls today. A run before today's last moment (the 10:00 payment) moves the whole story a
     * day back, rather than a moment into the future or out of its order, so "Today" and the order never depend on when
     * the capture runs.
     */
    private val storyToday = run {
        val startOfToday = Clock.System
            .todayIn(TimeZone.currentSystemDefault())
            .atStartOfDayIn(TimeZone.currentSystemDefault())
        val latestTodayMinute = (
            trip.bills.filter { it.daysAgo == 0 }.map { it.minuteOfDay } +
                listOfNotNull(trip.payment.minuteOfDay.takeIf { trip.payment.daysAgo == 0 })
        ).maxOrNull() ?: 0
        val isTooEarly = now < startOfToday + latestTodayMinute.minutes + feedDelay
        if (isTooEarly) startOfToday - 1.days else startOfToday
    }

    suspend fun createTrip(
        language: AppLanguage,
        theme: ThemePreference,
    ): SeededTrip {
        get<UpdateAppLanguage>().invoke(language)
        get<UpdateThemePreference>().invoke(theme)
        get<UpdateLastNotificationPromptAt>().invoke(now.toEpochMilliseconds())
        get<SaveProfile>().invoke(UserProfile(trip.people.first(), SampleTrips.avatarColorOf(0), photo = null))
        val tripMembers = createGroup(trip.groupName, GroupType.TRIP, AvatarColor.PEACH, trip.people.indices.toList())
        val tripId = tripMembers.groupId
        val billIds = trip.bills.map { bill -> createBill(tripMembers, bill) }
        val paymentId = get<RecordSettlement>()
            .invoke(
                groupId = tripId,
                fromMemberId = tripMembers.idOf(trip.payment.fromPerson),
                toMemberId = tripMembers.idOf(trip.payment.toPerson),
                amountMinor = trip.payment.amountMinor,
                currency = trip.currency,
                method = trip.payment.method,
                paidAt = momentOf(trip.payment.daysAgo, trip.payment.minuteOfDay),
            ).getOrThrow()
        // Why: a payment recorded here shows "Liam's payment recorded · Undo" once, which no screenshot should.
        get<ConsumeRecordedPayment>().invoke(paymentId)
        val sideGroups = trip.sideGroups.mapIndexed { index, group ->
            val members =
                createGroup(
                    group.name,
                    group.type,
                    SampleTrips.sideGroupColors[index % SampleTrips.sideGroupColors.size],
                    group.people,
                )
            createBill(
                members,
                SampleBill(
                    description = group.billDescription,
                    totalMinor = group.billTotalMinor,
                    currency = trip.currency,
                    payers = listOf(SamplePayer(group.billPayer, group.billTotalMinor)),
                    categoryKey = group.categoryKey,
                    daysAgo = group.daysAgo,
                    minuteOfDay = SIDE_BILL_MINUTE,
                    items = null,
                ),
            )
            members
        }
        val setGroupPinned = get<SetGroupPinned>()
        (listOf(tripMembers) + sideGroups).forEach { setGroupPinned(it.groupId, isPinned = true).getOrThrow() }
        markSynced(tripId, tripMembers, sideGroups)
        writeFeed(tripMembers)
        database.useWriterConnection { connection -> statements.forEach { connection.executeSQL(it) } }
        assetsDirectory?.let { NSData.dataWithContentsOfFile("$it/receipt.jpg") }?.let { receipt ->
            get<HoldPickedReceipt>().invoke(BlitzSplitApple.toByteArray(receipt))
        }
        return SeededTrip(
            groupId = tripId,
            dinnerId = billIds[trip.bills.indexOfFirst { it.items != null }],
            foreignBillId = billIds[trip.bills.indexOfFirst { it.currency != trip.currency }],
        )
    }

    private suspend fun createGroup(
        name: String,
        type: GroupType,
        color: AvatarColor,
        people: List<Int>,
    ): SeededMembers {
        val groupId = get<CreateGroup>()
            .invoke(
                name = name,
                type = type,
                color = color,
                shape = GroupShape.fallback,
                photo = null,
                currency = trip.currency,
                members = people.map { NewGroupMember(trip.people[it], SampleTrips.avatarColorOf(it), isMe = it == 0) },
            ).getOrThrow()
        val group = get<ObserveGroup>().invoke(groupId).filterNotNull().first()
        return SeededMembers(
            groupId = groupId,
            idsByPerson = people.associateWith { person -> group.members.first { it.name == trip.people[person] }.id },
        )
    }

    private suspend fun createBill(
        members: SeededMembers,
        bill: SampleBill,
    ): String {
        val payers = bill.payers.map { ExpensePayer(members.idOf(it.person), it.amountMinor) }
        val payerIds = payers.map { it.memberId }
        val items = bill.items?.let { split ->
            split.copy(
                items = split.items.map { item ->
                    item.copy(entries = item.entries.map { it.copy(memberId = members.idOf(it.memberId.toInt())) })
                },
            )
        }
        val shares = if (items == null) {
            BillSplit.split(
                totalMinor = bill.totalMinor,
                mode = SplitMode.EQUAL,
                entries = members.idsByPerson.values.map { SplitEntry(it, 1) },
                payerIds = payerIds,
            )
        } else {
            BillItemsSplit.sharesOf(items, payerIds)
        }
        return get<CreateExpense>()
            .invoke(
                groupId = members.groupId,
                description = bill.description,
                amountMinor = bill.totalMinor,
                currency = bill.currency,
                payers = payers,
                splitMode = if (items == null) SplitMode.EQUAL else SplitMode.ITEMS,
                shares = checkNotNull(shares) { "${bill.description} doesn't split" },
                items = items,
                categoryId = bill.categoryKey,
                spentAt = momentOf(bill.daysAgo, bill.minuteOfDay),
            ).getOrThrow()
    }

    private fun markSynced(
        tripId: String,
        tripMembers: SeededMembers,
        sideGroups: List<SeededMembers>,
    ) {
        listOf("split_group", "group_member", "expense", "settlement", "group_pin", "profile").forEach { table ->
            statements += "UPDATE $table SET pendingSync = 0"
        }
        statements += "UPDATE split_group SET createdBy = ${toSqlLiteral(userIdOf(0))}, ownerUserId = NULL"
        statements +=
            "UPDATE split_group SET inviteCode = ${toSqlLiteral(
                SampleTrips.INVITE_CODE,
            )} WHERE id = ${toSqlLiteral(tripId)}"
        statements +=
            "UPDATE expense SET createdBy = ${toSqlLiteral(userIdOf(0))}, updatedBy = ${toSqlLiteral(userIdOf(0))}"
        statements += "UPDATE settlement SET createdBy = ${toSqlLiteral(userIdOf(0))}"
        /*
         * Why: each row dated when it happened rather than when the seed wrote it, as a synced device holds them, so
         * Início's "latest activity" order and its "6 days ago" read the trip's story.
         */
        statements += "UPDATE expense SET addedAt = spentAt, updatedAt = spentAt"
        statements += "UPDATE settlement SET addedAt = paidAt, updatedAt = paidAt"
        statements +=
            "UPDATE split_group SET addedAt = (SELECT MIN(spentAt) FROM expense WHERE groupId = split_group.id), " +
            "updatedAt = (SELECT MIN(spentAt) FROM expense WHERE groupId = split_group.id)"
        statements +=
            "UPDATE group_member SET addedAt = (SELECT addedAt FROM split_group WHERE id = group_member.groupId), " +
            "updatedAt = (SELECT addedAt FROM split_group WHERE id = group_member.groupId)"
        (listOf(tripMembers) + sideGroups).forEach { members ->
            members.idsByPerson.forEach { (person, memberId) ->
                statements +=
                    "UPDATE group_member SET userId = ${toSqlLiteral(
                        userIdOf(person),
                    )} WHERE id = ${toSqlLiteral(memberId)}"
            }
        }
        trip.people.indices.forEach { person ->
            val face = faceOf(person) ?: return@forEach
            statements += "INSERT OR REPLACE INTO member_photo (userId, photoUrl) VALUES " +
                "(${toSqlLiteral(userIdOf(person))}, ${toSqlLiteral(face)})"
            if (person == 0) statements += "UPDATE profile SET photoUrl = ${toSqlLiteral(face)}"
        }
    }

    /*
     * Why: Atividade as it comes down from the server, newest first: the payment to this person, the bills the
     * others added and who joined. Nothing read yet, so Início's bell counts them.
     */
    private fun writeFeed(members: SeededMembers) {
        val bills = trip.bills.filter { bill -> bill.payers.none { it.person == 0 } }
        val events = bills.map { bill ->
            FeedEvent(
                kind = "bill_created",
                actor = bill.payers.first().person,
                occurredAt = momentOf(bill.daysAgo, bill.minuteOfDay) + feedDelay.inWholeMilliseconds,
                data = buildJsonObject {
                    put("description", bill.description)
                    put("amount_minor", bill.totalMinor)
                    put("currency", bill.currency.code)
                    put("split_mode", SplitMode.EQUAL.code)
                },
            )
        } + FeedEvent(
            kind = "payment_recorded",
            actor = trip.payment.fromPerson,
            occurredAt = momentOf(trip.payment.daysAgo, trip.payment.minuteOfDay),
            data = buildJsonObject {
                put("amount_minor", trip.payment.amountMinor)
                put("currency", trip.currency.code)
                put("method", trip.payment.method.code)
                put("from_member_id", members.idOf(trip.payment.fromPerson))
                put("to_member_id", members.idOf(trip.payment.toPerson))
            },
        ) + trip.people.indices.drop(1).map { person ->
            FeedEvent(
                kind = "member_joined",
                actor = person,
                occurredAt = momentOf(JOINED_DAYS_AGO, JOINED_MINUTE + person * JOIN_GAP_MINUTES),
                data = buildJsonObject { put("name", trip.people[person]) },
            )
        }
        events.sortedBy { it.occurredAt }.forEachIndexed { index, event ->
            val data = buildJsonObject {
                put("actor_name", trip.people[event.actor])
                put("group_name", trip.groupName)
                event.data.forEach { (key, value) -> put(key, value) }
            }
            val values = listOf(
                toSqlLiteral("sample-event-$index"),
                "${index + 1}",
                toSqlLiteral(members.groupId),
                toSqlLiteral(event.kind),
                toSqlLiteral(userIdOf(event.actor)),
                toSqlLiteral(members.idOf(event.actor)),
                "NULL",
                "${event.occurredAt}",
                toSqlLiteral(data.toString()),
                "0",
            )
            statements += "INSERT INTO activity_event " +
                "(id, seq, groupId, kind, actorUserId, actorMemberId, targetId, occurredAt, data, pendingSync) " +
                "VALUES (${values.joinToString()})"
        }
        statements += "INSERT OR REPLACE INTO activity_state " +
            "(id, ownerUserId, readThroughSeq, markUpdatedAt, markPendingSync, hasDownloaded) VALUES " +
            "(${toSqlLiteral(SINGLE_ROW_ID)}, NULL, 0, 0, 0, 1)"
    }

    // Why: a face as a data URI, which the avatars load like any photo's URL; none when the folder isn't there.
    private fun faceOf(person: Int): String? {
        val directory = assetsDirectory ?: return null
        val data = NSData.dataWithContentsOfFile("$directory/faces/person$person.jpg") ?: return null
        return "data:image/jpeg;base64," + data.base64EncodedStringWithOptions(0u)
    }

    private fun momentOf(
        daysAgo: Int,
        minuteOfDay: Int,
    ): Long {
        val moment = storyToday - daysAgo.days + minuteOfDay.minutes
        return moment.toEpochMilliseconds()
    }

    private fun userIdOf(person: Int): String = "sample-user-$person"

    private fun toSqlLiteral(text: String): String = "'" + text.replace("'", "''") + "'"

    private companion object {
        const val SIDE_BILL_MINUTE = 19 * 60
        const val JOINED_DAYS_AGO = 5
        const val JOINED_MINUTE = 20 * 60
        const val JOIN_GAP_MINUTES = 7
    }
}
