package com.quare.blitzsplit.feature.groups

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.GroupFace
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.expenses.domain.model.PersonBalance
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.feature.groups.presentation.mapper.ActivityClock
import com.quare.blitzsplit.feature.groups.presentation.mapper.GroupWithMoney
import com.quare.blitzsplit.feature.groups.presentation.mapper.homeBillDaysOf
import com.quare.blitzsplit.feature.groups.presentation.mapper.homeGroupsOf
import com.quare.blitzsplit.feature.groups.presentation.mapper.homePeopleOf
import com.quare.blitzsplit.feature.groups.presentation.model.ActivityAge
import com.quare.blitzsplit.feature.groups.presentation.model.GroupBalanceKind
import com.quare.blitzsplit.feature.groups.presentation.model.GroupRow
import com.quare.blitzsplit.feature.groups.presentation.model.HomeBill
import com.quare.blitzsplit.feature.groups.presentation.model.HomePerson
import com.quare.blitzsplit.feature.groups.presentation.model.LastActivity
import com.quare.blitzsplit.feature.groups.presentation.model.SearchBillPayer
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class HomeContentTest {
    @Test
    fun `GIVEN people you owe and who owe you WHEN listing the faces THEN whom you owe come first the largest first`() {
        // Given
        val balances = peopleBalances(
            person("name:ana", netMinor = 4500L),
            person("name:diego", netMinor = -1500L),
            person("name:bruno", netMinor = 500L),
            person("name:carla", netMinor = -300L),
        )

        // When
        val people = homePeopleOf(balances)

        // Then
        assertEquals(listOf("name:diego", "name:carla", "name:ana", "name:bruno"), people.map { it.key })
    }

    @Test
    fun `GIVEN two people owed the same amount WHEN listing the faces THEN keeps the order Saldos lists them in`() {
        // Given
        val balances = peopleBalances(
            person("name:bruno", netMinor = 3000L),
            person("name:ana", netMinor = 3000L),
            person("name:diego", netMinor = -700L),
            person("name:carla", netMinor = -700L),
        )

        // When
        val people = homePeopleOf(balances)

        // Then
        assertEquals(listOf("name:diego", "name:carla", "name:bruno", "name:ana"), people.map { it.key })
    }

    @Test
    fun `GIVEN a guest who owes you WHEN listing the faces THEN shows their name initials color and the guest mark`() {
        // Given
        val balances = peopleBalances(
            person(
                "name:ana paula",
                netMinor = 3200L,
                name = "Ana Paula",
                color = AvatarColor.PEACH,
                currency = GroupCurrency.USD,
                isGuest = true,
            ),
        )

        // When
        val people = homePeopleOf(balances)

        // Then
        assertEquals(
            listOf(
                HomePerson(
                    key = "name:ana paula",
                    name = "Ana Paula",
                    initials = "AP",
                    color = AvatarColor.PEACH,
                    currency = GroupCurrency.USD,
                    netMinor = 3200L,
                    isGuest = true,
                ),
            ),
            people,
        )
    }

    @Test
    fun `GIVEN someone no longer in the group WHEN listing the faces THEN shows no name initials nor color`() {
        // Given
        val balances = peopleBalances(
            person(
                "gone:trio:member-Ana",
                netMinor = -900L,
                name = null,
            ),
        )

        // When
        val person = homePeopleOf(balances).single()

        // Then
        assertNull(person.name)
        assertEquals("", person.initials)
        assertNull(person.color)
        assertFalse(person.isGuest)
    }

    @Test
    fun `GIVEN someone you owe in two currencies WHEN listing the faces THEN shows one face per currency`() {
        // Given
        val balances = peopleBalances(
            person(
                "name:ana",
                netMinor = -3000L,
                currency = GroupCurrency.BRL,
            ),
            person(
                "name:ana",
                netMinor = -200L,
                currency = GroupCurrency.USD,
            ),
        )

        // When
        val people = homePeopleOf(balances)

        // Then
        assertEquals(
            listOf("name:ana" to GroupCurrency.BRL, "name:ana" to GroupCurrency.USD),
            people.map { it.key to it.currency },
        )
    }

    @Test
    fun `GIVEN five open groups and no pin WHEN picking the cards THEN shows the first three in their order`() {
        // Given
        val rows = listOf(row("a"), row("b"), row("c"), row("d"), row("e"))

        // When
        val groups = homeGroupsOf(rows, pinnedGroupIds = emptySet())

        // Then
        assertEquals(listOf("a", "b", "c"), groups.map { it.row.id })
        assertTrue(groups.none { it.isPinned })
    }

    @Test
    fun `GIVEN pinned groups far down WHEN picking the cards THEN shows them first and then the latest`() {
        // Given
        val rows = listOf(row("a"), row("b"), row("c"), row("d"), row("e"))

        // When
        val groups = homeGroupsOf(rows, pinnedGroupIds = setOf("e", "c"))

        // Then
        assertEquals(listOf("c", "e", "a"), groups.map { it.row.id })
        assertEquals(listOf(true, true, false), groups.map { it.isPinned })
    }

    @Test
    fun `GIVEN a pinned group where everyone settled WHEN picking the cards THEN leaves it out`() {
        // Given
        val rows = listOf(
            row(
                "settled",
                balanceKind = GroupBalanceKind.EVERYONE_SETTLED,
            ),
            row("a"),
            row("b"),
            row("c"),
        )

        // When
        val groups = homeGroupsOf(rows, pinnedGroupIds = setOf("settled"))

        // Then
        assertEquals(listOf("a", "b", "c"), groups.map { it.row.id })
    }

    @Test
    fun `GIVEN open groups of every kind WHEN picking the cards THEN keeps those without bills or with debts`() {
        // Given
        val rows = listOf(
            row(
                "no-bills",
                balanceKind = GroupBalanceKind.NO_EXPENSES,
            ),
            row(
                "you-are-settled",
                balanceKind = GroupBalanceKind.YOU_ARE_SETTLED,
            ),
            row(
                "settled",
                balanceKind = GroupBalanceKind.EVERYONE_SETTLED,
            ),
        )

        // When
        val groups = homeGroupsOf(rows, pinnedGroupIds = setOf("missing"))

        // Then
        assertEquals(listOf("no-bills", "you-are-settled"), groups.map { it.row.id })
    }

    @Test
    fun `GIVEN seven bills of yours across groups WHEN listing the latest bills THEN keeps the five most recent`() {
        // Given
        val groups = listOf(
            withMoney(
                trio("home"),
                bill(
                    "h1",
                    groupId = "home",
                    spentAt = hoursAgo(1),
                ),
                bill(
                    "h2",
                    groupId = "home",
                    spentAt = hoursAgo(5),
                ),
                bill(
                    "h3",
                    groupId = "home",
                    spentAt = hoursAgo(30),
                ),
                bill(
                    "h4",
                    groupId = "home",
                    spentAt = hoursAgo(80),
                ),
            ),
            withMoney(
                trio("trip"),
                bill(
                    "t1",
                    groupId = "trip",
                    spentAt = hoursAgo(2),
                ),
                bill(
                    "t2",
                    groupId = "trip",
                    spentAt = hoursAgo(40),
                ),
                bill(
                    "t3",
                    groupId = "trip",
                    spentAt = hoursAgo(100),
                ),
            ),
        )

        // When
        val days = homeBillDaysOf(groups, clock)

        // Then
        assertEquals(listOf("h1", "t1", "h2", "h3", "t2"), days.flatMap { day -> day.bills.map { it.id } })
    }

    @Test
    fun `GIVEN newer bills only other members paid and split WHEN listing the latest bills THEN leaves them out`() {
        // Given
        val groups = listOf(
            withMoney(
                trio(),
                bill(
                    "others",
                    paidBy = "Ana",
                    splitOver = listOf("Ana", "Bruno"),
                    spentAt = hoursAgo(1),
                ),
                bill("mine", spentAt = hoursAgo(3)),
            ),
        )

        // When
        val days = homeBillDaysOf(groups, clock)

        // Then
        assertEquals(listOf("mine"), days.flatMap { day -> day.bills.map { it.id } })
    }

    @Test
    fun `GIVEN a bill you paid for the others only WHEN listing the latest bills THEN all of it is to receive`() {
        // Given
        val groups = listOf(
            withMoney(
                trio(),
                bill(
                    "gift",
                    paidBy = "Pierre",
                    amountMinor = 6000L,
                    splitOver = listOf("Ana", "Bruno"),
                    spentAt = hoursAgo(1),
                ),
            ),
        )

        // When
        val bill = homeBillDaysOf(groups, clock).single().bills.single()

        // Then
        assertEquals(6000L, bill.myBalanceMinor)
    }

    @Test
    fun `GIVEN a group this device user is not in WHEN listing the latest bills THEN leaves its bills out`() {
        // Given
        val groups = listOf(
            withMoney(
                group(
                    id = "theirs",
                    members = listOf(member("Ana"), member("Bruno")),
                ),
                bill(
                    "theirs-bill",
                    groupId = "theirs",
                    paidBy = "Ana",
                    splitOver = listOf("Ana", "Bruno"),
                    spentAt = hoursAgo(1),
                ),
            ),
        )

        // When
        val days = homeBillDaysOf(groups, clock)

        // Then
        assertTrue(days.isEmpty())
    }

    @Test
    fun `GIVEN bills of today yesterday and last week WHEN listing them THEN groups them by day the latest first`() {
        // Given
        val groups = listOf(
            withMoney(
                trio(),
                bill("today-late", spentAt = hoursAgo(1)),
                bill("today-early", spentAt = hoursAgo(10)),
                bill("yesterday", spentAt = hoursAgo(20)),
                bill("last-week", spentAt = now - 7.days.inWholeMilliseconds),
            ),
        )

        // When
        val days = homeBillDaysOf(groups, clock)

        // Then
        assertEquals(
            listOf(RelativeDay.TODAY, RelativeDay.YESTERDAY, RelativeDay.OTHER),
            days.map { it.relativeDay },
        )
        assertEquals(
            listOf(
                dayStart("2026-10-31"),
                dayStart("2026-10-30"),
                dayStart("2026-10-24"),
            ),
            days.map { it.dayStartMillis },
        )
        assertEquals(
            listOf(listOf("today-late", "today-early"), listOf("yesterday"), listOf("last-week")),
            days.map { day -> day.bills.map { it.id } },
        )
    }

    @Test
    fun `GIVEN a queued bill two people paid WHEN listing the latest bills THEN shows group payers and balance`() {
        // Given
        val groups = listOf(
            withMoney(
                trio(
                    id = "trip",
                    name = "Viagem Floripa",
                    currency = GroupCurrency.USD,
                ),
                Expense(
                    id = "dinner",
                    groupId = "trip",
                    description = "Jantar",
                    amountMinor = 9000L,
                    splitMode = SplitMode.EQUAL,
                    payers = listOf(ExpensePayer("member-Ana", 3000L), ExpensePayer("member-Pierre", 6000L)),
                    shares = everyone.map { ExpenseShare("member-$it", 3000L, 1L) },
                    spentAt = hoursAgo(1),
                    isPending = true,
                    updatedAt = hoursAgo(1),
                    updatedByMemberId = null,
                    items = null,
                    categoryId = null,
                ),
            ),
        )

        // When
        val bill = homeBillDaysOf(groups, clock).single().bills.single()

        // Then
        assertEquals(
            HomeBill(
                id = "dinner",
                groupId = "trip",
                groupName = "Viagem Floripa",
                description = "Jantar",
                totalMinor = 9000L,
                currency = GroupCurrency.USD,
                payers = listOf(
                    SearchBillPayer(
                        name = "Pierre",
                        isMe = true,
                    ),
                    SearchBillPayer(
                        name = "Ana",
                        isMe = false,
                    ),
                ),
                myBalanceMinor = 3000L,
                isPending = true,
            ),
            bill,
        )
    }

    @Test
    fun `GIVEN a bill someone else paid WHEN listing the latest bills THEN your balance is what you owe`() {
        // Given
        val groups = listOf(
            withMoney(
                trio(),
                bill(
                    "gas",
                    paidBy = "Ana",
                    amountMinor = 9000L,
                    spentAt = hoursAgo(1),
                ),
            ),
        )

        // When
        val bill = homeBillDaysOf(groups, clock).single().bills.single()

        // Then
        assertEquals(-3000L, bill.myBalanceMinor)
        assertFalse(bill.isPending)
    }

    private val everyone = listOf("Pierre", "Ana", "Bruno")

    // Why: Saturday, 31 October 2026, 15:00 in UTC, the time zone the tests count days in.
    private val now = Instant.parse("2026-10-31T15:00:00Z").toEpochMilliseconds()

    private val clock = ActivityClock(
        nowMillis = now,
        timeZone = TimeZone.UTC,
    )

    private fun hoursAgo(hours: Int): Long = now - hours.hours.inWholeMilliseconds

    private fun dayStart(date: String): Long = Instant.parse("${date}T00:00:00Z").toEpochMilliseconds()

    private fun peopleBalances(vararg people: PersonBalance): PeopleBalances = PeopleBalances(
        people = people.toList(),
        totals = emptyList(),
        me = null,
    )

    private fun person(
        key: String,
        netMinor: Long,
        name: String? = key.substringAfter(':'),
        color: AvatarColor = AvatarColor.BLUE,
        currency: GroupCurrency = GroupCurrency.BRL,
        isGuest: Boolean = false,
    ): PersonBalance = PersonBalance(
        key = key,
        name = name,
        face = name?.let { GroupFace(it.take(1).uppercase(), color) },
        currency = currency,
        netMinor = netMinor,
        groups = emptyList(),
        isGuest = isGuest,
        guestBills = emptyList(),
        guestPaidMinor = null,
    )

    private fun row(
        id: String,
        balanceKind: GroupBalanceKind = GroupBalanceKind.YOU_OWE,
    ): GroupRow = GroupRow(
        id = id,
        name = id,
        initials = "G",
        color = AvatarColor.BLUE,
        faces = emptyList(),
        extraMemberCount = 0,
        memberCount = 3,
        memberNames = emptyList(),
        isPending = false,
        pendingBillDescription = null,
        currency = GroupCurrency.BRL,
        balanceKind = balanceKind,
        netMinor = 0L,
        receivesMinor = 0L,
        owesMinor = 0L,
        lastActivity = LastActivity(0L, ActivityAge.TODAY, 0),
    )

    private fun withMoney(
        group: Group,
        vararg expenses: Expense,
    ): GroupWithMoney = GroupWithMoney(
        group = group,
        expenses = expenses.toList(),
        settlements = emptyList(),
        pendingDeletionCount = 0,
    )

    private fun group(
        id: String,
        name: String = "Grupo",
        members: List<GroupMember>,
        currency: GroupCurrency = GroupCurrency.BRL,
    ): Group = Group(
        id = id,
        name = name,
        type = GroupType.OTHER,
        color = AvatarColor.BLUE,
        currency = currency,
        members = members,
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
    )

    // Why: Pierre is this device's user, in a group with Ana and Bruno.
    private fun trio(
        id: String = "trio",
        name: String = "Apê 302",
        currency: GroupCurrency = GroupCurrency.BRL,
    ): Group = group(
        id = id,
        name = name,
        members = listOf(
            member(
                name = "Pierre",
                isMe = true,
            ),
            member("Ana"),
            member("Bruno"),
        ),
        currency = currency,
    )

    private fun member(
        name: String,
        isMe: Boolean = false,
    ): GroupMember = GroupMember(
        id = "member-$name",
        name = name,
        avatarColor = AvatarColor.BLUE,
        isMe = isMe,
        hasJoined = false,
        userId = null,
        inviteCode = null,
    )

    // Why: one payer for the whole bill, split over the members given.
    private fun bill(
        id: String,
        groupId: String = "trio",
        paidBy: String = "Pierre",
        amountMinor: Long = 9000L,
        splitOver: List<String> = everyone,
        spentAt: Long,
    ): Expense = Expense(
        id = id,
        groupId = groupId,
        description = "Mercado",
        amountMinor = amountMinor,
        splitMode = SplitMode.EQUAL,
        payers = listOf(ExpensePayer("member-$paidBy", amountMinor)),
        shares = splitOver.map { ExpenseShare("member-$it", amountMinor / splitOver.size, 1L) },
        spentAt = spentAt,
        isPending = false,
        updatedAt = spentAt,
        updatedByMemberId = null,
        items = null,
        categoryId = null,
    )
}
