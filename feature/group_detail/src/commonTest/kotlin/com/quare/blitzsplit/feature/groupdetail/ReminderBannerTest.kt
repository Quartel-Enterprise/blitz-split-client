package com.quare.blitzsplit.feature.groupdetail

import com.quare.blitzsplit.core.expenses.domain.model.CurrencyBalances
import com.quare.blitzsplit.core.expenses.domain.model.Debt
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.GroupReminder
import com.quare.blitzsplit.core.groups.domain.model.ReminderKind
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ReminderBanner
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

// Why: who sees "Time to settle up" on the group, and for which occurrence of the group's reminder.
class ReminderBannerTest {
    // Why: a Friday; the weekly Monday 10:00 reminder last fired on the 26th, the monthly 25th at 12:00 on the 25th.
    private val now = millisOf("2026-10-30T15:00:00Z")
    private val mondayTenOclock = millisOf("2026-10-26T10:00:00Z")
    private val twentyFifthNoon = millisOf("2026-10-25T12:00:00Z")

    private val weekly = GroupReminder(
        isEnabled = true,
        kind = ReminderKind.WEEKLY,
        day = 1,
        minute = 600,
    )
    private val monthly = GroupReminder(
        isEnabled = true,
        kind = ReminderKind.MONTHLY,
        day = 25,
        minute = 720,
    )

    private val iOweBruno = listOf(CurrencyBalances(GroupCurrency.USD, balancesOf(Debt(ME, BRUNO, 4_000))))
    private val brunoOwesMe = listOf(CurrencyBalances(GroupCurrency.USD, balancesOf(Debt(BRUNO, ME, 4_000))))
    private val everyoneSettled = listOf(CurrencyBalances(GroupCurrency.USD, balancesOf()))

    @Test
    fun `GIVEN a weekly reminder and a debtor WHEN this week occurrence has passed THEN shows the banner for it`() {
        // Given
        val group = group(reminder = weekly)

        // When
        val banner = ReminderBanner.of(group, iOweBruno, ME, now, TimeZone.UTC)

        // Then
        assertEquals(
            ReminderBanner(
                occurrenceAt = mondayTenOclock,
                reminder = weekly,
            ),
            banner,
        )
    }

    @Test
    fun `GIVEN a monthly reminder and a debtor WHEN this month occurrence has passed THEN shows the banner for it`() {
        // Given
        val group = group(reminder = monthly)

        // When
        val banner = ReminderBanner.of(group, iOweBruno, ME, now, TimeZone.UTC)

        // Then
        assertEquals(
            ReminderBanner(
                occurrenceAt = twentyFifthNoon,
                reminder = monthly,
            ),
            banner,
        )
    }

    @Test
    fun `GIVEN a monthly reminder on the 25th WHEN it is the 10th THEN shows the banner for the one of last month`() {
        // Given
        val group = group(reminder = monthly)
        val tenthOfNovember = millisOf("2026-11-10T09:00:00Z")

        // When
        val banner = ReminderBanner.of(group, iOweBruno, ME, tenthOfNovember, TimeZone.UTC)

        // Then
        assertEquals(twentyFifthNoon, banner?.occurrenceAt)
    }

    @Test
    fun `GIVEN a debtor WHEN the occurrence is exactly now THEN shows the banner`() {
        // Given
        val group = group(reminder = weekly)

        // When
        val banner = ReminderBanner.of(group, iOweBruno, ME, mondayTenOclock, TimeZone.UTC)

        // Then
        assertEquals(mondayTenOclock, banner?.occurrenceAt)
    }

    @Test
    fun `GIVEN a debtor WHEN the occurrence is a minute away THEN shows the one before`() {
        // Given
        val group = group(reminder = weekly)
        val aMinuteBefore = mondayTenOclock - 60_000L
        val previousMonday = millisOf("2026-10-19T10:00:00Z")

        // When
        val banner = ReminderBanner.of(group, iOweBruno, ME, aMinuteBefore, TimeZone.UTC)

        // Then
        assertEquals(previousMonday, banner?.occurrenceAt)
    }

    @Test
    fun `GIVEN someone owed money WHEN the occurrence has passed THEN shows no banner`() {
        // Given
        val group = group(reminder = weekly)

        // When
        val banner = ReminderBanner.of(group, brunoOwesMe, ME, now, TimeZone.UTC)

        // Then
        assertNull(banner)
    }

    @Test
    fun `GIVEN everyone settled WHEN the occurrence has passed THEN shows no banner`() {
        // Given
        val group = group(reminder = weekly)

        // When
        val banner = ReminderBanner.of(group, everyoneSettled, ME, now, TimeZone.UTC)

        // Then
        assertNull(banner)
    }

    @Test
    fun `GIVEN a group without bills in any currency WHEN the occurrence has passed THEN shows no banner`() {
        // Given
        val group = group(reminder = weekly)

        // When
        val banner = ReminderBanner.of(
            group,
            byCurrency = emptyList(),
            meId = ME,
            nowMillis = now,
            timeZone = TimeZone.UTC,
        )

        // Then
        assertNull(banner)
    }

    @Test
    fun `GIVEN two others owing each other and nothing from me WHEN the occurrence has passed THEN shows no banner`() {
        // Given
        val group = group(reminder = weekly)
        val othersOnly = listOf(CurrencyBalances(GroupCurrency.USD, balancesOf(Debt(DIEGO, BRUNO, 4_000))))

        // When
        val banner = ReminderBanner.of(group, othersOnly, ME, now, TimeZone.UTC)

        // Then
        assertNull(banner)
    }

    @Test
    fun `GIVEN a debt only in the second currency WHEN the occurrence has passed THEN shows the banner`() {
        // Given
        val group = group(reminder = weekly)
        val settledInDollarsOweInReais = listOf(
            CurrencyBalances(GroupCurrency.USD, balancesOf()),
            CurrencyBalances(GroupCurrency.BRL, balancesOf(Debt(ME, BRUNO, 4_000))),
        )

        // When
        val banner = ReminderBanner.of(group, settledInDollarsOweInReais, ME, now, TimeZone.UTC)

        // Then
        assertEquals(mondayTenOclock, banner?.occurrenceAt)
    }

    @Test
    fun `GIVEN owed in one currency and owing in another WHEN the occurrence has passed THEN shows the banner`() {
        // Given
        val group = group(reminder = weekly)
        val owedInDollarsOweInReais = listOf(
            CurrencyBalances(GroupCurrency.USD, balancesOf(Debt(BRUNO, ME, 4_000))),
            CurrencyBalances(GroupCurrency.BRL, balancesOf(Debt(ME, BRUNO, 4_000))),
        )

        // When
        val banner = ReminderBanner.of(group, owedInDollarsOweInReais, ME, now, TimeZone.UTC)

        // Then
        assertEquals(mondayTenOclock, banner?.occurrenceAt)
    }

    @Test
    fun `GIVEN no account row for me WHEN the occurrence has passed THEN shows no banner`() {
        // Given
        val group = group(reminder = weekly)

        // When
        val banner = ReminderBanner.of(
            group,
            iOweBruno,
            meId = null,
            nowMillis = now,
            timeZone = TimeZone.UTC,
        )

        // Then
        assertNull(banner)
    }

    @Test
    fun `GIVEN the reminder off WHEN a debtor opens the group THEN shows no banner`() {
        // Given
        val group = group(reminder = weekly.copy(isEnabled = false))

        // When
        val banner = ReminderBanner.of(group, iOweBruno, ME, now, TimeZone.UTC)

        // Then
        assertNull(banner)
    }

    @Test
    fun `GIVEN the occurrence dismissed WHEN a debtor opens the group THEN shows no banner`() {
        // Given
        val group = group(
            reminder = weekly,
            reminderDismissedAt = mondayTenOclock,
        )

        // When
        val banner = ReminderBanner.of(group, iOweBruno, ME, now, TimeZone.UTC)

        // Then
        assertNull(banner)
    }

    @Test
    fun `GIVEN a dismissal of a later time than the occurrence WHEN a debtor opens the group THEN shows no banner`() {
        // Given
        val group = group(
            reminder = weekly,
            reminderDismissedAt = mondayTenOclock + 1L,
        )

        // When
        val banner = ReminderBanner.of(group, iOweBruno, ME, now, TimeZone.UTC)

        // Then
        assertNull(banner)
    }

    @Test
    fun `GIVEN a dismissal a millisecond before the occurrence WHEN a debtor opens the group THEN shows the banner`() {
        // Given
        val group = group(
            reminder = weekly,
            reminderDismissedAt = mondayTenOclock - 1L,
        )

        // When
        val banner = ReminderBanner.of(group, iOweBruno, ME, now, TimeZone.UTC)

        // Then
        assertEquals(mondayTenOclock, banner?.occurrenceAt)
    }

    @Test
    fun `GIVEN last week dismissed WHEN the next occurrence arrives THEN shows the banner for it`() {
        // Given
        val group = group(
            reminder = weekly,
            reminderDismissedAt = mondayTenOclock,
        )
        val nextMonday = millisOf("2026-11-02T10:00:00Z")

        // When
        val banner = ReminderBanner.of(group, iOweBruno, ME, nextMonday, TimeZone.UTC)

        // Then
        assertEquals(nextMonday, banner?.occurrenceAt)
    }

    @Test
    fun `GIVEN last month dismissed WHEN the next occurrence arrives THEN shows the banner for it`() {
        // Given
        val group = group(
            reminder = monthly,
            reminderDismissedAt = twentyFifthNoon,
        )
        val nextOccurrence = millisOf("2026-11-25T12:00:00Z")

        // When
        val banner = ReminderBanner.of(group, iOweBruno, ME, nextOccurrence, TimeZone.UTC)

        // Then
        assertEquals(nextOccurrence, banner?.occurrenceAt)
    }

    @Test
    fun `GIVEN a clock behind UTC WHEN it is Monday 9 in the morning there THEN shows the occurrence of last Monday`() {
        // Given
        val group = group(reminder = weekly)
        val behindUtc = UtcOffset(hours = -3).asTimeZone()
        val mondayNineOnTheirClock = millisOf("2026-10-26T12:00:00Z")
        val lastMondayTenOnTheirClock = millisOf("2026-10-19T13:00:00Z")

        // When
        val banner = ReminderBanner.of(group, iOweBruno, ME, mondayNineOnTheirClock, behindUtc)

        // Then
        assertEquals(lastMondayTenOnTheirClock, banner?.occurrenceAt)
    }

    @Test
    fun `GIVEN a clock behind UTC WHEN it is Monday 10 there THEN shows the occurrence of this Monday`() {
        // Given
        val group = group(reminder = weekly)
        val behindUtc = UtcOffset(hours = -3).asTimeZone()
        val mondayTenOnTheirClock = millisOf("2026-10-26T13:00:00Z")

        // When
        val banner = ReminderBanner.of(group, iOweBruno, ME, mondayTenOnTheirClock, behindUtc)

        // Then
        assertEquals(mondayTenOnTheirClock, banner?.occurrenceAt)
    }

    private fun millisOf(instant: String): Long = Instant.parse(instant).toEpochMilliseconds()

    private fun balancesOf(vararg debts: Debt): GroupBalances = GroupBalances(
        debts = debts.toList(),
        hasBills = true,
        unsimplifiedDebts = null,
    )

    private fun group(
        reminder: GroupReminder,
        reminderDismissedAt: Long = 0L,
    ): Group = Group(
        id = "group-1",
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.LILAC,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.USD,
        members = listOf(
            GroupMember(
                id = ME,
                name = "Pierre",
                avatarColor = AvatarColor.BLUE,
                isMe = true,
                hasJoined = true,
                userId = null,
                inviteCode = null,
                photoUrl = null,
            ),
        ),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
        simplifiesDebts = false,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = null,
        simplifyDebtsConflictBy = null,
        reminder = reminder,
        reminderDismissedAt = reminderDismissedAt,
    )

    private companion object {
        const val ME = "member-2"
        const val BRUNO = "member-3"
        const val DIEGO = "member-5"
    }
}
