package com.quare.blitzsplit.feature.groupstats

import com.quare.blitzsplit.core.expenses.domain.model.DateSpan
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.feature.groupstats.presentation.viewmodel.SettlingCalculator
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class SettlingCalculatorTest {
    private lateinit var calculator: SettlingCalculator

    @Test
    fun `GIVEN Ana pays in two days and Bruno in five WHEN ranking THEN Ana is the fastest and Bruno the slowest`() {
        // Given
        prepareScenario(
            bills = listOf(bill("dinner", "2026-10-10T12:00:00Z", PIERRE, PIERRE to 100, ANA to 100, BRUNO to 100)),
            payments = listOf(
                payment(ANA, PIERRE, 100, "2026-10-12T12:00:00Z"),
                payment(BRUNO, PIERRE, 100, "2026-10-15T12:00:00Z"),
            ),
        )

        // When
        val settling = assertNotNull(calculator.createOrNull(october))

        // Then
        assertEquals(ANA, settling.fastest.memberId)
        assertEquals(2, settling.fastest.averageDays)
        assertEquals(0L, settling.fastest.openMinor)
        assertEquals(BRUNO, settling.slowest.memberId)
        assertEquals(5, settling.slowest.averageDays)
        assertEquals(0L, settling.slowest.openMinor)
    }

    @Test
    fun `GIVEN Pierre only fronts the bills WHEN ranking THEN he is not ranked`() {
        // Given
        prepareScenario(
            bills = listOf(
                bill("dinner", "2026-10-10T12:00:00Z", PIERRE, PIERRE to 100, ANA to 100),
                bill("taxi", "2026-10-20T12:00:00Z", PIERRE, PIERRE to 100, BRUNO to 100),
            ),
        )

        // When
        val settling = assertNotNull(calculator.createOrNull(october))

        // Then
        assertEquals(setOf(ANA, BRUNO), setOf(settling.fastest.memberId, settling.slowest.memberId))
    }

    @Test
    fun `GIVEN three people with debts WHEN ranking THEN the one in the middle shows in neither`() {
        // Given
        prepareScenario(
            members = listOf(pierre, ana, bruno, carla),
            bills = listOf(
                bill(
                    "boat",
                    "2026-10-10T12:00:00Z",
                    PIERRE,
                    PIERRE to 100,
                    ANA to 100,
                    BRUNO to 100,
                    CARLA to 100,
                ),
            ),
            payments = listOf(
                payment(ANA, PIERRE, 100, "2026-10-11T12:00:00Z"),
                payment(BRUNO, PIERRE, 100, "2026-10-15T12:00:00Z"),
                payment(CARLA, PIERRE, 100, "2026-10-20T12:00:00Z"),
            ),
        )

        // When
        val settling = assertNotNull(calculator.createOrNull(october))

        // Then
        assertEquals(ANA, settling.fastest.memberId)
        assertEquals(1, settling.fastest.averageDays)
        assertEquals(CARLA, settling.slowest.memberId)
        assertEquals(10, settling.slowest.averageDays)
    }

    @Test
    fun `GIVEN a small debt paid fast and a large one slowly WHEN ranking THEN each weighs by its amount`() {
        // Given
        prepareScenario(
            bills = listOf(
                bill("coffee", "2026-10-01T12:00:00Z", PIERRE, PIERRE to 100, ANA to 100),
                bill("hotel", "2026-10-10T12:00:00Z", PIERRE, PIERRE to 300, ANA to 300),
                brunoOpenDebt,
            ),
            payments = listOf(
                payment(ANA, PIERRE, 100, "2026-10-02T12:00:00Z"),
                payment(ANA, PIERRE, 300, "2026-10-15T12:00:00Z"),
            ),
        )

        // When
        val settling = assertNotNull(calculator.createOrNull(october))

        // Then
        assertEquals(ANA, settling.fastest.memberId)
        assertEquals(4, settling.fastest.averageDays)
    }

    @Test
    fun `GIVEN an average of a day and a half WHEN ranking THEN it rounds up to two days`() {
        // Given
        prepareScenario(
            bills = listOf(
                bill("coffee", "2026-10-01T12:00:00Z", PIERRE, PIERRE to 100, ANA to 100),
                bill("lunch", "2026-10-10T12:00:00Z", PIERRE, PIERRE to 100, ANA to 100),
                brunoOpenDebt,
            ),
            payments = listOf(
                payment(ANA, PIERRE, 100, "2026-10-02T12:00:00Z"),
                payment(ANA, PIERRE, 100, "2026-10-12T12:00:00Z"),
            ),
        )

        // When
        val settling = assertNotNull(calculator.createOrNull(october))

        // Then
        assertEquals(ANA, settling.fastest.memberId)
        assertEquals(2, settling.fastest.averageDays)
    }

    @Test
    fun `GIVEN an average under a day and a half WHEN ranking THEN it rounds down to one day`() {
        // Given
        prepareScenario(
            bills = listOf(
                bill("coffee", "2026-10-01T12:00:00Z", PIERRE, PIERRE to 300, ANA to 300),
                bill("lunch", "2026-10-10T12:00:00Z", PIERRE, PIERRE to 100, ANA to 100),
                brunoOpenDebt,
            ),
            payments = listOf(
                payment(ANA, PIERRE, 300, "2026-10-02T12:00:00Z"),
                payment(ANA, PIERRE, 100, "2026-10-12T12:00:00Z"),
            ),
        )

        // When
        val settling = assertNotNull(calculator.createOrNull(october))

        // Then
        assertEquals(ANA, settling.fastest.memberId)
        assertEquals(1, settling.fastest.averageDays)
    }

    @Test
    fun `GIVEN a partial payment WHEN ranking THEN the rest stays open and counts up to today`() {
        // Given
        prepareScenario(
            bills = listOf(
                bill("dinner", "2026-10-19T12:00:00Z", PIERRE, PIERRE to 100, ANA to 100),
                bill("taxi", "2026-10-25T12:00:00Z", PIERRE, PIERRE to 100, BRUNO to 100),
            ),
            payments = listOf(payment(ANA, PIERRE, 40, "2026-10-21T12:00:00Z")),
        )

        // When
        val settling = assertNotNull(calculator.createOrNull(october))

        // Then
        assertEquals(ANA, settling.slowest.memberId)
        assertEquals(7, settling.slowest.averageDays)
        assertEquals(60L, settling.slowest.openMinor)
        assertEquals(BRUNO, settling.fastest.memberId)
        assertEquals(4, settling.fastest.averageDays)
        assertEquals(100L, settling.fastest.openMinor)
    }

    @Test
    fun `GIVEN a debt covered half by a payment and half later WHEN ranking THEN each half counts its own days`() {
        // Given
        prepareScenario(
            bills = listOf(
                bill("dinner", "2026-10-10T12:00:00Z", PIERRE, PIERRE to 100, ANA to 100),
                brunoOpenDebt,
            ),
            payments = listOf(
                payment(ANA, PIERRE, 50, "2026-10-11T12:00:00Z"),
                payment(ANA, PIERRE, 50, "2026-10-17T12:00:00Z"),
            ),
        )

        // When
        val settling = assertNotNull(calculator.createOrNull(october))

        // Then
        assertEquals(ANA, settling.fastest.memberId)
        assertEquals(4, settling.fastest.averageDays)
        assertEquals(0L, settling.fastest.openMinor)
    }

    @Test
    fun `GIVEN an older debt from before the period WHEN a payment comes THEN it covers the older debt first`() {
        // Given
        prepareScenario(
            bills = listOf(
                bill("market", "2026-09-20T12:00:00Z", PIERRE, PIERRE to 100, ANA to 100),
                bill("dinner", "2026-10-05T12:00:00Z", PIERRE, PIERRE to 100, ANA to 100),
                brunoOpenDebt,
            ),
            payments = listOf(
                payment(ANA, PIERRE, 100, "2026-10-06T12:00:00Z"),
                payment(ANA, PIERRE, 100, "2026-10-15T12:00:00Z"),
            ),
        )

        // When
        val settling = assertNotNull(calculator.createOrNull(october))

        // Then
        assertEquals(ANA, settling.slowest.memberId)
        assertEquals(10, settling.slowest.averageDays)
        assertEquals(0L, settling.slowest.openMinor)
    }

    @Test
    fun `GIVEN a debt of the period paid after it WHEN ranking THEN the payment still counts`() {
        // Given
        prepareScenario(
            bills = listOf(
                bill("rent", "2026-09-10T12:00:00Z", PIERRE, PIERRE to 100, BRUNO to 100),
                bill("market", "2026-09-28T12:00:00Z", PIERRE, PIERRE to 100, ANA to 100),
            ),
            payments = listOf(
                payment(BRUNO, PIERRE, 100, "2026-09-30T12:00:00Z"),
                payment(ANA, PIERRE, 100, "2026-10-03T12:00:00Z"),
            ),
        )

        // When
        val settling = assertNotNull(calculator.createOrNull(september))

        // Then
        assertEquals(ANA, settling.fastest.memberId)
        assertEquals(5, settling.fastest.averageDays)
        assertEquals(0L, settling.fastest.openMinor)
        assertEquals(BRUNO, settling.slowest.memberId)
        assertEquals(20, settling.slowest.averageDays)
    }

    @Test
    fun `GIVEN only one debt born in the period WHEN ranking THEN there is nobody to compare`() {
        // Given
        prepareScenario(
            bills = listOf(
                bill("market", "2026-09-20T12:00:00Z", PIERRE, PIERRE to 100, ANA to 100),
                brunoOpenDebt,
            ),
        )

        // When
        val settling = calculator.createOrNull(october)

        // Then
        assertNull(settling)
    }

    @Test
    fun `GIVEN one person with a debt WHEN ranking THEN there is nobody to compare`() {
        // Given
        prepareScenario(
            bills = listOf(bill("dinner", "2026-10-10T12:00:00Z", PIERRE, PIERRE to 100, ANA to 100)),
        )

        // When
        val settling = calculator.createOrNull(october)

        // Then
        assertNull(settling)
    }

    @Test
    fun `GIVEN a group with no payments WHEN ranking THEN the open debts count up to today`() {
        // Given
        prepareScenario(
            bills = listOf(
                bill("dinner", "2026-10-09T12:00:00Z", PIERRE, PIERRE to 100, ANA to 100),
                bill("taxi", "2026-10-19T12:00:00Z", PIERRE, PIERRE to 100, BRUNO to 100),
            ),
        )

        // When
        val settling = assertNotNull(calculator.createOrNull(october))

        // Then
        assertEquals(BRUNO, settling.fastest.memberId)
        assertEquals(10, settling.fastest.averageDays)
        assertEquals(100L, settling.fastest.openMinor)
        assertEquals(ANA, settling.slowest.memberId)
        assertEquals(20, settling.slowest.averageDays)
        assertEquals(100L, settling.slowest.openMinor)
    }

    @Test
    fun `GIVEN Ana fronted an earlier bill WHEN a later one puts a part on her THEN her credit covers it`() {
        // Given
        prepareScenario(
            bills = listOf(
                bill("dinner", "2026-10-01T12:00:00Z", ANA, ANA to 100, BRUNO to 100),
                bill("taxi", "2026-10-05T12:00:00Z", PIERRE, PIERRE to 50, ANA to 50),
            ),
        )

        // When
        val settling = calculator.createOrNull(october)

        // Then
        assertNull(settling)
    }

    @Test
    fun `GIVEN a debt WHEN a later bill Ana paid more than her part of comes THEN it covers the debt`() {
        // Given
        prepareScenario(
            bills = listOf(
                bill("dinner", "2026-10-10T12:00:00Z", PIERRE, PIERRE to 100, ANA to 100),
                bill("boat", "2026-10-13T12:00:00Z", ANA, PIERRE to 100, ANA to 100, BRUNO to 100),
            ),
        )

        // When
        val settling = assertNotNull(calculator.createOrNull(october))

        // Then
        assertEquals(ANA, settling.fastest.memberId)
        assertEquals(3, settling.fastest.averageDays)
        assertEquals(0L, settling.fastest.openMinor)
        assertEquals(BRUNO, settling.slowest.memberId)
        assertEquals(16, settling.slowest.averageDays)
        assertEquals(100L, settling.slowest.openMinor)
    }

    @Test
    fun `GIVEN Ana had credit WHEN she receives a payment THEN it uses the credit and her next part is a debt`() {
        // Given
        prepareScenario(
            bills = listOf(
                bill("dinner", "2026-10-01T12:00:00Z", ANA, ANA to 100, BRUNO to 100),
                bill("taxi", "2026-10-10T12:00:00Z", PIERRE, PIERRE to 100, ANA to 100),
            ),
            payments = listOf(
                payment(BRUNO, ANA, 100, "2026-10-02T12:00:00Z"),
                payment(ANA, PIERRE, 100, "2026-10-13T12:00:00Z"),
            ),
        )

        // When
        val settling = assertNotNull(calculator.createOrNull(october))

        // Then
        assertEquals(BRUNO, settling.fastest.memberId)
        assertEquals(1, settling.fastest.averageDays)
        assertEquals(ANA, settling.slowest.memberId)
        assertEquals(3, settling.slowest.averageDays)
    }

    @Test
    fun `GIVEN Ana received a payment she was not owed WHEN she pays THEN it covers that first and it never counts`() {
        // Given
        prepareScenario(
            bills = listOf(
                bill("dinner", "2026-10-05T12:00:00Z", PIERRE, PIERRE to 100, ANA to 100),
                brunoOpenDebt,
            ),
            payments = listOf(
                payment(PIERRE, ANA, 100, "2026-10-02T12:00:00Z"),
                payment(ANA, PIERRE, 100, "2026-10-06T12:00:00Z"),
            ),
        )

        // When
        val settling = assertNotNull(calculator.createOrNull(october))

        // Then
        assertEquals(ANA, settling.slowest.memberId)
        assertEquals(24, settling.slowest.averageDays)
        assertEquals(100L, settling.slowest.openMinor)
        assertEquals(BRUNO, settling.fastest.memberId)
    }

    @Test
    fun `GIVEN a payment at the instant of the bill WHEN ranking THEN the bill comes first and is paid on the day`() {
        // Given
        prepareScenario(
            bills = listOf(
                bill("dinner", "2026-10-10T12:00:00Z", PIERRE, PIERRE to 100, ANA to 100),
                brunoOpenDebt,
            ),
            payments = listOf(payment(ANA, PIERRE, 100, "2026-10-10T12:00:00Z")),
        )

        // When
        val settling = assertNotNull(calculator.createOrNull(october))

        // Then
        assertEquals(ANA, settling.fastest.memberId)
        assertEquals(0, settling.fastest.averageDays)
        assertEquals(0L, settling.fastest.openMinor)
    }

    @Test
    fun `GIVEN someone who left the group had a debt WHEN ranking THEN they are left out`() {
        // Given
        prepareScenario(
            bills = listOf(bill("dinner", "2026-10-10T12:00:00Z", PIERRE, PIERRE to 100, ANA to 100, FORMER to 100)),
        )

        // When
        val settling = calculator.createOrNull(october)

        // Then
        assertNull(settling)
    }

    @Test
    fun `GIVEN Bruno is a guest who never joined WHEN ranking THEN he counts like anyone`() {
        // Given
        prepareScenario(
            members = listOf(pierre, ana, bruno.copy(hasJoined = false)),
            bills = listOf(bill("dinner", "2026-10-10T12:00:00Z", PIERRE, PIERRE to 100, ANA to 100, BRUNO to 100)),
            payments = listOf(payment(ANA, PIERRE, 100, "2026-10-11T12:00:00Z")),
        )

        // When
        val settling = assertNotNull(calculator.createOrNull(october))

        // Then
        assertEquals(ANA, settling.fastest.memberId)
        assertEquals(BRUNO, settling.slowest.memberId)
        assertEquals(19, settling.slowest.averageDays)
    }

    @Test
    fun `GIVEN two people with the same average WHEN ranking THEN the one with less open is the fastest`() {
        // Given
        prepareScenario(
            bills = listOf(
                bill("taxi", "2026-10-10T12:00:00Z", PIERRE, PIERRE to 100, BRUNO to 100),
                bill("dinner", "2026-10-24T12:00:00Z", PIERRE, PIERRE to 100, ANA to 100),
            ),
            payments = listOf(payment(BRUNO, PIERRE, 100, "2026-10-15T12:00:00Z")),
        )

        // When
        val settling = assertNotNull(calculator.createOrNull(october))

        // Then
        assertEquals(BRUNO, settling.fastest.memberId)
        assertEquals(5, settling.fastest.averageDays)
        assertEquals(ANA, settling.slowest.memberId)
        assertEquals(5, settling.slowest.averageDays)
    }

    @Test
    fun `GIVEN the same average and open amount WHEN ranking THEN the group order decides between two people`() {
        // Given
        prepareScenario(
            members = listOf(pierre, bruno, ana),
            bills = listOf(bill("dinner", "2026-10-10T12:00:00Z", PIERRE, PIERRE to 100, ANA to 100, BRUNO to 100)),
        )

        // When
        val settling = assertNotNull(calculator.createOrNull(october))

        // Then
        assertEquals(BRUNO, settling.fastest.memberId)
        assertEquals(ANA, settling.slowest.memberId)
        assertNotEquals(settling.fastest.memberId, settling.slowest.memberId)
    }

    @Test
    fun `GIVEN a bill and a payment on two UTC days WHEN ranking in Sao Paulo THEN both are on one day`() {
        // Given
        prepareScenario(
            bills = listOf(
                bill("dinner", "2026-10-10T23:30:00Z", PIERRE, PIERRE to 100, ANA to 100),
                brunoOpenDebt,
            ),
            payments = listOf(payment(ANA, PIERRE, 100, "2026-10-11T01:00:00Z")),
            timeZone = TimeZone.of("America/Sao_Paulo"),
        )

        // When
        val settling = assertNotNull(calculator.createOrNull(october))

        // Then
        assertEquals(ANA, settling.fastest.memberId)
        assertEquals(0, settling.fastest.averageDays)
    }

    @Test
    fun `GIVEN a pending payment WHEN ranking THEN it counts like a synced one`() {
        // Given
        prepareScenario(
            bills = listOf(
                bill("dinner", "2026-10-10T12:00:00Z", PIERRE, PIERRE to 100, ANA to 100),
                brunoOpenDebt,
            ),
            payments = listOf(payment(ANA, PIERRE, 100, "2026-10-12T12:00:00Z", isPending = true)),
        )

        // When
        val settling = assertNotNull(calculator.createOrNull(october))

        // Then
        assertEquals(ANA, settling.fastest.memberId)
        assertEquals(2, settling.fastest.averageDays)
        assertEquals(0L, settling.fastest.openMinor)
    }

    @Test
    fun `GIVEN Pierre and Ana with debts WHEN ranking THEN each shows their name face and whether it is me`() {
        // Given
        prepareScenario(
            members = listOf(
                pierre.copy(name = "Pierre Vieira"),
                ana.copy(
                    name = "Ana Souza",
                    avatarColor = AvatarColor.LILAC,
                    photoUrl = ANA_PHOTO,
                ),
                bruno,
            ),
            bills = listOf(bill("dinner", "2026-10-10T12:00:00Z", BRUNO, PIERRE to 100, ANA to 100, BRUNO to 100)),
            payments = listOf(payment(ANA, BRUNO, 100, "2026-10-12T12:00:00Z")),
        )

        // When
        val settling = assertNotNull(calculator.createOrNull(october))

        // Then
        val fastest = settling.fastest
        assertEquals(ANA, fastest.memberId)
        assertEquals("Ana Souza", fastest.name)
        assertEquals("AS", fastest.initials)
        assertEquals(AvatarColor.LILAC, fastest.avatarColor)
        assertEquals(ANA_PHOTO, fastest.photoUrl)
        assertEquals(false, fastest.isMe)
        val slowest = settling.slowest
        assertEquals(PIERRE, slowest.memberId)
        assertEquals("Pierre Vieira", slowest.name)
        assertEquals("P", slowest.initials)
        assertNull(slowest.photoUrl)
        assertEquals(true, slowest.isMe)
    }

    // Why: Thursday, 29 October 2026, in the afternoon.
    private val todayMillis = millisOf("2026-10-29T15:00:00Z")

    private val october = DateSpan(LocalDate(2026, 10, 1), LocalDate(2026, 10, 31))
    private val september = DateSpan(LocalDate(2026, 9, 1), LocalDate(2026, 9, 30))

    private val pierre = member(PIERRE, "Pierre", isMe = true)
    private val ana = member(ANA, "Ana")
    private val bruno = member(BRUNO, "Bruno")
    private val carla = member(CARLA, "Carla")

    // Why: R$ 1 Pierre fronted for Bruno on the 20th, still open nine days later.
    private val brunoOpenDebt = bill("taxi", "2026-10-20T12:00:00Z", PIERRE, PIERRE to 100, BRUNO to 100)

    private fun millisOf(instant: String): Long = Instant.parse(instant).toEpochMilliseconds()

    private fun member(
        id: String,
        name: String,
        isMe: Boolean = false,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = AvatarColor.BLUE,
        isMe = isMe,
        hasJoined = true,
        userId = null,
        inviteCode = null,
        photoUrl = null,
    )

    // Why: a bill one person paid whole, split as the shares say.
    private fun bill(
        id: String,
        at: String,
        payerId: String,
        vararg shares: Pair<String, Long>,
    ): Expense {
        val total = shares.sumOf { it.second }
        return Expense(
            id = id,
            groupId = GROUP_ID,
            description = id,
            amountMinor = total,
            splitMode = SplitMode.AMOUNT,
            payers = listOf(
                ExpensePayer(
                    memberId = payerId,
                    amountMinor = total,
                ),
            ),
            shares = shares.map { (memberId, amount) -> ExpenseShare(memberId, amount, weight = 1) },
            items = null,
            categoryId = null,
            spentAt = millisOf(at),
            isPending = false,
            updatedAt = millisOf(at),
            updatedByMemberId = null,
        )
    }

    private fun payment(
        fromMemberId: String,
        toMemberId: String,
        amountMinor: Long,
        at: String,
        isPending: Boolean = false,
    ): Settlement = Settlement(
        id = "$fromMemberId-$toMemberId-$at",
        groupId = GROUP_ID,
        fromMemberId = fromMemberId,
        toMemberId = toMemberId,
        amountMinor = amountMinor,
        method = SettlementMethod.PIX,
        paidAt = millisOf(at),
        isPending = isPending,
        updatedAt = millisOf(at),
    )

    private fun prepareScenario(
        members: List<GroupMember> = listOf(pierre, ana, bruno),
        bills: List<Expense> = emptyList(),
        payments: List<Settlement> = emptyList(),
        timeZone: TimeZone = TimeZone.UTC,
    ) {
        calculator = SettlingCalculator(
            group = Group(
                id = GROUP_ID,
                name = "Viagem Floripa",
                type = GroupType.TRIP,
                color = AvatarColor.PEACH,
                shape = GroupShape.SQUARE,
                photo = null,
                currency = GroupCurrency.BRL,
                members = members,
                isPending = false,
                inviteCode = null,
                isCreatedByMe = true,
                defaultSplit = null,
                updatedAt = 0L,
            ),
            expenses = bills,
            settlements = payments,
            today = SpentDay.dateOf(todayMillis, timeZone),
            timeZone = timeZone,
        )
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val PIERRE = "member-1"
        const val ANA = "member-2"
        const val BRUNO = "member-3"
        const val CARLA = "member-4"
        const val FORMER = "member-9"
        const val ANA_PHOTO = "https://example.com/ana.jpg"
    }
}
