package com.quare.blitzsplit.feature.activity.presentation.mapper

import com.quare.blitzsplit.core.activity.domain.model.ActivityChange
import com.quare.blitzsplit.core.activity.domain.model.ActivityDebt
import com.quare.blitzsplit.core.activity.domain.model.ActivityEvent
import com.quare.blitzsplit.core.activity.domain.model.ActivityFeed
import com.quare.blitzsplit.core.activity.domain.model.ActivityField
import com.quare.blitzsplit.core.activity.domain.model.ActivityKind
import com.quare.blitzsplit.core.activity.domain.model.ActivityReadState
import com.quare.blitzsplit.core.activity.domain.model.ActivityTargets
import com.quare.blitzsplit.core.activity.domain.model.ActivityType
import com.quare.blitzsplit.core.activity.domain.model.ActivityValue
import com.quare.blitzsplit.core.activity.domain.model.QueuedChange
import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.NotificationPreferences
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityChangeRow
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityChangeValue
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityDebtChange
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityDeletion
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityFilter
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityItem
import com.quare.blitzsplit.feature.activity.presentation.model.ActivitySimplification
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityTarget
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ActivityItemsTest {
    @Test
    fun `GIVEN queued changes and events WHEN mapping the feed THEN lists the queued ones first latest first`() {
        // Given
        val feed = feed(
            events = listOf(
                event(
                    id = "newer",
                    seq = 2L,
                ),
                event(
                    id = "older",
                    seq = 1L,
                ),
            ),
            queued = listOf(
                queued(
                    id = "queued-old",
                    savedAt = NOW - 3 * DAY,
                ),
                queued(
                    id = "queued-new",
                    savedAt = NOW - HOUR,
                ),
            ),
        )

        // When
        val entries = entriesOf(feed, groups)

        // Then
        assertEquals(listOf("queued-new", "queued-old", "newer", "older"), entries.map { it.item.id })
    }

    @Test
    fun `GIVEN a queued bill WHEN mapping the feed THEN shows it waiting read and done by you`() {
        // Given
        val feed = feed(queued = listOf(queued(id = BILL_MERCADO)))

        // When
        val item = entriesOf(feed, groups).single().item

        // Then
        assertTrue(item.isPending)
        assertFalse(item.isUnread)
        assertTrue(item.actor.isMe)
        assertEquals("Pierre", item.actor.name)
        assertEquals("Apê 302", item.groupName)
        assertEquals(GroupCurrency.BRL, item.currency)
        assertEquals(ActivityTarget.BILL, item.target)
        assertEquals(BILL_MERCADO, item.targetId)
        assertNull(item.deletion)
    }

    @Test
    fun `GIVEN a payment in the queue WHEN mapping the feed THEN names its payer and receiver`() {
        // Given
        val feed = feed(
            queued = listOf(
                queued(
                    id = "settlement-1",
                    kind = ActivityKind.PAYMENT_RECORDED,
                    fromMemberId = ME_APE,
                    toMemberId = ANA_APE,
                ),
            ),
        )

        // When
        val entry = entriesOf(feed, groups).single()

        // Then
        assertEquals("Pierre", entry.item.fromName)
        assertTrue(entry.item.fromIsMe)
        assertEquals("Ana", entry.item.toName)
        assertFalse(entry.item.toIsMe)
        assertNull(entry.item.targetId)
        assertEquals(setOf("user:pierre", "user:ana"), entry.personKeys)
    }

    @Test
    fun `GIVEN a change in the queue of a group no longer on the device WHEN mapping the feed THEN leaves it out`() {
        // Given
        val feed = feed(
            queued = listOf(
                queued(
                    id = "bill-gone",
                    groupId = PRAIA,
                ),
            ),
        )

        // When
        val entries = entriesOf(feed, groups)

        // Then
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `GIVEN an event by a member WHEN mapping the feed THEN names the member as the group has them now`() {
        // Given
        val feed = feed(
            events = listOf(
                event(
                    id = "e1",
                    actorMemberId = ANA_APE,
                    actorName = "Ana antiga",
                ),
            ),
        )

        // When
        val actor = entriesOf(feed, groups).single().item.actor

        // Then
        assertEquals("Ana", actor.name)
        assertFalse(actor.isMe)
        assertEquals("A", actor.initials)
        assertEquals(AvatarColor.GREEN, actor.color)
    }

    @Test
    fun `GIVEN an event with an unknown member WHEN mapping the feed THEN finds the actor by account`() {
        // Given
        val feed = feed(
            events = listOf(
                event(
                    id = "e1",
                    actorMemberId = "unknown",
                    actorUserId = "bruno",
                    actorName = "Bruno antigo",
                ),
            ),
        )

        // When
        val actor = entriesOf(feed, groups).single().item.actor

        // Then
        assertEquals("Bruno", actor.name)
        assertEquals(AvatarColor.LILAC, actor.color)
    }

    @Test
    fun `GIVEN an actor no longer in the group WHEN mapping the feed THEN names them as the server wrote down`() {
        // Given
        val feed = feed(
            events = listOf(
                event(
                    id = "e1",
                    actorMemberId = "gone-member",
                    actorUserId = "gone-user",
                    actorName = "Elisa",
                ),
            ),
        )

        // When
        val entry = entriesOf(feed, groups).single()

        // Then
        assertEquals("Elisa", entry.item.actor.name)
        assertFalse(entry.item.actor.isMe)
        assertEquals(AvatarColor.default, entry.item.actor.color)
        assertTrue("user:gone-user" in entry.personKeys)
    }

    @Test
    fun `GIVEN an account deleted before it had a member WHEN mapping the feed THEN the actor has no name`() {
        // Given
        val feed = feed(
            events = listOf(
                event(
                    id = "e1",
                    actorMemberId = null,
                    actorUserId = null,
                    actorName = null,
                ),
            ),
        )

        // When
        val actor = entriesOf(feed, groups).single().item.actor

        // Then
        assertNull(actor.name)
        assertEquals("", actor.initials)
    }

    @Test
    fun `GIVEN your event in a group gone WHEN mapping the feed THEN marks the actor as you`() {
        // Given
        val feed = feed(
            events = listOf(
                event(
                    id = "e1",
                    groupId = PRAIA,
                    groupName = "Praia",
                    actorMemberId = "me-praia",
                    actorUserId = "pierre",
                    actorName = "Pierre",
                ),
            ),
        )

        // When
        val actor = entriesOf(feed, groups).single().item.actor

        // Then
        assertTrue(actor.isMe)
        assertEquals("Pierre", actor.name)
    }

    @Test
    fun `GIVEN a payment WHEN mapping the feed THEN names its payer and receiver`() {
        // Given
        val feed = feed(
            events = listOf(
                payment(
                    fromMemberId = ANA_APE,
                    toMemberId = ME_APE,
                ),
            ),
        )

        // When
        val item = entriesOf(feed, groups).single().item

        // Then
        assertEquals("Ana", item.fromName)
        assertFalse(item.fromIsMe)
        assertEquals("Pierre", item.toName)
        assertTrue(item.toIsMe)
    }

    @Test
    fun `GIVEN a payment to a former member WHEN mapping the feed THEN the receiver has no name`() {
        // Given
        val feed = feed(
            events = listOf(
                payment(
                    fromMemberId = ANA_APE,
                    toMemberId = "gone-member",
                ),
            ),
        )

        // When
        val item = entriesOf(feed, groups).single().item

        // Then
        assertEquals("Ana", item.fromName)
        assertNull(item.toName)
        assertFalse(item.toIsMe)
    }

    @Test
    fun `GIVEN a group renamed since the event WHEN mapping the feed THEN shows the group as it is now`() {
        // Given
        val feed = feed(
            events = listOf(
                event(
                    id = "e1",
                    groupName = "Apê antigo",
                ),
            ),
        )

        // When
        val item = entriesOf(feed, groups).single().item

        // Then
        assertEquals("Apê 302", item.groupName)
        assertEquals(AvatarColor.PEACH, item.groupColor)
        assertEquals(GroupType.HOME, item.groupType)
        assertEquals(GroupCurrency.BRL, item.currency)
    }

    @Test
    fun `GIVEN a group gone from the device WHEN mapping its event THEN shows the name the event wrote down`() {
        // Given
        val feed = feed(
            events = listOf(
                event(
                    id = "e1",
                    groupId = PRAIA,
                    groupName = "Praia",
                ),
            ),
        )

        // When
        val item = entriesOf(feed, groups).single().item

        // Then
        assertEquals("Praia", item.groupName)
        assertNull(item.groupColor)
        assertNull(item.groupType)
        assertNull(item.currency)
    }

    @Test
    fun `GIVEN an event of a group on the device WHEN mapping the feed THEN its group can be opened`() {
        // Given
        val feed = feed(events = listOf(payment()))

        // When
        val item = entriesOf(feed, groups).single().item

        // Then
        assertTrue(item.isGroupOnDevice)
    }

    @Test
    fun `GIVEN an event of a group deleted or left WHEN mapping the feed THEN its group cannot be opened`() {
        // Given
        val feed = feed(
            events = listOf(
                payment(
                    id = "payment",
                    groupId = PRAIA,
                    groupName = "Praia",
                ),
            ),
        )

        // When
        val item = entriesOf(feed, groups).single().item

        // Then
        assertFalse(item.isGroupOnDevice)
        assertEquals("Praia", item.groupName)
    }

    @Test
    fun `GIVEN a change waiting to be sent WHEN mapping the feed THEN its group can be opened`() {
        // Given
        val feed = feed(queued = listOf(queued(id = BILL_MERCADO)))

        // When
        val item = entriesOf(feed, groups).single().item

        // Then
        assertTrue(item.isGroupOnDevice)
    }

    @Test
    fun `GIVEN a bill deleted in a group still here WHEN mapping its creation THEN its group can be opened`() {
        // Given
        val feed = feed(
            events = listOf(
                event(
                    id = "pastel-created",
                    targetId = BILL_PASTEL,
                    subject = "Pastel",
                ),
            ),
        )

        // When
        val item = entriesOf(feed, groups).single().item

        // Then
        assertTrue(item.isGroupOnDevice)
    }

    @Test
    fun `GIVEN events around the read mark WHEN mapping the feed THEN only newer unopened ones of others are unread`() {
        // Given
        val feed = feed(
            events = listOf(
                event(
                    id = "mine",
                    seq = 14L,
                    actorUserId = "pierre",
                    actorMemberId = ME_APE,
                ),
                event(
                    id = "opened",
                    seq = 13L,
                ),
                event(
                    id = "new",
                    seq = 12L,
                ),
                event(
                    id = "under-mark",
                    seq = 5L,
                ),
            ),
            readThroughSeq = 10L,
            readEventIds = setOf("opened"),
        )

        // When
        val unread = entriesOf(feed, groups).map { it.item.id to it.item.isUnread }

        // Then
        assertEquals(
            listOf("mine" to false, "opened" to false, "new" to true, "under-mark" to false),
            unread,
        )
    }

    @Test
    fun `GIVEN an edited bill WHEN mapping the feed THEN shows each change with the members by name`() {
        // Given
        val feed = feed(
            events = listOf(
                event(
                    id = "edit",
                    kind = ActivityKind.BILL_EDITED,
                    changes = listOf(
                        ActivityChange(
                            field = ActivityField.AMOUNT,
                            before = ActivityValue.none.copy(minor = 61_200L),
                            after = ActivityValue.none.copy(minor = 64_200L),
                        ),
                        ActivityChange(
                            field = ActivityField.PAYERS,
                            before = ActivityValue.none.copy(memberIds = listOf(ME_APE)),
                            after = ActivityValue.none.copy(memberIds = listOf(ANA_APE, "gone-member")),
                        ),
                    ),
                ),
            ),
        )

        // When
        val changes = entriesOf(feed, groups).single().item.changes

        // Then
        assertEquals(
            listOf(
                ActivityChangeRow(
                    field = ActivityField.AMOUNT,
                    before = changeValue(minor = 61_200L),
                    after = changeValue(minor = 64_200L),
                ),
                ActivityChangeRow(
                    field = ActivityField.PAYERS,
                    before = changeValue(names = listOf("Pierre")),
                    after = changeValue(
                        names = listOf("Ana"),
                        hasFormerMember = true,
                    ),
                ),
            ),
            changes,
        )
    }

    @Test
    fun `GIVEN a bill someone deleted WHEN mapping its creation THEN says who deleted it and when`() {
        // Given
        val feed = feed(
            events = listOf(
                event(
                    id = "deleted",
                    kind = ActivityKind.BILL_DELETED,
                    targetId = BILL_PASTEL,
                    subject = "Pastel na praia",
                    actorMemberId = BRUNO_APE,
                    actorUserId = "bruno",
                    actorName = "Bruno",
                    occurredAt = NOW - HOUR,
                ),
                event(
                    id = "created",
                    targetId = BILL_PASTEL,
                    subject = "Pastel",
                    occurredAt = NOW - DAY,
                ),
            ),
            billIds = setOf(BILL_MERCADO),
        )

        // When
        val items = entriesOf(feed, groups).map { it.item }

        // Then
        val deletion = ActivityDeletion(
            isGroup = false,
            subject = "Pastel na praia",
            byName = "Bruno",
            byMe = false,
            atMillis = NOW - HOUR,
        )
        assertEquals(listOf(deletion, deletion), items.map { it.deletion })
        assertTrue(items.all { it.isTargetDeleted })
    }

    @Test
    fun `GIVEN a bill you deleted elsewhere WHEN mapping its creation THEN says you deleted it`() {
        // Given
        val feed = feed(
            events = listOf(
                event(
                    id = "deleted",
                    kind = ActivityKind.BILL_DELETED,
                    targetId = BILL_PASTEL,
                    actorMemberId = null,
                    actorUserId = "pierre",
                    actorName = "Pierre",
                ),
                event(
                    id = "created",
                    targetId = BILL_PASTEL,
                ),
            ),
            billIds = emptySet(),
        )

        // When
        val deletion = entriesOf(feed, groups).last().item.deletion

        // Then
        assertEquals("Pierre", deletion?.byName)
        assertEquals(true, deletion?.byMe)
    }

    @Test
    fun `GIVEN a bill deleted here not sent WHEN mapping its creation THEN says you deleted it when saved`() {
        // Given
        val feed = feed(
            events = listOf(
                event(
                    id = "created",
                    targetId = BILL_MERCADO,
                    subject = "Mercado",
                ),
            ),
            queued = listOf(
                queued(
                    id = BILL_MERCADO,
                    kind = ActivityKind.BILL_DELETED,
                    subject = "Mercado do mês",
                    savedAt = NOW - HOUR,
                ),
            ),
            billIds = emptySet(),
        )

        // When
        val items = entriesOf(feed, groups).map { it.item }

        // Then
        val deletion = ActivityDeletion(
            isGroup = false,
            subject = "Mercado do mês",
            byName = "Pierre",
            byMe = true,
            atMillis = NOW - HOUR,
        )
        assertEquals(listOf(deletion, deletion), items.map { it.deletion })
    }

    @Test
    fun `GIVEN a bill deleted before what the device keeps WHEN mapping its creation THEN the deletion names nobody`() {
        // Given
        val feed = feed(
            events = listOf(
                event(
                    id = "created",
                    targetId = BILL_PASTEL,
                    subject = "Pastel",
                ),
            ),
            billIds = emptySet(),
        )

        // When
        val deletion = entriesOf(feed, groups).single().item.deletion

        // Then
        assertEquals(
            ActivityDeletion(
                isGroup = false,
                subject = "Pastel",
                byName = null,
                byMe = false,
                atMillis = null,
            ),
            deletion,
        )
    }

    @Test
    fun `GIVEN a group someone deleted WHEN mapping its events THEN says who deleted the group and when`() {
        // Given
        val feed = feed(
            events = listOf(
                event(
                    id = "group-deleted",
                    kind = ActivityKind.GROUP_DELETED,
                    groupId = PRAIA,
                    groupName = "Praia 2026",
                    targetId = PRAIA,
                    actorMemberId = "ana-praia",
                    actorUserId = "ana",
                    actorName = "Ana",
                    occurredAt = NOW - HOUR,
                ),
                payment(
                    id = "payment",
                    groupId = PRAIA,
                    groupName = "Praia",
                ),
            ),
        )

        // When
        val deletion = entriesOf(feed, groups).last().item.deletion

        // Then
        assertEquals(
            ActivityDeletion(
                isGroup = true,
                subject = "Praia 2026",
                byName = "Ana",
                byMe = false,
                atMillis = NOW - HOUR,
            ),
            deletion,
        )
    }

    @Test
    fun `GIVEN a group this device user deleted WHEN mapping its events THEN says this device user deleted it`() {
        // Given
        val feed = feed(
            events = listOf(
                event(
                    id = "group-deleted",
                    kind = ActivityKind.GROUP_DELETED,
                    groupId = PRAIA,
                    groupName = "Praia",
                    actorMemberId = "me-praia",
                    actorUserId = "pierre",
                    actorName = "Pierre",
                ),
            ),
        )

        // When
        val deletion = entriesOf(feed, groups).single().item.deletion

        // Then
        assertEquals(true, deletion?.isGroup)
        assertEquals(true, deletion?.byMe)
    }

    @Test
    fun `GIVEN a group deleted before what the device keeps WHEN mapping its events THEN the deletion names nobody`() {
        // Given
        val feed = feed(
            events = listOf(
                payment(
                    id = "payment",
                    groupId = PRAIA,
                    groupName = "Praia",
                ),
            ),
        )

        // When
        val deletion = entriesOf(feed, groups).single().item.deletion

        // Then
        assertEquals(
            ActivityDeletion(
                isGroup = true,
                subject = "Praia",
                byName = null,
                byMe = false,
                atMillis = null,
            ),
            deletion,
        )
    }

    @Test
    fun `GIVEN a bill and a payment still on the device WHEN mapping the feed THEN neither has a deletion`() {
        // Given
        val feed = feed(
            events = listOf(
                event(
                    id = "bill",
                    targetId = BILL_MERCADO,
                ),
                payment(),
            ),
        )

        // When
        val deletions = entriesOf(feed, groups).map { it.item.deletion }

        // Then
        assertEquals(listOf(null, null), deletions)
    }

    @Test
    fun `GIVEN every kind WHEN mapping the feed THEN each opens what it is about`() {
        // Given
        val feed = feed(
            events = ActivityKind.entries.map { kind ->
                event(
                    id = kind.code,
                    kind = kind,
                    targetId = BILL_MERCADO,
                )
            },
        )

        // When
        val targets = entriesOf(feed, groups).associate { it.item.kind to it.item.target }

        // Then
        assertEquals(
            mapOf(
                ActivityKind.BILL_CREATED to ActivityTarget.BILL,
                ActivityKind.BILL_EDITED to ActivityTarget.BILL,
                ActivityKind.BILL_DELETED to ActivityTarget.BILL,
                ActivityKind.PAYMENT_RECORDED to ActivityTarget.GROUP,
                ActivityKind.PAYMENT_UNDONE to ActivityTarget.GROUP,
                ActivityKind.GROUP_CREATED to ActivityTarget.GROUP,
                ActivityKind.GROUP_EDITED to ActivityTarget.GROUP,
                ActivityKind.GROUP_DELETED to ActivityTarget.GROUP,
                ActivityKind.INVITE_LINK_CHANGED to ActivityTarget.MEMBERS,
                ActivityKind.MEMBER_ADDED to ActivityTarget.MEMBERS,
                ActivityKind.MEMBER_JOINED to ActivityTarget.MEMBERS,
                ActivityKind.MEMBER_LEFT to ActivityTarget.MEMBERS,
                ActivityKind.MEMBER_REMOVED to ActivityTarget.MEMBERS,
                ActivityKind.MEMBER_RELEASED to ActivityTarget.MEMBERS,
                ActivityKind.MEMBER_RENAMED to ActivityTarget.MEMBERS,
                ActivityKind.SIMPLIFY_DEBTS_CHANGED to ActivityTarget.BALANCES,
                ActivityKind.CHARGE_SENT to ActivityTarget.BALANCES,
                ActivityKind.COMMENT_ADDED to ActivityTarget.BILL,
                ActivityKind.ATTACHMENT_ADDED to ActivityTarget.BILL,
                ActivityKind.ATTACHMENT_REMOVED to ActivityTarget.BILL,
                ActivityKind.BILL_LINK_CHANGED to ActivityTarget.BILL,
            ),
            targets,
        )
    }

    @Test
    fun `GIVEN a payment Bruno recorded from Ana to Carla WHEN mapping the feed THEN it is about the three of them`() {
        // Given
        val feed = feed(
            events = listOf(
                payment(
                    actorMemberId = BRUNO_APE,
                    actorUserId = "bruno",
                    fromMemberId = ANA_APE,
                    toMemberId = CARLA_APE,
                ),
            ),
        )

        // When
        val personKeys = entriesOf(feed, groups).single().personKeys

        // Then
        assertEquals(setOf("user:bruno", "user:ana", "name:carla"), personKeys)
    }

    @Test
    fun `GIVEN a member added WHEN mapping the feed THEN it is about whoever added them and the member`() {
        // Given
        val feed = feed(
            events = listOf(
                event(
                    id = "added",
                    kind = ActivityKind.MEMBER_ADDED,
                    targetId = CARLA_APE,
                ),
            ),
        )

        // When
        val personKeys = entriesOf(feed, groups).single().personKeys

        // Then
        assertEquals(setOf("user:ana", "name:carla"), personKeys)
    }

    @Test
    fun `GIVEN a bill moved to a category of its group WHEN building the feed THEN names it from the group`() {
        // Given
        val beach = GroupCategory(
            id = "category-beach",
            groupId = FLORIPA,
            parentId = null,
            name = "Praia",
            icon = CategoryIcon.SUN,
            createdByMemberId = null,
            isDeleted = false,
            isPending = false,
        )
        val feed = feed(
            events = listOf(
                event(
                    id = "edit",
                    kind = ActivityKind.BILL_EDITED,
                    groupId = FLORIPA,
                    changes = listOf(
                        ActivityChange(
                            field = ActivityField.CATEGORY,
                            before = ActivityValue.none,
                            after = ActivityValue.none.copy(code = beach.id),
                        ),
                    ),
                ),
            ),
        )

        // When
        val change = entriesOf(feed, groups, CategoryTree.ofGroups(listOf(beach)))
            .single()
            .item.changes
            .single()

        // Then
        assertEquals(AppCategory.OTHER, change.before.category?.app)
        assertEquals("Praia", change.after.category?.name)
    }

    @Test
    fun `GIVEN entries of two groups WHEN filtering by one group THEN only its entries match`() {
        // Given
        val entries = entriesOf(
            feed(
                events = listOf(
                    event(id = "ape"),
                    event(
                        id = "floripa",
                        groupId = FLORIPA,
                    ),
                ),
            ),
            groups,
        )
        val filter = ActivityFilter.none.copy(groupId = FLORIPA)

        // When
        val matching = entries.filter { it.matches(filter) }

        // Then
        assertEquals(listOf("floripa"), matching.map { it.item.id })
    }

    @Test
    fun `GIVEN entries of several types WHEN filtering by types THEN only those types match`() {
        // Given
        val entries = entriesOf(
            feed(
                events = listOf(
                    event(id = "bill"),
                    payment(id = "payment"),
                    event(
                        id = "charge",
                        kind = ActivityKind.CHARGE_SENT,
                    ),
                    event(
                        id = "member",
                        kind = ActivityKind.MEMBER_JOINED,
                    ),
                ),
            ),
            groups,
        )
        val filter = ActivityFilter.none.copy(
            types = listOf(
                ActivityType.PAYMENTS,
                ActivityType.CHARGES,
            ),
        )

        // When
        val matching = entries.filter { it.matches(filter) }

        // Then
        assertEquals(listOf("payment", "charge"), matching.map { it.item.id })
    }

    @Test
    fun `GIVEN entries about several people WHEN filtering by a person THEN only those about them match`() {
        // Given
        val entries = entriesOf(
            feed(
                events = listOf(
                    event(id = "by-ana"),
                    event(
                        id = "by-bruno",
                        actorMemberId = BRUNO_APE,
                        actorUserId = "bruno",
                    ),
                    payment(
                        id = "to-ana",
                        actorMemberId = BRUNO_APE,
                        fromMemberId = ME_APE,
                        toMemberId = ANA_APE,
                    ),
                ),
            ),
            groups,
        )
        val filter = ActivityFilter.none.copy(personKey = "user:ana")

        // When
        val matching = entries.filter { it.matches(filter) }

        // Then
        assertEquals(listOf("by-ana", "to-ana"), matching.map { it.item.id })
    }

    @Test
    fun `GIVEN no filter WHEN filtering THEN every entry matches`() {
        // Given
        val entries = entriesOf(
            feed(
                events = listOf(
                    event(id = "ape"),
                    payment(
                        id = "floripa",
                        groupId = FLORIPA,
                    ),
                ),
            ),
            groups,
        )
        val filter = ActivityFilter.none

        // When
        val matching = entries.filter { it.matches(filter) }

        // Then
        assertEquals(2, matching.size)
    }

    @Test
    fun `GIVEN items of three days WHEN grouping by day THEN shows Hoje Ontem and the date latest first`() {
        // Given
        val items = listOf(
            item(
                id = "today",
                occurredAt = NOW - HOUR,
            ),
            item(
                id = "yesterday",
                occurredAt = NOW - DAY,
            ),
            item(
                id = "before",
                occurredAt = NOW - 3 * DAY,
            ),
        )

        // When
        val days = daysOf(items, NOW, TimeZone.UTC)

        // Then
        assertEquals(listOf(RelativeDay.TODAY, RelativeDay.YESTERDAY, RelativeDay.OTHER), days.map { it.relativeDay })
        assertEquals(listOf(TODAY_START, TODAY_START - DAY, TODAY_START - 3 * DAY), days.map { it.dayStartMillis })
        assertEquals(
            listOf(listOf("today"), listOf("yesterday"), listOf("before")),
            days.map { day ->
                day.items.map { it.id }
            },
        )
    }

    @Test
    fun `GIVEN a change waiting to be sent saved days ago WHEN grouping by day THEN it goes on top of Hoje`() {
        // Given
        val items = listOf(
            item(
                id = "queued",
                occurredAt = NOW - 3 * DAY,
                isPending = true,
            ),
            item(
                id = "today",
                occurredAt = NOW - HOUR,
            ),
            item(
                id = "before",
                occurredAt = NOW - 3 * DAY,
            ),
        )

        // When
        val days = daysOf(items, NOW, TimeZone.UTC)

        // Then
        assertEquals(RelativeDay.TODAY, days.first().relativeDay)
        assertEquals(listOf("queued", "today"), days.first().items.map { it.id })
        assertEquals(listOf("before"), days.last().items.map { it.id })
    }

    @Test
    fun `GIVEN a time zone behind UTC WHEN grouping by day THEN counts the days in that zone`() {
        // Given
        val items = listOf(
            item(
                id = "late-night",
                occurredAt = TODAY_START + HOUR,
            ),
        )

        // When
        // Why: a fixed offset, since the browser's tests have no time zone database to read a region from.
        val days = daysOf(items, NOW, TimeZone.of("UTC-03:00"))

        // Then
        assertEquals(RelativeDay.YESTERDAY, days.single().relativeDay)
    }

    @Test
    fun `GIVEN no items WHEN grouping by day THEN there are no days`() {
        // Given
        val items = emptyList<ActivityItem>()

        // When
        val days = daysOf(items, NOW, TimeZone.UTC)

        // Then
        assertTrue(days.isEmpty())
    }

    @Test
    fun `GIVEN someone who joined WHEN naming the person of a filter THEN names them from any group they are in`() {
        // Given
        val personKey = "user:diego"

        // When
        val name = personNameOf(personKey, groups)

        // Then
        assertEquals("Diego", name)
    }

    @Test
    fun `GIVEN a name nobody joined as WHEN naming the person of a filter THEN finds it whatever the case`() {
        // Given
        val personKey = "name:carla"

        // When
        val name = personNameOf(personKey, groups)

        // Then
        assertEquals("Carla", name)
    }

    @Test
    fun `GIVEN a person in no group WHEN naming the person of a filter THEN has no name`() {
        // Given
        val personKey = "user:elisa"

        // When
        val name = personNameOf(personKey, groups)

        // Then
        assertNull(name)
    }

    // region Simplificar dívidas

    @Test
    fun `GIVEN Simplificar dividas turned on WHEN mapping the feed THEN counts payments and what changed for you`() {
        // Given
        val feed = feed(
            events = listOf(
                simplifyEvent(
                    debtsBefore = listOf(
                        debt(CARLA_APE, BRUNO_APE, 6_400L),
                        debt(BRUNO_APE, ME_APE, 6_400L),
                        debt(ANA_APE, ME_APE, 5_000L),
                    ),
                    debtsAfter = listOf(
                        debt(CARLA_APE, ME_APE, 6_400L),
                        debt(ANA_APE, ME_APE, 5_000L),
                    ),
                ),
            ),
        )

        // When
        val simplification = entriesOf(feed, groups).single().item.simplification

        // Then
        assertEquals(
            ActivitySimplification(
                paymentsBefore = 3,
                paymentsAfter = 2,
                myChanges = listOf(
                    debtChange("Carla", 0L, 6_400L),
                    debtChange("Bruno", 6_400L, 0L),
                ),
            ),
            simplification,
        )
    }

    @Test
    fun `GIVEN you owed someone before and someone else after WHEN mapping the feed THEN what you owe is negative`() {
        // Given
        val feed = feed(
            events = listOf(
                simplifyEvent(
                    code = ActivityEvent.SIMPLIFY_OFF,
                    debtsBefore = listOf(debt(ME_APE, BRUNO_APE, 3_000L)),
                    debtsAfter = listOf(
                        debt(ME_APE, ANA_APE, 3_000L),
                        debt(ANA_APE, BRUNO_APE, 3_000L),
                    ),
                ),
            ),
        )

        // When
        val changes = entriesOf(feed, groups)
            .single()
            .item.simplification
            ?.myChanges

        // Then
        assertEquals(
            setOf(
                debtChange("Ana", 0L, -3_000L),
                debtChange("Bruno", -3_000L, 0L),
            ),
            changes?.toSet(),
        )
    }

    @Test
    fun `GIVEN changes of different sizes WHEN mapping the feed THEN lists the largest change first`() {
        // Given
        val feed = feed(
            events = listOf(
                simplifyEvent(
                    debtsBefore = listOf(
                        debt(ANA_APE, ME_APE, 5_000L),
                        debt(BRUNO_APE, ME_APE, 2_000L),
                    ),
                    debtsAfter = listOf(
                        debt(ANA_APE, ME_APE, 1_000L),
                        debt(BRUNO_APE, ME_APE, 9_000L),
                    ),
                ),
            ),
        )

        // When
        val changes = entriesOf(feed, groups)
            .single()
            .item.simplification
            ?.myChanges

        // Then
        assertEquals(
            listOf(
                debtChange("Bruno", 2_000L, 9_000L),
                debtChange("Ana", 5_000L, 1_000L),
            ),
            changes,
        )
    }

    @Test
    fun `GIVEN a balance with you that stayed the same WHEN mapping the feed THEN leaves that person out`() {
        // Given
        val feed = feed(
            events = listOf(
                simplifyEvent(
                    debtsBefore = listOf(
                        debt(ANA_APE, ME_APE, 5_000L),
                        debt(BRUNO_APE, CARLA_APE, 2_000L),
                    ),
                    debtsAfter = listOf(debt(ANA_APE, ME_APE, 5_000L)),
                ),
            ),
        )

        // When
        val item = entriesOf(feed, groups).single().item

        // Then
        assertEquals(emptyList(), item.simplification?.myChanges)
        assertEquals(2, item.simplification?.paymentsBefore)
        assertEquals(1, item.simplification?.paymentsAfter)
        assertFalse(item.hasChangesToShow)
    }

    @Test
    fun `GIVEN a counterpart renamed since the event WHEN mapping the feed THEN names them as the group does now`() {
        // Given
        val feed = feed(
            events = listOf(
                simplifyEvent(
                    debtsBefore = emptyList(),
                    debtsAfter = listOf(debt(BRUNO_APE, ME_APE, 2_000L)),
                    memberNames = mapOf(BRUNO_APE to "Bruno antigo"),
                ),
            ),
        )

        // When
        val change = entriesOf(feed, groups)
            .single()
            .item.simplification
            ?.myChanges
            ?.single()

        // Then
        assertEquals("Bruno", change?.name)
    }

    @Test
    fun `GIVEN a counterpart no longer in the group WHEN mapping the feed THEN names them as the event wrote down`() {
        // Given
        val feed = feed(
            events = listOf(
                simplifyEvent(
                    debtsBefore = listOf(debt("gone-member", ME_APE, 2_000L)),
                    debtsAfter = emptyList(),
                    memberNames = mapOf("gone-member" to "Elisa"),
                ),
            ),
        )

        // When
        val change = entriesOf(feed, groups)
            .single()
            .item.simplification
            ?.myChanges
            ?.single()

        // Then
        assertEquals(debtChange("Elisa", 2_000L, 0L), change)
    }

    @Test
    fun `GIVEN a counterpart nobody knows the name of WHEN mapping the feed THEN the change has no name`() {
        // Given
        val feed = feed(
            events = listOf(
                simplifyEvent(
                    debtsBefore = listOf(debt("gone-member", ME_APE, 2_000L)),
                    debtsAfter = emptyList(),
                ),
            ),
        )

        // When
        val change = entriesOf(feed, groups)
            .single()
            .item.simplification
            ?.myChanges
            ?.single()

        // Then
        assertEquals(debtChange(null, 2_000L, 0L), change)
    }

    @Test
    fun `GIVEN Simplificar dividas in a group not on the device WHEN mapping the feed THEN only counts payments`() {
        // Given
        val feed = feed(
            events = listOf(
                simplifyEvent(
                    groupId = PRAIA,
                    groupName = "Praia",
                    debtsBefore = listOf(
                        debt("ana-praia", "me-praia", 5_000L),
                        debt("bruno-praia", "ana-praia", 5_000L),
                    ),
                    debtsAfter = listOf(debt("bruno-praia", "me-praia", 5_000L)),
                    memberNames = mapOf("ana-praia" to "Ana", "bruno-praia" to "Bruno", "me-praia" to "Pierre"),
                ),
            ),
        )

        // When
        val item = entriesOf(feed, groups).single().item

        // Then
        assertEquals(
            ActivitySimplification(
                paymentsBefore = 2,
                paymentsAfter = 1,
                myChanges = emptyList(),
            ),
            item.simplification,
        )
        assertFalse(item.hasChangesToShow)
    }

    @Test
    fun `GIVEN Simplificar dividas switched here and not sent WHEN mapping the feed THEN waits with no detail`() {
        // Given
        val feed = feed(
            queued = listOf(
                queued(
                    id = APE,
                    kind = ActivityKind.SIMPLIFY_DEBTS_CHANGED,
                    subject = "Apê 302",
                    code = ActivityEvent.SIMPLIFY_ON,
                ),
            ),
        )

        // When
        val item = entriesOf(feed, groups).single().item

        // Then
        assertTrue(item.isPending)
        assertNull(item.simplification)
        assertTrue(item.isSimplifyTurnedOn)
        assertFalse(item.hasChangesToShow)
        assertEquals(ActivityTarget.BALANCES, item.target)
    }

    @Test
    fun `GIVEN an event of another kind WHEN mapping the feed THEN has no simplification`() {
        // Given
        val feed = feed(
            events = listOf(
                event(
                    id = "edit",
                    kind = ActivityKind.GROUP_EDITED,
                    debtsBefore = listOf(debt(ANA_APE, ME_APE, 5_000L)),
                ),
            ),
        )

        // When
        val simplification = entriesOf(feed, groups).single().item.simplification

        // Then
        assertNull(simplification)
    }

    @Test
    fun `GIVEN Simplificar dividas turned on WHEN mapping the feed THEN says it was turned on`() {
        // Given
        val feed = feed(events = listOf(simplifyEvent(code = ActivityEvent.SIMPLIFY_ON)))

        // When
        val item = entriesOf(feed, groups).single().item

        // Then
        assertTrue(item.isSimplifyTurnedOn)
    }

    @Test
    fun `GIVEN Simplificar dividas turned off WHEN mapping the feed THEN says it was turned off`() {
        // Given
        val feed = feed(events = listOf(simplifyEvent(code = ActivityEvent.SIMPLIFY_OFF)))

        // When
        val item = entriesOf(feed, groups).single().item

        // Then
        assertFalse(item.isSimplifyTurnedOn)
    }

    @Test
    fun `GIVEN Simplificar dividas that changed something for you WHEN mapping the feed THEN offers Ver mudancas`() {
        // Given
        val feed = feed(
            events = listOf(
                simplifyEvent(
                    debtsBefore = listOf(debt(BRUNO_APE, ME_APE, 2_000L)),
                    debtsAfter = listOf(debt(ANA_APE, ME_APE, 2_000L)),
                ),
            ),
        )

        // When
        val item = entriesOf(feed, groups).single().item

        // Then
        assertTrue(item.hasChangesToShow)
    }

    @Test
    fun `GIVEN an edited bill with changes WHEN mapping the feed THEN offers Ver mudancas`() {
        // Given
        val feed = feed(
            events = listOf(
                event(
                    id = "edit",
                    kind = ActivityKind.BILL_EDITED,
                    changes = listOf(
                        ActivityChange(
                            field = ActivityField.AMOUNT,
                            before = ActivityValue.none.copy(minor = 61_200L),
                            after = ActivityValue.none.copy(minor = 64_200L),
                        ),
                    ),
                ),
            ),
        )

        // When
        val item = entriesOf(feed, groups).single().item

        // Then
        assertTrue(item.hasChangesToShow)
    }

    @Test
    fun `GIVEN an edited bill with no change written down WHEN mapping the feed THEN offers no Ver mudancas`() {
        // Given
        val feed = feed(
            events = listOf(
                event(
                    id = "edit",
                    kind = ActivityKind.BILL_EDITED,
                ),
            ),
        )

        // When
        val item = entriesOf(feed, groups).single().item

        // Then
        assertFalse(item.hasChangesToShow)
    }

    @Test
    fun `GIVEN a group edited with changes WHEN mapping the feed THEN offers no Ver mudancas`() {
        // Given
        val feed = feed(
            events = listOf(
                event(
                    id = "edit",
                    kind = ActivityKind.GROUP_EDITED,
                    changes = listOf(
                        ActivityChange(
                            field = ActivityField.NAME,
                            before = ActivityValue.none.copy(text = "Apê"),
                            after = ActivityValue.none.copy(text = "Apê 302"),
                        ),
                    ),
                ),
            ),
        )

        // When
        val item = entriesOf(feed, groups).single().item

        // Then
        assertFalse(item.hasChangesToShow)
    }

    @Test
    fun `GIVEN Simplificar dividas changed WHEN filtering by type THEN Pessoas e grupos keeps it and Pagamentos not`() {
        // Given
        val entry = entriesOf(feed(events = listOf(simplifyEvent())), groups).single()
        val peopleAndGroups = ActivityFilter.none.copy(types = listOf(ActivityType.PEOPLE_AND_GROUPS))
        val payments = ActivityFilter.none.copy(types = listOf(ActivityType.PAYMENTS))

        // When
        val matches = listOf(entry.matches(peopleAndGroups), entry.matches(payments))

        // Then
        assertEquals(listOf(true, false), matches)
    }

    // endregion

    private val groups = listOf(
        group(
            id = APE,
            name = "Apê 302",
            type = GroupType.HOME,
            color = AvatarColor.PEACH,
            currency = GroupCurrency.BRL,
            member(
                ME_APE,
                "Pierre",
                AvatarColor.BLUE,
                userId = "pierre",
                isMe = true,
            ),
            member(ANA_APE, "Ana", AvatarColor.GREEN, userId = "ana"),
            member(BRUNO_APE, "Bruno", AvatarColor.LILAC, userId = "bruno"),
            member(CARLA_APE, "Carla", AvatarColor.SAND, userId = null),
        ),
        group(
            id = FLORIPA,
            name = "Viagem Floripa",
            type = GroupType.TRIP,
            color = AvatarColor.GREEN,
            currency = GroupCurrency.USD,
            member(
                "me-floripa",
                "Pierre",
                AvatarColor.BLUE,
                userId = "pierre",
                isMe = true,
            ),
            member("diego-floripa", "Diego", AvatarColor.SAND, userId = "diego"),
        ),
    )

    private fun member(
        id: String,
        name: String,
        color: AvatarColor,
        userId: String?,
        isMe: Boolean = false,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = color,
        isMe = isMe,
        hasJoined = userId != null,
        userId = userId,
        inviteCode = null,
        photoUrl = null,
    )

    private fun group(
        id: String,
        name: String,
        type: GroupType,
        color: AvatarColor,
        currency: GroupCurrency,
        vararg members: GroupMember,
    ): Group = Group(
        id = id,
        name = name,
        type = type,
        color = color,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = currency,
        members = members.toList(),
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

    // Why: the app's categories only, for the cases that don't name one.
    private fun entriesOf(
        feed: ActivityFeed,
        groups: List<Group>,
    ): List<FeedEntry> = entriesOf(feed, groups, CategoryTree.appOnly)

    private fun feed(
        events: List<ActivityEvent> = emptyList(),
        queued: List<QueuedChange> = emptyList(),
        readThroughSeq: Long = 0L,
        readEventIds: Set<String> = emptySet(),
        billIds: Set<String> = setOf(BILL_MERCADO),
    ): ActivityFeed = ActivityFeed(
        events = events,
        queued = queued,
        readState = ActivityReadState(
            readThroughSeq = readThroughSeq,
            readEventIds = readEventIds,
            hasDownloaded = true,
        ),
        targets = ActivityTargets(
            billIds = billIds,
            groupIds = setOf(APE, FLORIPA),
        ),
        myUserId = "pierre",
        hasOlder = false,
        inAppTypes = NotificationPreferences.DEFAULT.inApp,
    )

    private fun event(
        id: String,
        kind: ActivityKind = ActivityKind.BILL_CREATED,
        seq: Long? = 1L,
        groupId: String = APE,
        groupName: String? = "Apê 302",
        actorMemberId: String? = ANA_APE,
        actorUserId: String? = "ana",
        actorName: String? = "Ana",
        targetId: String? = BILL_MERCADO,
        subject: String? = "Mercado",
        occurredAt: Long = NOW,
        fromMemberId: String? = null,
        toMemberId: String? = null,
        changes: List<ActivityChange> = emptyList(),
        code: String? = null,
        debtsBefore: List<ActivityDebt> = emptyList(),
        debtsAfter: List<ActivityDebt> = emptyList(),
        memberNames: Map<String, String> = emptyMap(),
    ): ActivityEvent = ActivityEvent(
        id = id,
        seq = seq,
        groupId = groupId,
        kind = kind,
        actorUserId = actorUserId,
        actorMemberId = actorMemberId,
        actorName = actorName,
        groupName = groupName,
        targetId = targetId,
        occurredAt = occurredAt,
        subject = subject,
        amountMinor = 10_000L,
        code = code,
        fromMemberId = fromMemberId,
        toMemberId = toMemberId,
        changes = changes,
        sharesCount = null,
        body = null,
        isPending = false,
        commentId = null,
        isCommentDeleted = false,
        debtsBefore = debtsBefore,
        debtsAfter = debtsAfter,
        memberNames = memberNames,
    )

    // Why: "Simplificar dívidas" switched in Apê 302 by Ana.
    private fun simplifyEvent(
        code: String? = ActivityEvent.SIMPLIFY_ON,
        groupId: String = APE,
        groupName: String? = "Apê 302",
        debtsBefore: List<ActivityDebt> = emptyList(),
        debtsAfter: List<ActivityDebt> = emptyList(),
        memberNames: Map<String, String> = emptyMap(),
    ): ActivityEvent = event(
        id = "simplify",
        kind = ActivityKind.SIMPLIFY_DEBTS_CHANGED,
        groupId = groupId,
        groupName = groupName,
        targetId = null,
        subject = groupName,
        code = code,
        debtsBefore = debtsBefore,
        debtsAfter = debtsAfter,
        memberNames = memberNames,
    )

    private fun debt(
        from: String,
        to: String,
        amountMinor: Long,
    ): ActivityDebt = ActivityDebt(
        fromMemberId = from,
        toMemberId = to,
        amountMinor = amountMinor,
    )

    private fun debtChange(
        name: String?,
        beforeMinor: Long,
        afterMinor: Long,
    ): ActivityDebtChange = ActivityDebtChange(
        name = name,
        beforeMinor = beforeMinor,
        afterMinor = afterMinor,
    )

    private fun payment(
        id: String = "payment",
        groupId: String = APE,
        groupName: String = "Apê 302",
        actorMemberId: String = ANA_APE,
        actorUserId: String = "ana",
        fromMemberId: String = ANA_APE,
        toMemberId: String = ME_APE,
    ): ActivityEvent = event(
        id = id,
        kind = ActivityKind.PAYMENT_RECORDED,
        groupId = groupId,
        groupName = groupName,
        actorMemberId = actorMemberId,
        actorUserId = actorUserId,
        targetId = "settlement-1",
        subject = null,
        fromMemberId = fromMemberId,
        toMemberId = toMemberId,
    )

    private fun queued(
        id: String,
        kind: ActivityKind = ActivityKind.BILL_CREATED,
        groupId: String = APE,
        subject: String? = "Mercado",
        fromMemberId: String? = null,
        toMemberId: String? = null,
        savedAt: Long = NOW,
        code: String? = null,
    ): QueuedChange = QueuedChange(
        id = id,
        groupId = groupId,
        kind = kind,
        subject = subject,
        amountMinor = 10_000L,
        code = code,
        fromMemberId = fromMemberId,
        toMemberId = toMemberId,
        savedAt = savedAt,
    )

    private fun changeValue(
        minor: Long? = null,
        names: List<String> = emptyList(),
        hasFormerMember: Boolean = false,
    ): ActivityChangeValue = ActivityChangeValue(
        minor = minor,
        currency = GroupCurrency.BRL,
        text = null,
        millis = null,
        code = null,
        names = names,
        hasFormerMember = hasFormerMember,
        category = null,
    )

    private fun item(
        id: String,
        occurredAt: Long,
        isPending: Boolean = false,
    ): ActivityItem = entriesOf(
        feed(
            events = listOf(
                event(
                    id = id,
                    occurredAt = occurredAt,
                ),
            ),
        ),
        groups,
    ).single().item.copy(isPending = isPending)

    private companion object {
        const val HOUR = 3_600_000L
        const val DAY = 24 * HOUR

        // Why: 2026-10-30T00:00:00Z, a Friday.
        const val TODAY_START = 1_793_318_400_000L
        const val NOW = TODAY_START + 15 * HOUR
        const val APE = "ape"
        const val FLORIPA = "floripa"
        const val PRAIA = "praia"
        const val ME_APE = "me-ape"
        const val ANA_APE = "ana-ape"
        const val BRUNO_APE = "bruno-ape"
        const val CARLA_APE = "carla-ape"
        const val BILL_MERCADO = "bill-mercado"
        const val BILL_PASTEL = "bill-pastel"
    }
}
