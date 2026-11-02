package com.quare.blitzsplit.feature.billdetail.presentation.mapper

import com.quare.blitzsplit.core.activity.domain.model.ActivityChange
import com.quare.blitzsplit.core.activity.domain.model.ActivityEvent
import com.quare.blitzsplit.core.activity.domain.model.ActivityField
import com.quare.blitzsplit.core.activity.domain.model.ActivityKind
import com.quare.blitzsplit.core.activity.domain.model.ActivityMemberValue
import com.quare.blitzsplit.core.activity.domain.model.ActivityValue
import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.BillItems
import com.quare.blitzsplit.core.expenses.domain.model.BillLinkVisibility
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillHistoryChange
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillHistoryEntry
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillHistoryKind
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillHistoryPart
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class BillHistoriesTest {
    @Test
    fun `GIVEN the bill was created by Ana WHEN building the history THEN says she created it and how`() {
        // Given
        val events = listOf(
            event(
                kind = ActivityKind.BILL_CREATED,
                code = SplitMode.EQUAL.code,
                sharesCount = 3,
            ),
        )

        // When
        val entry = entriesOf(events).single()

        // Then
        assertEquals(BillHistoryKind.CREATED, entry.kind)
        assertEquals(48_000, entry.amountMinor)
        assertEquals(SplitMode.EQUAL, entry.splitMode)
        assertEquals(3, entry.sharesCount)
        assertEquals("Ana", entry.actorName)
        assertEquals("AN", entry.actorInitials)
        assertEquals(AvatarColor.PEACH, entry.actorColor)
        assertFalse(entry.actorIsMe)
        assertFalse(entry.isPending)
        assertFalse(entry.canUndo)
        assertTrue(entry.changes.isEmpty())
    }

    @Test
    fun `GIVEN the device user created the bill WHEN building the history THEN the line is theirs`() {
        // Given
        val events = listOf(
            event(
                kind = ActivityKind.BILL_CREATED,
                actorMemberId = PIERRE,
                actorName = "Pierre",
            ),
        )

        // When
        val entry = entriesOf(events).single()

        // Then
        assertTrue(entry.actorIsMe)
        assertEquals("Pierre", entry.actorName)
        assertEquals("P", entry.actorInitials)
    }

    @Test
    fun `GIVEN Ana renamed since editing WHEN building the history THEN names her as the group has her now`() {
        // Given
        val events = listOf(
            event(
                kind = ActivityKind.ATTACHMENT_REMOVED,
                actorName = "Ana antiga",
            ),
        )
        val renamed = group.copy(members = group.members.map { if (it.id == ANA) it.copy(name = "Ana Paula") else it })

        // When
        val entry = historyOf(events, expense(), renamed, now, TimeZone.UTC).single().entries.single()

        // Then
        assertEquals("Ana Paula", entry.actorName)
        assertEquals("AP", entry.actorInitials)
    }

    @Test
    fun `GIVEN an edit by someone who left the group WHEN building the history THEN names them as the server kept`() {
        // Given
        val events = listOf(
            event(
                kind = ActivityKind.ATTACHMENT_REMOVED,
                actorMemberId = FORMER,
                actorName = "Diego",
            ),
        )

        // When
        val entry = entriesOf(events).single()

        // Then
        assertEquals("Diego", entry.actorName)
        assertEquals("DI", entry.actorInitials)
        assertFalse(entry.actorIsMe)
    }

    @Test
    fun `GIVEN events of today yesterday and last week WHEN building the history THEN groups them by day`() {
        // Given
        val events = listOf(
            event(
                id = "today",
                kind = ActivityKind.ATTACHMENT_REMOVED,
                occurredAt = now - HOUR,
            ),
            event(
                id = "yesterday",
                kind = ActivityKind.ATTACHMENT_REMOVED,
                occurredAt = yesterdayEvening,
            ),
            event(
                id = "week",
                kind = ActivityKind.BILL_CREATED,
                occurredAt = lastWeek,
            ),
        )

        // When
        val days = historyOf(events, expense(), group, now, TimeZone.UTC)

        // Then
        assertEquals(listOf(RelativeDay.TODAY, RelativeDay.YESTERDAY, RelativeDay.OTHER), days.map { it.relativeDay })
        assertEquals(
            listOf("2026-10-30T00:00:00Z", "2026-10-29T00:00:00Z", "2026-10-20T00:00:00Z").map(::millisOf),
            days.map { it.dayStartMillis },
        )
        assertEquals(
            listOf(listOf("today"), listOf("yesterday"), listOf("week")),
            days.map { day -> day.entries.map { it.id } },
        )
    }

    @Test
    fun `GIVEN two events of the same day WHEN building the history THEN they share the day latest first`() {
        // Given
        val events = listOf(
            event(
                id = "newer",
                kind = ActivityKind.ATTACHMENT_REMOVED,
                occurredAt = now - HOUR,
            ),
            event(
                id = "older",
                kind = ActivityKind.BILL_CREATED,
                occurredAt = now - 5 * HOUR,
            ),
        )

        // When
        val days = historyOf(events, expense(), group, now, TimeZone.UTC)

        // Then
        assertEquals(listOf("newer", "older"), days.single().entries.map { it.id })
    }

    @Test
    fun `GIVEN an event of yesterday in the device time zone WHEN building the history THEN it is of yesterday`() {
        // Given
        val saoPaulo = UtcOffset(hours = -3).asTimeZone()
        val events = listOf(
            event(
                kind = ActivityKind.BILL_CREATED,
                occurredAt = millisOf("2026-10-30T01:00:00Z"),
            ),
        )

        // When
        val day = historyOf(events, expense(), group, now, saoPaulo).single()

        // Then
        assertEquals(RelativeDay.YESTERDAY, day.relativeDay)
        assertEquals(millisOf("2026-10-29T03:00:00Z"), day.dayStartMillis)
    }

    @Test
    fun `GIVEN an edit of the amount description date and category WHEN building the history THEN lists each change`() {
        // Given
        val events = listOf(
            event(
                changes = listOf(
                    change(ActivityField.AMOUNT, minor(40_000), minor(48_000)),
                    change(ActivityField.DESCRIPTION, text("Feira"), text("Mercado")),
                    change(ActivityField.SPENT_AT, millis(lastWeek), millis(spentAt)),
                    change(ActivityField.CATEGORY, code(null), code(AppCategory.FOOD_MARKET.key)),
                ),
            ),
        )

        // When
        val entry = entriesOf(events).single()

        // Then
        assertEquals(BillHistoryKind.EDITED, entry.kind)
        assertEquals(
            listOf(
                BillHistoryChange.Amount(40_000, 48_000),
                BillHistoryChange.Description("Feira", "Mercado"),
                BillHistoryChange.SpentAt(lastWeek, spentAt),
                BillHistoryChange.Category(null, AppCategory.FOOD_MARKET.key),
            ),
            entry.changes,
        )
        assertNull(entry.amountMinor)
        assertNull(entry.splitMode)
        assertNull(entry.sharesCount)
    }

    @Test
    fun `GIVEN an edit of the payers WHEN building the history THEN names them as the group has them now`() {
        // Given
        val events = listOf(
            event(
                changes = listOf(
                    change(ActivityField.PAYERS, payers(BRUNO to 48_000), payers(ANA to 30_000, FORMER to 18_000)),
                ),
            ),
        )

        // When
        val entry = entriesOf(events).single()

        // Then
        assertEquals(listOf(BillHistoryChange.Payers(listOf("Bruno"), listOf("Ana", null), emptyList())), entry.changes)
    }

    @Test
    fun `GIVEN the same payers paying other amounts WHEN building the history THEN lists each amount that moved`() {
        // Given
        val events = listOf(
            event(
                changes = listOf(
                    change(
                        ActivityField.PAYERS,
                        payers(ANA to 30_000, BRUNO to 18_000),
                        payers(
                            ANA to 24_000,
                            BRUNO to 24_000,
                        ),
                    ),
                ),
            ),
        )

        // When
        val entry = entriesOf(events).single()

        // Then
        assertEquals(
            listOf(
                BillHistoryChange.Payers(
                    before = listOf("Ana", "Bruno"),
                    after = listOf("Ana", "Bruno"),
                    parts = listOf(
                        BillHistoryPart(
                            "Ana",
                            isMe = false,
                            beforeMinor = 30_000,
                            afterMinor = 24_000,
                        ),
                        BillHistoryPart(
                            "Bruno",
                            isMe = false,
                            beforeMinor = 18_000,
                            afterMinor = 24_000,
                        ),
                    ),
                ),
            ),
            entry.changes,
        )
    }

    @Test
    fun `GIVEN an edit of the amounts of a split WHEN building the history THEN lists only the parts that moved`() {
        // Given
        val events = listOf(
            event(
                changes = listOf(
                    change(
                        ActivityField.SPLIT,
                        split(SplitMode.AMOUNT, ANA to 20_000, PIERRE to 14_000, BRUNO to 14_000),
                        split(SplitMode.AMOUNT, ANA to 20_000, PIERRE to 10_000, BRUNO to 18_000),
                    ),
                ),
            ),
        )

        // When
        val entry = entriesOf(events).single()

        // Then
        assertEquals(
            listOf(
                BillHistoryChange.Split(
                    beforeMode = SplitMode.AMOUNT,
                    afterMode = SplitMode.AMOUNT,
                    parts = listOf(
                        BillHistoryPart(
                            "Pierre",
                            isMe = true,
                            beforeMinor = 14_000,
                            afterMinor = 10_000,
                        ),
                        BillHistoryPart(
                            "Bruno",
                            isMe = false,
                            beforeMinor = 14_000,
                            afterMinor = 18_000,
                        ),
                    ),
                ),
            ),
            entry.changes,
        )
    }

    @Test
    fun `GIVEN a split moved from Igual to Partes WHEN building the history THEN gives each part from the weights`() {
        // Given
        val events = listOf(
            event(
                changes = listOf(
                    change(
                        ActivityField.SPLIT,
                        split(SplitMode.EQUAL, ANA to 1, PIERRE to 1, BRUNO to 1),
                        split(SplitMode.SHARES, ANA to 2, PIERRE to 1, BRUNO to 1),
                    ),
                ),
            ),
        )

        // When
        val entry = entriesOf(events).single()

        // Then
        assertEquals(
            listOf(
                BillHistoryChange.Split(
                    beforeMode = SplitMode.EQUAL,
                    afterMode = SplitMode.SHARES,
                    parts = listOf(
                        BillHistoryPart(
                            "Ana",
                            isMe = false,
                            beforeMinor = 16_000,
                            afterMinor = 24_000,
                        ),
                        BillHistoryPart(
                            "Pierre",
                            isMe = true,
                            beforeMinor = 16_000,
                            afterMinor = 12_000,
                        ),
                        BillHistoryPart(
                            "Bruno",
                            isMe = false,
                            beforeMinor = 16_000,
                            afterMinor = 12_000,
                        ),
                    ),
                ),
            ),
            entry.changes,
        )
    }

    @Test
    fun `GIVEN the amount and the split changed together WHEN building THEN each side splits its own total`() {
        // Given
        val events = listOf(
            event(
                changes = listOf(
                    change(ActivityField.AMOUNT, minor(10_000), minor(48_000)),
                    change(
                        ActivityField.SPLIT,
                        split(SplitMode.EQUAL, ANA to 1, PIERRE to 1, BRUNO to 1, CARLA to 0),
                        split(SplitMode.EQUAL, ANA to 1, PIERRE to 1, BRUNO to 1, CARLA to 1),
                    ),
                ),
            ),
        )

        // When
        val split = entriesOf(events)
            .single()
            .changes
            .filterIsInstance<BillHistoryChange.Split>()
            .single()

        // Then
        assertEquals(
            listOf(
                BillHistoryPart(
                    "Ana",
                    isMe = false,
                    beforeMinor = 3_334,
                    afterMinor = 12_000,
                ),
                BillHistoryPart(
                    "Pierre",
                    isMe = true,
                    beforeMinor = 3_333,
                    afterMinor = 12_000,
                ),
                BillHistoryPart(
                    "Bruno",
                    isMe = false,
                    beforeMinor = 3_333,
                    afterMinor = 12_000,
                ),
                BillHistoryPart(
                    "Carla",
                    isMe = false,
                    beforeMinor = null,
                    afterMinor = 12_000,
                ),
            ),
            split.parts,
        )
    }

    @Test
    fun `GIVEN the split and the payers changed WHEN building the history THEN the cents left go to each side payer`() {
        // Given
        val events = listOf(
            event(
                amountMinor = 10_000,
                changes = listOf(
                    change(ActivityField.PAYERS, payers(ANA to 10_000), payers(BRUNO to 10_000)),
                    change(
                        ActivityField.SPLIT,
                        split(SplitMode.EQUAL, ANA to 1, PIERRE to 1, BRUNO to 1),
                        split(SplitMode.EQUAL, ANA to 0, PIERRE to 1, BRUNO to 1, CARLA to 1),
                    ),
                ),
            ),
        )

        // When
        val split = entriesOf(events)
            .single()
            .changes
            .filterIsInstance<BillHistoryChange.Split>()
            .single()

        // Then
        assertEquals(
            listOf(
                BillHistoryPart(
                    "Ana",
                    isMe = false,
                    beforeMinor = 3_334,
                    afterMinor = null,
                ),
                BillHistoryPart(
                    "Bruno",
                    isMe = false,
                    beforeMinor = 3_333,
                    afterMinor = 3_334,
                ),
                BillHistoryPart(
                    "Carla",
                    isMe = false,
                    beforeMinor = null,
                    afterMinor = 3_333,
                ),
            ),
            split.parts,
        )
    }

    @Test
    fun `GIVEN the payer changed after an edit of the split WHEN building THEN the old payer gets its cents`() {
        // Given
        val events = listOf(
            event(
                id = "payer",
                amountMinor = 10_000,
                changes = listOf(change(ActivityField.PAYERS, payers(ANA to 10_000), payers(BRUNO to 10_000))),
            ),
            event(
                id = "split",
                occurredAt = now - 2 * HOUR,
                amountMinor = 10_000,
                changes = listOf(
                    change(
                        ActivityField.SPLIT,
                        split(SplitMode.EQUAL, ANA to 1, PIERRE to 1, CARLA to 1),
                        split(SplitMode.EQUAL, ANA to 1, PIERRE to 1, BRUNO to 1),
                    ),
                ),
            ),
        )
        val bill = expense(payers = listOf(ExpensePayer(BRUNO, 48_000)))

        // When
        val split = historyOf(events, bill, group, now, TimeZone.UTC)
            .flatMap { it.entries }
            .flatMap { it.changes }
            .filterIsInstance<BillHistoryChange.Split>()
            .single()

        // Then
        assertEquals(
            listOf(
                BillHistoryPart(
                    "Bruno",
                    isMe = false,
                    beforeMinor = null,
                    afterMinor = 3_333,
                ),
                BillHistoryPart(
                    "Carla",
                    isMe = false,
                    beforeMinor = 3_333,
                    afterMinor = null,
                ),
            ),
            split.parts,
        )
    }

    @Test
    fun `GIVEN a split whose percentages do not add up WHEN building the history THEN names the modes with no parts`() {
        // Given
        val events = listOf(
            event(
                changes = listOf(
                    change(
                        ActivityField.SPLIT,
                        split(SplitMode.PERCENT, ANA to 50, PIERRE to 50),
                        split(SplitMode.PERCENT, ANA to 60, PIERRE to 30),
                    ),
                ),
            ),
        )

        // When
        val entry = entriesOf(events).single()

        // Then
        assertEquals(
            listOf(BillHistoryChange.Split(SplitMode.PERCENT, SplitMode.PERCENT, parts = emptyList())),
            entry.changes,
        )
    }

    @Test
    fun `GIVEN Ana added three photos within ten minutes WHEN building the history THEN they read as one line`() {
        // Given
        val events = listOf(
            attachment(
                id = "a1",
                occurredAt = now - HOUR,
            ),
            attachment(
                id = "a2",
                occurredAt = now - HOUR - 5 * MINUTE,
            ),
            attachment(
                id = "a3",
                occurredAt = now - HOUR - 9 * MINUTE,
            ),
        )

        // When
        val entry = entriesOf(events).single()

        // Then
        assertEquals(BillHistoryKind.ATTACHMENTS_ADDED, entry.kind)
        assertEquals(3, entry.attachmentCount)
        assertEquals("a1", entry.id)
        assertEquals(now - HOUR, entry.occurredAt)
        assertFalse(entry.isReceipt)
    }

    @Test
    fun `GIVEN two photos eleven minutes apart WHEN building the history THEN each has its own line`() {
        // Given
        val events = listOf(
            attachment(
                id = "a1",
                occurredAt = now - HOUR,
            ),
            attachment(
                id = "a2",
                occurredAt = now - HOUR - 11 * MINUTE,
            ),
        )

        // When
        val entries = entriesOf(events)

        // Then
        assertEquals(listOf("a1", "a2"), entries.map { it.id })
        assertEquals(listOf(1, 1), entries.map { it.attachmentCount })
    }

    @Test
    fun `GIVEN photos by Ana and Bruno a minute apart WHEN building the history THEN each has their own line`() {
        // Given
        val events = listOf(
            attachment(
                id = "a1",
                occurredAt = now - HOUR,
            ),
            attachment(
                id = "a2",
                occurredAt = now - HOUR - MINUTE,
                actorMemberId = BRUNO,
                actorName = "Bruno",
            ),
        )

        // When
        val entries = entriesOf(events)

        // Then
        assertEquals(listOf("a1", "a2"), entries.map { it.id })
        assertEquals(listOf("Ana", "Bruno"), entries.map { it.actorName })
    }

    @Test
    fun `GIVEN a photo and the receipt a minute apart WHEN building the history THEN the receipt has its own line`() {
        // Given
        val events = listOf(
            attachment(
                id = "photo",
                occurredAt = now - HOUR,
            ),
            attachment(
                id = "receipt",
                occurredAt = now - HOUR - MINUTE,
                code = "receipt",
            ),
        )

        // When
        val entries = entriesOf(events)

        // Then
        assertEquals(listOf("photo", "receipt"), entries.map { it.id })
        assertEquals(listOf(false, true), entries.map { it.isReceipt })
    }

    @Test
    fun `GIVEN an edit between two photos WHEN building the history THEN the photos are not gathered`() {
        // Given
        val events = listOf(
            attachment(
                id = "a1",
                occurredAt = now - HOUR,
            ),
            event(
                id = "edit",
                kind = ActivityKind.BILL_EDITED,
                occurredAt = now - HOUR - MINUTE,
            ),
            attachment(
                id = "a2",
                occurredAt = now - HOUR - 2 * MINUTE,
            ),
        )

        // When
        val entries = entriesOf(events)

        // Then
        assertEquals(listOf("a1", "edit", "a2"), entries.map { it.id })
        val photos = entries.filter { it.kind == BillHistoryKind.ATTACHMENTS_ADDED }
        assertEquals(listOf(1, 1), photos.map { it.attachmentCount })
    }

    @Test
    fun `GIVEN a photo removed WHEN building the history THEN says a photo was removed`() {
        // Given
        val events = listOf(
            event(
                kind = ActivityKind.ATTACHMENT_REMOVED,
                code = "photo",
            ),
        )

        // When
        val entry = entriesOf(events).single()

        // Then
        assertEquals(BillHistoryKind.ATTACHMENT_REMOVED, entry.kind)
        assertFalse(entry.isReceipt)
    }

    @Test
    fun `GIVEN the link opened to anyone WHEN building the history THEN says who it opened to`() {
        // Given
        val events = listOf(
            event(
                kind = ActivityKind.BILL_LINK_CHANGED,
                code = BillLinkVisibility.ANYONE.code,
            ),
        )

        // When
        val entry = entriesOf(events).single()

        // Then
        assertEquals(BillHistoryKind.LINK_CHANGED, entry.kind)
        assertEquals(BillLinkVisibility.ANYONE, entry.linkVisibility)
    }

    @Test
    fun `GIVEN the link turned off WHEN building the history THEN the line has no visibility`() {
        // Given
        val events = listOf(
            event(
                kind = ActivityKind.BILL_LINK_CHANGED,
                code = null,
            ),
        )

        // When
        val entry = entriesOf(events).single()

        // Then
        assertEquals(BillHistoryKind.LINK_CHANGED, entry.kind)
        assertNull(entry.linkVisibility)
    }

    @Test
    fun `GIVEN a deletion a payment and a new member about the bill WHEN building the history THEN leaves them out`() {
        // Given
        val events = listOf(
            event(
                id = "deleted",
                kind = ActivityKind.BILL_DELETED,
            ),
            event(
                id = "payment",
                kind = ActivityKind.PAYMENT_RECORDED,
            ),
            event(
                id = "member",
                kind = ActivityKind.MEMBER_ADDED,
            ),
        )

        // When
        val days = historyOf(events, expense(), group, now, TimeZone.UTC)

        // Then
        assertTrue(days.isEmpty())
    }

    @Test
    fun `GIVEN a bill created offline and never up WHEN building the history THEN the device user created it`() {
        // Given
        val pending = expense(
            isPending = true,
            updatedAt = now - 30 * MINUTE,
        )

        // When
        val entry = historyOf(emptyList(), pending, group, now, TimeZone.UTC).single().entries.single()

        // Then
        assertEquals(BillHistoryKind.CREATED, entry.kind)
        assertTrue(entry.isPending)
        assertTrue(entry.actorIsMe)
        assertEquals("Pierre", entry.actorName)
        assertEquals(now - 30 * MINUTE, entry.occurredAt)
        assertTrue(entry.changes.isEmpty())
        assertFalse(entry.canUndo)
    }

    @Test
    fun `GIVEN a bill edited offline after it went up WHEN building the history THEN an edit not up yet is on top`() {
        // Given
        val pending = expense(
            isPending = true,
            updatedAt = now - 30 * MINUTE,
        )
        val events = listOf(
            event(
                id = "created",
                kind = ActivityKind.BILL_CREATED,
                occurredAt = now - 2 * HOUR,
            ),
        )

        // When
        val entries = historyOf(events, pending, group, now, TimeZone.UTC).single().entries

        // Then
        assertEquals(2, entries.size)
        val top = entries.first()
        assertEquals(BillHistoryKind.EDITED, top.kind)
        assertTrue(top.isPending)
        assertTrue(top.actorIsMe)
        assertTrue(top.changes.isEmpty())
        assertEquals("created", entries[1].id)
    }

    @Test
    fun `GIVEN the latest edit and the bill as it left it WHEN building the history THEN only it can be undone`() {
        // Given
        val events = listOf(
            event(
                id = "edit",
                changes = listOf(change(ActivityField.DESCRIPTION, text("Feira"), text("Mercado"))),
            ),
            event(
                id = "created",
                kind = ActivityKind.BILL_CREATED,
                occurredAt = now - 2 * HOUR,
            ),
        )

        // When
        val entries = entriesOf(events)

        // Then
        assertEquals(listOf(true, false), entries.map { it.canUndo })
    }

    @Test
    fun `GIVEN two edits in a row WHEN building the history THEN only the latest can be undone`() {
        // Given
        val events = listOf(
            event(
                id = "edit-2",
                changes = listOf(change(ActivityField.DESCRIPTION, text("Feira"), text("Mercado"))),
            ),
            event(
                id = "edit-1",
                occurredAt = now - 2 * HOUR,
                changes = listOf(change(ActivityField.AMOUNT, minor(30_000), minor(48_000))),
            ),
        )

        // When
        val entries = entriesOf(events)

        // Then
        assertEquals(listOf(true, false), entries.map { it.canUndo })
    }

    @Test
    fun `GIVEN a photo added after the latest edit WHEN building the history THEN nothing can be undone`() {
        // Given
        val events = listOf(
            attachment(
                id = "photo",
                occurredAt = now - HOUR,
            ),
            event(
                id = "edit",
                occurredAt = now - 2 * HOUR,
                changes = listOf(change(ActivityField.DESCRIPTION, text("Feira"), text("Mercado"))),
            ),
        )

        // When
        val entries = entriesOf(events)

        // Then
        assertTrue(entries.none { it.canUndo })
    }

    @Test
    fun `GIVEN the bill changed on the device after the latest edit WHEN building THEN it cannot be undone`() {
        // Given
        val events = listOf(event(changes = listOf(change(ActivityField.DESCRIPTION, text("Feira"), text("Mercado")))))
        val pending = expense(isPending = true)

        // When
        val entries = historyOf(events, pending, group, now, TimeZone.UTC).single().entries

        // Then
        assertTrue(entries.none { it.canUndo })
    }

    @Test
    fun `GIVEN the bill no longer as the latest edit left it WHEN building the history THEN it cannot be undone`() {
        // Given
        val events = listOf(event(changes = listOf(change(ActivityField.DESCRIPTION, text("Feira"), text("Mercado")))))
        val movedOn = expense(description = "Mercado do Zé")

        // When
        val entry = historyOf(events, movedOn, group, now, TimeZone.UTC).single().entries.single()

        // Then
        assertFalse(entry.canUndo)
    }

    @Test
    fun `GIVEN a bill split by its items WHEN building the history THEN its latest edit cannot be undone`() {
        // Given
        val events = listOf(event(changes = listOf(change(ActivityField.DESCRIPTION, text("Feira"), text("Mercado")))))
        val withItems = expense(items = noItems)

        // When
        val entry = historyOf(events, withItems, group, now, TimeZone.UTC).single().entries.single()

        // Then
        assertFalse(entry.canUndo)
    }

    @Test
    fun `GIVEN an edit that also changed a field Desfazer does not put back WHEN building the history THEN no undo`() {
        // Given
        val events = listOf(
            event(
                changes = listOf(
                    change(ActivityField.DESCRIPTION, text("Feira"), text("Mercado")),
                    change(ActivityField.CURRENCY, code("USD"), code("BRL")),
                ),
            ),
        )

        // When
        val entry = entriesOf(events).single()

        // Then
        assertFalse(entry.canUndo)
    }

    @Test
    fun `GIVEN an edit with no changes WHEN building the history THEN it cannot be undone`() {
        // Given
        val events = listOf(event(changes = emptyList()))

        // When
        val entry = entriesOf(events).single()

        // Then
        assertFalse(entry.canUndo)
    }

    @Test
    fun `GIVEN payers listed in another order than the bill WHEN building the history THEN the edit can be undone`() {
        // Given
        val events = listOf(
            event(
                changes = listOf(
                    change(ActivityField.PAYERS, payers(ANA to 48_000), payers(BRUNO to 20_000, ANA to 28_000)),
                ),
            ),
        )
        val bill = expense(payers = listOf(ExpensePayer(ANA, 28_000), ExpensePayer(BRUNO, 20_000)))

        // When
        val entry = historyOf(events, bill, group, now, TimeZone.UTC).single().entries.single()

        // Then
        assertTrue(entry.canUndo)
    }

    @Test
    fun `GIVEN payers paying other amounts than the edit left WHEN building the history THEN it cannot be undone`() {
        // Given
        val events = listOf(
            event(
                changes = listOf(
                    change(ActivityField.PAYERS, payers(ANA to 48_000), payers(ANA to 28_000, BRUNO to 20_000)),
                ),
            ),
        )
        val bill = expense(payers = listOf(ExpensePayer(ANA, 18_000), ExpensePayer(BRUNO, 30_000)))

        // When
        val entry = historyOf(events, bill, group, now, TimeZone.UTC).single().entries.single()

        // Then
        assertFalse(entry.canUndo)
    }

    @Test
    fun `GIVEN a split in Partes listed in another order than the bill WHEN building THEN it can be undone`() {
        // Given
        val events = listOf(
            event(
                changes = listOf(
                    change(
                        ActivityField.SPLIT,
                        split(SplitMode.EQUAL, ANA to 1, PIERRE to 1, BRUNO to 1),
                        split(SplitMode.SHARES, BRUNO to 1, PIERRE to 1, ANA to 2),
                    ),
                ),
            ),
        )

        // When
        val entry = historyOf(events, sharesBill, group, now, TimeZone.UTC).single().entries.single()

        // Then
        assertTrue(entry.canUndo)
    }

    @Test
    fun `GIVEN the split weights changed since the latest edit WHEN building the history THEN it cannot be undone`() {
        // Given
        val events = listOf(
            event(
                changes = listOf(
                    change(
                        ActivityField.SPLIT,
                        split(SplitMode.EQUAL, ANA to 1, PIERRE to 1, BRUNO to 1),
                        split(SplitMode.SHARES, ANA to 3, PIERRE to 1, BRUNO to 1),
                    ),
                ),
            ),
        )

        // When
        val entry = historyOf(events, sharesBill, group, now, TimeZone.UTC).single().entries.single()

        // Then
        assertFalse(entry.canUndo)
    }

    @Test
    fun `GIVEN the split mode changed since the latest edit WHEN building the history THEN it cannot be undone`() {
        // Given
        val events = listOf(
            event(
                changes = listOf(
                    change(
                        ActivityField.SPLIT,
                        split(SplitMode.AMOUNT, ANA to 16_000, PIERRE to 16_000, BRUNO to 16_000),
                        split(SplitMode.SHARES, ANA to 1, PIERRE to 1, BRUNO to 1),
                    ),
                ),
            ),
        )

        // When
        val entry = entriesOf(events).single()

        // Then
        assertFalse(entry.canUndo)
    }

    @Test
    fun `GIVEN an edit of the amount payers description category and date WHEN undoing THEN puts back each before`() {
        // Given
        val event = event(
            changes = listOf(
                change(ActivityField.AMOUNT, minor(30_000), minor(48_000)),
                change(ActivityField.PAYERS, payers(ANA to 30_000), payers(ANA to 48_000)),
                change(ActivityField.DESCRIPTION, text("Feira"), text("Mercado")),
                change(ActivityField.CATEGORY, code(null), code(AppCategory.FOOD_MARKET.key)),
                change(ActivityField.SPENT_AT, millis(lastWeek), millis(spentAt)),
            ),
        )
        val bill = expense(categoryId = AppCategory.FOOD_MARKET.key)

        // When
        val undo = undoOf(event, bill, group)

        // Then
        assertEquals(
            BillUndo(
                description = "Feira",
                amountMinor = 30_000,
                payers = listOf(ExpensePayer(ANA, 30_000)),
                splitMode = SplitMode.EQUAL,
                shares = listOf(share(ANA, 10_000), share(PIERRE, 10_000), share(BRUNO, 10_000)),
                categoryId = null,
                spentAt = lastWeek,
            ),
            undo,
        )
    }

    @Test
    fun `GIVEN an edit of the description only WHEN undoing THEN keeps the rest of the bill as it is now`() {
        // Given
        val event = event(changes = listOf(change(ActivityField.DESCRIPTION, text("Feira"), text("Mercado"))))
        val bill = expense(categoryId = AppCategory.FOOD_MARKET.key)

        // When
        val undo = undoOf(event, bill, group)

        // Then
        assertEquals(
            BillUndo(
                description = "Feira",
                amountMinor = 48_000,
                payers = listOf(ExpensePayer(ANA, 48_000)),
                splitMode = SplitMode.EQUAL,
                shares = listOf(share(ANA, 16_000), share(PIERRE, 16_000), share(BRUNO, 16_000)),
                categoryId = AppCategory.FOOD_MARKET.key,
                spentAt = spentAt,
            ),
            undo,
        )
    }

    @Test
    fun `GIVEN an edit of the total payer and split WHEN undoing THEN the old payer gets the cents of the old total`() {
        // Given
        val event = event(
            changes = listOf(
                change(ActivityField.AMOUNT, minor(10_000), minor(48_000)),
                change(ActivityField.PAYERS, payers(BRUNO to 10_000), payers(ANA to 48_000)),
                change(
                    ActivityField.SPLIT,
                    split(SplitMode.EQUAL, ANA to 1, PIERRE to 1, BRUNO to 1),
                    split(SplitMode.SHARES, ANA to 2, PIERRE to 1, BRUNO to 1),
                ),
            ),
        )

        // When
        val undo = undoOf(event, sharesBill, group)

        // Then
        assertEquals(10_000, undo?.amountMinor)
        assertEquals(listOf(ExpensePayer(BRUNO, 10_000)), undo?.payers)
        assertEquals(SplitMode.EQUAL, undo?.splitMode)
        assertEquals(listOf(share(ANA, 3_333), share(PIERRE, 3_333), share(BRUNO, 3_334)), undo?.shares)
    }

    @Test
    fun `GIVEN an edit whose old payer left the group WHEN undoing THEN there is nothing to undo`() {
        // Given
        val event =
            event(changes = listOf(change(ActivityField.PAYERS, payers(FORMER to 48_000), payers(ANA to 48_000))))

        // When
        val undo = undoOf(event, expense(), group)

        // Then
        assertNull(undo)
    }

    @Test
    fun `GIVEN an edit that left a split in Valores WHEN undoing THEN there is nothing to undo`() {
        // Given
        val event = event(
            changes = listOf(
                change(
                    ActivityField.SPLIT,
                    split(SplitMode.AMOUNT, ANA to 20_000, PIERRE to 14_000, BRUNO to 14_000),
                    split(SplitMode.EQUAL, ANA to 1, PIERRE to 1, BRUNO to 1),
                ),
            ),
        )

        // When
        val undo = undoOf(event, expense(), group)

        // Then
        assertNull(undo)
    }

    @Test
    fun `GIVEN the bill was created WHEN undoing THEN there is nothing to undo`() {
        // Given
        val event = event(kind = ActivityKind.BILL_CREATED)

        // When
        val undo = undoOf(event, expense(), group)

        // Then
        assertNull(undo)
    }

    @Test
    fun `GIVEN old payers that do not add up to the old total WHEN undoing THEN there is nothing to undo`() {
        // Given
        val event = event(changes = listOf(change(ActivityField.AMOUNT, minor(30_000), minor(48_000))))

        // When
        val undo = undoOf(event, expense(), group)

        // Then
        assertNull(undo)
    }

    private fun millisOf(iso: String): Long = Instant.parse(iso).toEpochMilliseconds()

    private fun entriesOf(events: List<ActivityEvent>): List<BillHistoryEntry> =
        historyOf(events, expense(), group, now, TimeZone.UTC).flatMap { it.entries }

    private val now = millisOf("2026-10-30T15:00:00Z")
    private val spentAt = millisOf("2026-10-30T12:00:00Z")
    private val yesterdayEvening = millisOf("2026-10-29T20:00:00Z")
    private val lastWeek = millisOf("2026-10-20T12:00:00Z")

    private val noItems = BillItems(
        items = emptyList(),
        servicePercent = 0,
        couvertMinor = 0,
        couvertMemberIds = emptyList(),
        extraMinor = 0,
        isFromReceipt = false,
    )

    // Why: Ana paid R$ 480 split in Partes, 2 for Ana and 1 for Pierre and Bruno.
    private val sharesBill = Expense(
        id = EXPENSE_ID,
        groupId = GROUP_ID,
        description = "Mercado",
        amountMinor = 48_000,
        splitMode = SplitMode.SHARES,
        payers = listOf(ExpensePayer(ANA, 48_000)),
        shares = listOf(
            ExpenseShare(ANA, 24_000, weight = 2),
            ExpenseShare(PIERRE, 12_000, weight = 1),
            ExpenseShare(BRUNO, 12_000, weight = 1),
        ),
        items = null,
        categoryId = null,
        spentAt = spentAt,
        isPending = false,
        updatedAt = spentAt,
        updatedByMemberId = null,
    )

    private val group = Group(
        id = GROUP_ID,
        name = "Viagem de Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.LILAC,
        currency = GroupCurrency.BRL,
        members = listOf(
            member(ANA, "Ana", AvatarColor.PEACH),
            member(PIERRE, "Pierre", AvatarColor.BLUE, isMe = true),
            member(BRUNO, "Bruno", AvatarColor.GREEN),
            member(CARLA, "Carla", AvatarColor.LILAC),
        ),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
    )

    private fun member(
        id: String,
        name: String,
        color: AvatarColor,
        isMe: Boolean = false,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = color,
        isMe = isMe,
        hasJoined = true,
        userId = null,
        inviteCode = null,
        photoUrl = null,
    )

    private fun share(
        memberId: String,
        amountMinor: Long,
    ): ExpenseShare = ExpenseShare(
        memberId = memberId,
        amountMinor = amountMinor,
        weight = 1,
    )

    // Why: by default Ana paid R$ 480 split equally among Ana, Pierre (this device's user) and Bruno; Carla is out.
    private fun expense(
        description: String = "Mercado",
        payers: List<ExpensePayer> = listOf(ExpensePayer(ANA, 48_000)),
        categoryId: String? = null,
        items: BillItems? = null,
        isPending: Boolean = false,
        updatedAt: Long = spentAt,
    ): Expense = Expense(
        id = EXPENSE_ID,
        groupId = GROUP_ID,
        description = description,
        amountMinor = 48_000,
        splitMode = SplitMode.EQUAL,
        payers = payers,
        shares = listOf(share(ANA, 16_000), share(PIERRE, 16_000), share(BRUNO, 16_000)),
        items = items,
        categoryId = categoryId,
        spentAt = spentAt,
        isPending = isPending,
        updatedAt = updatedAt,
        updatedByMemberId = null,
    )

    private fun event(
        id: String = "event-1",
        kind: ActivityKind = ActivityKind.BILL_EDITED,
        actorMemberId: String? = ANA,
        actorName: String? = "Ana",
        occurredAt: Long = now - HOUR,
        amountMinor: Long? = 48_000,
        code: String? = null,
        changes: List<ActivityChange> = emptyList(),
        sharesCount: Int? = null,
    ): ActivityEvent = ActivityEvent(
        id = id,
        seq = 1L,
        groupId = GROUP_ID,
        kind = kind,
        actorUserId = null,
        actorMemberId = actorMemberId,
        actorName = actorName,
        groupName = "Viagem de Floripa",
        targetId = EXPENSE_ID,
        occurredAt = occurredAt,
        subject = "Mercado",
        amountMinor = amountMinor,
        code = code,
        fromMemberId = null,
        toMemberId = null,
        changes = changes,
        sharesCount = sharesCount,
        body = null,
        isPending = false,
    )

    private fun attachment(
        id: String,
        occurredAt: Long,
        actorMemberId: String = ANA,
        actorName: String = "Ana",
        code: String = "photo",
    ): ActivityEvent = event(
        id = id,
        kind = ActivityKind.ATTACHMENT_ADDED,
        actorMemberId = actorMemberId,
        actorName = actorName,
        occurredAt = occurredAt,
        code = code,
    )

    private fun change(
        field: ActivityField,
        before: ActivityValue,
        after: ActivityValue,
    ): ActivityChange = ActivityChange(field, before, after)

    private fun minor(value: Long): ActivityValue = ActivityValue.none.copy(minor = value)

    private fun text(value: String): ActivityValue = ActivityValue.none.copy(text = value)

    private fun millis(value: Long): ActivityValue = ActivityValue.none.copy(millis = value)

    private fun code(value: String?): ActivityValue = ActivityValue.none.copy(code = value)

    private fun payers(vararg paid: Pair<String, Long>): ActivityValue = ActivityValue.none.copy(
        memberIds = paid.map { it.first },
        memberValues = paid.map { ActivityMemberValue(it.first, it.second) },
    )

    private fun split(
        mode: SplitMode,
        vararg weights: Pair<String, Long>,
    ): ActivityValue = ActivityValue.none.copy(
        code = mode.code,
        memberIds = weights.filter { it.second > 0 }.map { it.first },
        memberValues = weights.map { ActivityMemberValue(it.first, it.second) },
    )

    private companion object {
        const val GROUP_ID = "group-1"
        const val EXPENSE_ID = "expense-1"
        const val ANA = "member-1"
        const val PIERRE = "member-2"
        const val BRUNO = "member-3"
        const val CARLA = "member-4"
        const val FORMER = "member-9"
        const val MINUTE = 60_000L
        const val HOUR = 60 * MINUTE
    }
}
