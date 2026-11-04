package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.CounterpartShift
import com.quare.blitzsplit.core.expenses.domain.model.Debt
import com.quare.blitzsplit.core.expenses.domain.model.DebtShift
import com.quare.blitzsplit.core.expenses.domain.model.DebtShifts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DebtShiftsTest {
    @Test
    fun `GIVEN a chain simplified WHEN listing the shifts THEN says who pays whom now and who stops`() {
        // Given
        val before = listOf(Debt(ANA, BRUNO, 100L), Debt(BRUNO, CARLA, 100L))
        val after = listOf(Debt(ANA, CARLA, 100L))

        // When
        val shifts = DebtShifts.shiftsOf(
            before,
            after,
            memberOrder = listOf(ANA, BRUNO, CARLA),
            meId = null,
        )

        // Then
        assertEquals(
            listOf(
                DebtShift(
                    memberId = ANA,
                    pays = listOf(Debt(ANA, CARLA, 100L)),
                    stopsPaying = listOf(Debt(ANA, BRUNO, 100L)),
                    receivesFrom = emptyList(),
                    stopsReceivingFrom = emptyList(),
                ),
                DebtShift(
                    memberId = BRUNO,
                    pays = emptyList(),
                    stopsPaying = listOf(Debt(BRUNO, CARLA, 100L)),
                    receivesFrom = emptyList(),
                    stopsReceivingFrom = listOf(ANA),
                ),
                DebtShift(
                    memberId = CARLA,
                    pays = emptyList(),
                    stopsPaying = emptyList(),
                    receivesFrom = listOf(ANA),
                    stopsReceivingFrom = listOf(BRUNO),
                ),
            ),
            shifts,
        )
    }

    @Test
    fun `GIVEN a pair nothing changes for WHEN listing the shifts THEN its two people are left out`() {
        // Given
        val before = listOf(Debt(ANA, BRUNO, 100L), Debt(BRUNO, CARLA, 100L), Debt(DIEGO, ELISA, 10L))
        val after = listOf(Debt(ANA, CARLA, 100L), Debt(DIEGO, ELISA, 10L))

        // When
        val shifts = DebtShifts.shiftsOf(
            before,
            after,
            memberOrder = memberOrder,
            meId = null,
        )

        // Then
        assertEquals(listOf(ANA, BRUNO, CARLA), shifts.map { it.memberId })
    }

    @Test
    fun `GIVEN one payment changes its amount WHEN listing the shifts THEN lists every payment they make after`() {
        // Given
        val before = listOf(Debt(ANA, BRUNO, 100L), Debt(ANA, CARLA, 50L))
        val after = listOf(Debt(ANA, BRUNO, 100L), Debt(ANA, CARLA, 80L))

        // When
        val shifts = DebtShifts.shiftsOf(
            before,
            after,
            memberOrder = memberOrder,
            meId = null,
        )

        // Then
        assertEquals(
            listOf(
                DebtShift(
                    memberId = ANA,
                    pays = listOf(Debt(ANA, BRUNO, 100L), Debt(ANA, CARLA, 80L)),
                    stopsPaying = emptyList(),
                    receivesFrom = emptyList(),
                    stopsReceivingFrom = emptyList(),
                ),
            ),
            shifts,
        )
    }

    @Test
    fun `GIVEN someone new pays a person WHEN listing the shifts THEN lists everyone who pays that person after`() {
        // Given
        val before = listOf(Debt(BRUNO, CARLA, 50L))
        val after = listOf(Debt(BRUNO, CARLA, 50L), Debt(ANA, CARLA, 30L))

        // When
        val shifts = DebtShifts.shiftsOf(
            before,
            after,
            memberOrder = memberOrder,
            meId = null,
        )

        // Then
        assertEquals(
            listOf(
                DebtShift(
                    memberId = ANA,
                    pays = listOf(Debt(ANA, CARLA, 30L)),
                    stopsPaying = emptyList(),
                    receivesFrom = emptyList(),
                    stopsReceivingFrom = emptyList(),
                ),
                DebtShift(
                    memberId = CARLA,
                    pays = emptyList(),
                    stopsPaying = emptyList(),
                    receivesFrom = listOf(BRUNO, ANA),
                    stopsReceivingFrom = emptyList(),
                ),
            ),
            shifts,
        )
    }

    @Test
    fun `GIVEN the user and ex-members WHEN listing the shifts THEN the user then join order then ex-members`() {
        // Given
        val before = listOf(Debt(EX_MEMBER_Z, PIERRE, 10L), Debt(EX_MEMBER_A, BRUNO, 10L), Debt(ANA, BRUNO, 5L))
        val after = emptyList<Debt>()

        // When
        val shifts = DebtShifts.shiftsOf(
            before,
            after,
            memberOrder = memberOrder,
            meId = PIERRE,
        )

        // Then
        assertEquals(listOf(PIERRE, ANA, BRUNO, EX_MEMBER_A, EX_MEMBER_Z), shifts.map { it.memberId })
    }

    @Test
    fun `GIVEN the same pairs before and after WHEN listing the shifts THEN nothing changes for anyone`() {
        // Given
        val pairs = listOf(Debt(ANA, BRUNO, 100L), Debt(CARLA, BRUNO, 50L))

        // When
        val shifts = DebtShifts.shiftsOf(
            pairs,
            pairs,
            memberOrder = memberOrder,
            meId = ANA,
        )

        // Then
        assertTrue(shifts.isEmpty())
    }

    @Test
    fun `GIVEN pairs that moved WHEN listing counterparts THEN signs what they owe largest change first`() {
        // Given
        val before = listOf(Debt(BRUNO, PIERRE, 100L), Debt(PIERRE, CARLA, 30L))
        val after = listOf(Debt(DIEGO, PIERRE, 64L), Debt(BRUNO, PIERRE, 40L), Debt(PIERRE, CARLA, 30L))

        // When
        val shifts = DebtShifts.counterpartShiftsOf(before, after, memberId = PIERRE)

        // Then
        assertEquals(
            listOf(
                CounterpartShift(
                    DIEGO,
                    beforeMinor = 0L,
                    afterMinor = 64L,
                ),
                CounterpartShift(
                    BRUNO,
                    beforeMinor = 100L,
                    afterMinor = 40L,
                ),
            ),
            shifts,
        )
    }

    @Test
    fun `GIVEN the member owes someone else after WHEN listing the counterparts THEN what they owe is negative`() {
        // Given
        val before = listOf(Debt(PIERRE, CARLA, 30L))
        val after = listOf(Debt(PIERRE, DIEGO, 30L))

        // When
        val shifts = DebtShifts.counterpartShiftsOf(before, after, memberId = PIERRE)

        // Then
        assertEquals(
            listOf(
                CounterpartShift(
                    DIEGO,
                    beforeMinor = 0L,
                    afterMinor = -30L,
                ),
                CounterpartShift(
                    CARLA,
                    beforeMinor = -30L,
                    afterMinor = 0L,
                ),
            ),
            shifts,
        )
    }

    @Test
    fun `GIVEN only pairs between others changed WHEN listing the counterparts of a member THEN there is none`() {
        // Given
        val before = listOf(Debt(ANA, BRUNO, 100L), Debt(BRUNO, CARLA, 100L))
        val after = listOf(Debt(ANA, CARLA, 100L))

        // When
        val shifts = DebtShifts.counterpartShiftsOf(before, after, memberId = PIERRE)

        // Then
        assertTrue(shifts.isEmpty())
    }

    private val memberOrder = listOf(ANA, BRUNO, PIERRE, CARLA, DIEGO, ELISA)

    private companion object {
        const val PIERRE = "pierre"
        const val ANA = "ana"
        const val BRUNO = "bruno"
        const val CARLA = "carla"
        const val DIEGO = "diego"
        const val ELISA = "elisa"

        // Why: ids of people no longer in the group.
        const val EX_MEMBER_A = "a-ex"
        const val EX_MEMBER_Z = "z-ex"
    }
}
