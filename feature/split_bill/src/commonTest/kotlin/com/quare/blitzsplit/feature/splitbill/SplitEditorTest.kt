package com.quare.blitzsplit.feature.splitbill

import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillPerson
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitEditor
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitFact
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SplitEditorTest {
    @Test
    fun `GIVEN an equal split with cents left over WHEN reading the amount each THEN rounds it down`() {
        // Given
        val editor = editor(SplitMode.EQUAL, totalMinor = 10_001L, row(ANA), row(PIERRE), row(BRUNO))

        // When
        val perPerson = editor.perPersonMinor

        // Then
        assertEquals(3_333L, perPerson)
    }

    @Test
    fun `GIVEN an equal split with cents left over WHEN reading the shares THEN the payer takes them`() {
        // Given
        val editor = editor(SplitMode.EQUAL, totalMinor = 10_001L, row(ANA), row(PIERRE), row(BRUNO))

        // When
        val shares = editor.shares

        // Then
        assertEquals(
            listOf(
                ExpenseShare(ANA, 3_333L, weight = 1L),
                ExpenseShare(PIERRE, 3_335L, weight = 1L),
                ExpenseShare(BRUNO, 3_333L, weight = 1L),
            ),
            shares,
        )
    }

    @Test
    fun `GIVEN the payer out of an equal split WHEN reading the shares THEN the first members take one cent each`() {
        // Given
        val editor = editor(
            SplitMode.EQUAL,
            totalMinor = 10_001L,
            row(ANA),
            row(PIERRE, isIncluded = false),
            row(BRUNO),
            row(CARLA),
        )

        // When
        val shares = editor.shares

        // Then
        assertEquals(
            listOf(
                ExpenseShare(ANA, 3_334L, weight = 1L),
                ExpenseShare(BRUNO, 3_334L, weight = 1L),
                ExpenseShare(CARLA, 3_333L, weight = 1L),
            ),
            shares,
        )
    }

    @Test
    fun `GIVEN nobody in an equal split WHEN reading it THEN nobody gets anything and it does not add up`() {
        // Given
        val editor = editor(SplitMode.EQUAL, totalMinor = 10_000L, row(ANA, isIncluded = false))

        // When
        val perPerson = editor.perPersonMinor

        // Then
        assertEquals(0L, perPerson)
        assertNull(editor.shares)
        assertFalse(editor.addsUp)
    }

    @Test
    fun `GIVEN no total WHEN reading the split THEN it does not add up`() {
        // Given
        val editor = editor(SplitMode.EQUAL, totalMinor = 0L, row(ANA), row(PIERRE))

        // When
        val addsUp = editor.addsUp

        // Then
        assertFalse(addsUp)
        assertNull(editor.shares)
    }

    @Test
    fun `GIVEN Valores with a field that is no amount WHEN reading what is assigned THEN counts it as zero`() {
        // Given
        val editor = editor(
            SplitMode.AMOUNT,
            totalMinor = 10_000L,
            row(ANA, valueText = "60,00"),
            row(PIERRE, valueText = "abc"),
            row(BRUNO, valueText = ""),
        )

        // When
        val assigned = editor.assignedMinor

        // Then
        assertEquals(6_000L, assigned)
        assertEquals(4_000L, editor.remainingMinor)
        assertEquals(listOf(PIERRE, BRUNO), editor.withoutValue.map { it.id })
        assertFalse(editor.addsUp)
    }

    @Test
    fun `GIVEN Valores over the total WHEN reading what remains THEN it is negative`() {
        // Given
        val editor = editor(
            SplitMode.AMOUNT,
            totalMinor = 10_000L,
            row(ANA, valueText = "60,00"),
            row(PIERRE, valueText = "50,00"),
        )

        // When
        val remaining = editor.remainingMinor

        // Then
        assertEquals(-1_000L, remaining)
        assertFalse(editor.addsUp)
    }

    @Test
    fun `GIVEN Valores adding up to the total WHEN reading the shares THEN each gets what was typed`() {
        // Given
        val editor = editor(
            SplitMode.AMOUNT,
            totalMinor = 10_000L,
            row(ANA, valueText = "60,00"),
            row(PIERRE, valueText = "40.00"),
            row(BRUNO, valueText = "0"),
        )

        // When
        val shares = editor.shares

        // Then
        assertEquals(
            listOf(
                ExpenseShare(ANA, 6_000L, weight = 6_000L),
                ExpenseShare(PIERRE, 4_000L, weight = 4_000L),
            ),
            shares,
        )
        assertEquals(listOf(BRUNO), editor.excluded.map { it.id })
        assertEquals(listOf(BRUNO), editor.withoutValue.map { it.id })
    }

    @Test
    fun `GIVEN an equal split with someone out WHEN reading the Valores status THEN nothing is missing`() {
        // Given
        val editor = editor(SplitMode.EQUAL, totalMinor = 10_000L, row(ANA), row(PIERRE, isIncluded = false))

        // When
        val remaining = editor.remainingMinor

        // Then
        assertEquals(10_000L, editor.assignedMinor)
        assertEquals(0L, remaining)
        assertTrue(editor.withoutValue.isEmpty())
    }

    @Test
    fun `GIVEN percentages under 100 WHEN reading the percent status THEN shows what is missing`() {
        // Given
        val editor = editor(
            SplitMode.PERCENT,
            totalMinor = 10_000L,
            row(ANA, valueText = "50"),
            row(PIERRE, valueText = "35"),
            row(BRUNO, valueText = ""),
        )

        // When
        val sum = editor.percentSum

        // Then
        assertEquals(85L, sum)
        assertEquals(15L, editor.remainingPercent)
        assertFalse(editor.addsUp)
    }

    @Test
    fun `GIVEN percentages adding up to 100 with a cent left over WHEN reading the shares THEN the payer takes it`() {
        // Given
        val editor = editor(
            SplitMode.PERCENT,
            totalMinor = 10_001L,
            row(ANA, valueText = "50"),
            row(PIERRE, valueText = "50"),
        )

        // When
        val shares = editor.shares

        // Then
        assertEquals(
            listOf(
                ExpenseShare(ANA, 5_000L, weight = 50L),
                ExpenseShare(PIERRE, 5_001L, weight = 50L),
            ),
            shares,
        )
    }

    @Test
    fun `GIVEN an equal split WHEN reading the percent status THEN nothing is missing`() {
        // Given
        val editor = editor(SplitMode.EQUAL, totalMinor = 10_000L, row(ANA), row(PIERRE))

        // When
        val sum = editor.percentSum

        // Then
        assertEquals(100L, sum)
        assertEquals(0L, editor.remainingPercent)
    }

    @Test
    fun `GIVEN Partes WHEN reading the parts THEN counts them and the value of each`() {
        // Given
        val editor = editor(
            SplitMode.SHARES,
            totalMinor = 10_000L,
            row(ANA, parts = 2L),
            row(PIERRE, parts = 3L),
            row(BRUNO, parts = 0L),
        )

        // When
        val totalParts = editor.totalParts

        // Then
        assertEquals(5L, totalParts)
        assertEquals(2_000L, editor.partValueMinor)
        assertEquals(listOf(ANA, PIERRE), editor.included.map { it.id })
        assertEquals(listOf(BRUNO), editor.excluded.map { it.id })
        assertEquals(
            listOf(
                ExpenseShare(ANA, 4_000L, weight = 2L),
                ExpenseShare(PIERRE, 6_000L, weight = 3L),
            ),
            editor.shares,
        )
    }

    @Test
    fun `GIVEN Partes with no parts WHEN reading the value of a part THEN it is zero`() {
        // Given
        val editor = editor(SplitMode.SHARES, totalMinor = 10_000L, row(ANA, parts = 0L), row(PIERRE, parts = 0L))

        // When
        val partValue = editor.partValueMinor

        // Then
        assertEquals(0L, partValue)
        assertFalse(editor.addsUp)
    }

    @Test
    fun `GIVEN an equal split WHEN reading the Partes status THEN counts no parts`() {
        // Given
        val editor = editor(SplitMode.EQUAL, totalMinor = 10_000L, row(ANA, parts = 2L), row(PIERRE, parts = 1L))

        // When
        val totalParts = editor.totalParts

        // Then
        assertEquals(0L, totalParts)
        assertEquals(0L, editor.partValueMinor)
    }

    @Test
    fun `GIVEN everyone checked WHEN reading the toggle THEN everyone is included`() {
        // Given
        val editor = editor(SplitMode.EQUAL, totalMinor = 10_000L, row(ANA), row(PIERRE))

        // When
        val isEveryoneIncluded = editor.isEveryoneIncluded

        // Then
        assertTrue(isEveryoneIncluded)
        assertEquals(2, editor.includedCount)
    }

    @Test
    fun `GIVEN someone unchecked WHEN reading the toggle THEN not everyone is included`() {
        // Given
        val editor = editor(SplitMode.EQUAL, totalMinor = 10_000L, row(ANA), row(PIERRE, isIncluded = false))

        // When
        val isEveryoneIncluded = editor.isEveryoneIncluded

        // Then
        assertFalse(isEveryoneIncluded)
        assertEquals(listOf(ANA), editor.included.map { it.id })
        assertEquals(listOf(PIERRE), editor.excluded.map { it.id })
    }

    @Test
    fun `GIVEN an editor WHEN copying it with someone unchecked THEN the copy splits without them`() {
        // Given
        val editor = editor(SplitMode.EQUAL, totalMinor = 9_000L, row(ANA), row(PIERRE), row(BRUNO))
        val rows = editor.rows.map { if (it.person.id == BRUNO) it.copy(isIncluded = false) else it }

        // When
        val copy = editor.copy(rows = rows)

        // Then
        assertEquals(listOf(1L, 1L, 0L), copy.entries.map { it.value })
        assertEquals(
            listOf(
                ExpenseShare(ANA, 4_500L, weight = 1L),
                ExpenseShare(PIERRE, 4_500L, weight = 1L),
            ),
            copy.shares,
        )
        assertEquals(listOf(1L, 1L, 1L), editor.entries.map { it.value })
    }

    @Test
    fun `GIVEN an editor WHEN copying it with a new total THEN the copy splits the new total`() {
        // Given
        val editor = editor(SplitMode.EQUAL, totalMinor = 9_000L, row(ANA), row(PIERRE))

        // When
        val copy = editor.copy(totalMinor = 3_000L)

        // Then
        assertEquals(
            listOf(
                ExpenseShare(ANA, 1_500L, weight = 1L),
                ExpenseShare(PIERRE, 1_500L, weight = 1L),
            ),
            copy.shares,
        )
    }

    @Test
    fun `GIVEN an equal split with nobody checked WHEN reading the status title THEN nobody takes part`() {
        // Given
        val editor = editor(
            SplitMode.EQUAL,
            totalMinor = 10_000L,
            row(ANA, isIncluded = false),
            row(PIERRE, isIncluded = false),
        )

        // When
        val title = editor.statusTitle

        // Then
        assertEquals(SplitFact.Nobody, title)
    }

    @Test
    fun `GIVEN an equal split with cents left over WHEN reading the status title THEN rounds each down`() {
        // Given
        val editor = editor(SplitMode.EQUAL, totalMinor = 10_001L, row(ANA), row(PIERRE), row(BRUNO))

        // When
        val title = editor.statusTitle

        // Then
        assertEquals(SplitFact.Each(3_333L), title)
    }

    @Test
    fun `GIVEN an equal split with no total WHEN reading the status title THEN each pays nothing`() {
        // Given
        val editor = editor(SplitMode.EQUAL, totalMinor = 0L, row(ANA), row(PIERRE))

        // When
        val title = editor.statusTitle

        // Then
        assertEquals(SplitFact.Each(0L), title)
    }

    @Test
    fun `GIVEN Valores under the total WHEN reading the status title THEN says what is missing`() {
        // Given
        val editor = editor(
            SplitMode.AMOUNT,
            totalMinor = 64_200L,
            row(ANA, valueText = "300,00"),
            row(PIERRE, valueText = "258,00"),
        )

        // When
        val title = editor.statusTitle

        // Then
        assertEquals(SplitFact.AmountLeft(8_400L), title)
    }

    @Test
    fun `GIVEN Valores over the total WHEN reading the status title THEN says how much is over as a positive amount`() {
        // Given
        val editor = editor(
            SplitMode.AMOUNT,
            totalMinor = 10_000L,
            row(ANA, valueText = "60,00"),
            row(PIERRE, valueText = "52,00"),
        )

        // When
        val title = editor.statusTitle

        // Then
        assertEquals(SplitFact.AmountOver(1_200L), title)
    }

    @Test
    fun `GIVEN Valores adding up to the total WHEN reading the status title THEN says it adds up to the total`() {
        // Given
        val editor = editor(
            SplitMode.AMOUNT,
            totalMinor = 10_000L,
            row(ANA, valueText = "60,00"),
            row(PIERRE, valueText = "40,00"),
        )

        // When
        val title = editor.statusTitle

        // Then
        assertEquals(SplitFact.AmountAddsUp(10_000L), title)
    }

    @Test
    fun `GIVEN Valores with no total nor values WHEN reading the status title THEN adds up to zero`() {
        // Given
        val editor = editor(SplitMode.AMOUNT, totalMinor = 0L, row(ANA), row(PIERRE))

        // When
        val title = editor.statusTitle

        // Then
        assertEquals(SplitFact.AmountAddsUp(0L), title)
    }

    @Test
    fun `GIVEN percentages under 100 WHEN reading the status title THEN says the percent missing`() {
        // Given
        val editor = editor(
            SplitMode.PERCENT,
            totalMinor = 10_000L,
            row(ANA, valueText = "50"),
            row(PIERRE, valueText = "35"),
        )

        // When
        val title = editor.statusTitle

        // Then
        assertEquals(SplitFact.PercentLeft(15L), title)
    }

    @Test
    fun `GIVEN percentages over 100 WHEN reading the status title THEN says the percent over as a positive number`() {
        // Given
        val editor = editor(
            SplitMode.PERCENT,
            totalMinor = 10_000L,
            row(ANA, valueText = "70"),
            row(PIERRE, valueText = "45"),
        )

        // When
        val title = editor.statusTitle

        // Then
        assertEquals(SplitFact.PercentOver(15L), title)
    }

    @Test
    fun `GIVEN percentages adding up to 100 WHEN reading the status title THEN says it adds up`() {
        // Given
        val editor = editor(
            SplitMode.PERCENT,
            totalMinor = 10_000L,
            row(ANA, valueText = "60"),
            row(PIERRE, valueText = "40"),
        )

        // When
        val title = editor.statusTitle

        // Then
        assertEquals(SplitFact.PercentAddsUp, title)
    }

    @Test
    fun `GIVEN Partes with no parts WHEN reading the status title THEN nobody takes part`() {
        // Given
        val editor = editor(SplitMode.SHARES, totalMinor = 10_000L, row(ANA, parts = 0L), row(PIERRE, parts = 0L))

        // When
        val title = editor.statusTitle

        // Then
        assertEquals(SplitFact.Nobody, title)
    }

    @Test
    fun `GIVEN Partes with cents left over WHEN reading the status title THEN rounds a part down`() {
        // Given
        val editor = editor(
            SplitMode.SHARES,
            totalMinor = 10_000L,
            row(ANA, parts = 2L),
            row(PIERRE, parts = 1L),
            row(BRUNO, parts = 0L),
        )

        // When
        val title = editor.statusTitle

        // Then
        assertEquals(
            SplitFact.Parts(
                count = 3L,
                partMinor = 3_333L,
            ),
            title,
        )
    }

    @Test
    fun `GIVEN an equal split with nobody checked WHEN reading the details THEN asks to check someone`() {
        // Given
        val editor = editor(
            SplitMode.EQUAL,
            totalMinor = 31_200L,
            row(ANA, isIncluded = false),
            row(PIERRE, isIncluded = false),
        )

        // When
        val details = editor.statusDetails

        // Then
        assertEquals(listOf(SplitFact.NobodyChecked(31_200L)), details)
    }

    @Test
    fun `GIVEN an equal split with everyone in WHEN reading the status details THEN says the total among everyone`() {
        // Given
        val editor = editor(SplitMode.EQUAL, totalMinor = 31_200L, row(ANA), row(PIERRE), row(BRUNO))

        // When
        val details = editor.statusDetails

        // Then
        assertEquals(
            listOf(
                SplitFact.Among(
                    totalMinor = 31_200L,
                    count = 3,
                ),
            ),
            details,
        )
    }

    @Test
    fun `GIVEN an equal split without the payer and me WHEN reading the details THEN says who is out`() {
        // Given
        val editor = editor(
            SplitMode.EQUAL,
            totalMinor = 31_200L,
            row(ANA),
            row(PIERRE, isIncluded = false),
            row(BRUNO),
            row(CARLA, isIncluded = false),
        )

        // When
        val details = editor.statusDetails

        // Then
        assertEquals(
            listOf(
                SplitFact.Among(
                    totalMinor = 31_200L,
                    count = 2,
                ),
                SplitFact.Out(listOf(person(PIERRE), person(CARLA))),
            ),
            details,
        )
        assertTrue(person(PIERRE).isMe)
    }

    @Test
    fun `GIVEN an equal split with no total WHEN reading the status details THEN says the zero total among those in`() {
        // Given
        val editor = editor(SplitMode.EQUAL, totalMinor = 0L, row(ANA), row(PIERRE))

        // When
        val details = editor.statusDetails

        // Then
        assertEquals(
            listOf(
                SplitFact.Among(
                    totalMinor = 0L,
                    count = 2,
                ),
            ),
            details,
        )
    }

    @Test
    fun `GIVEN Valores adding up with someone at zero WHEN reading the details THEN only says it matches`() {
        // Given
        val editor = editor(
            SplitMode.AMOUNT,
            totalMinor = 10_000L,
            row(ANA, valueText = "60,00"),
            row(PIERRE, valueText = "40,00"),
            row(BRUNO, valueText = "0"),
        )

        // When
        val details = editor.statusDetails

        // Then
        assertEquals(listOf(SplitFact.PartsMatchTotal), details)
    }

    @Test
    fun `GIVEN Valores under with blank and zero fields WHEN reading the details THEN says so far and who has none`() {
        // Given
        val editor = editor(
            SplitMode.AMOUNT,
            totalMinor = 64_200L,
            row(ANA, valueText = "558,00"),
            row(PIERRE, valueText = ""),
            row(BRUNO, valueText = "0"),
        )

        // When
        val details = editor.statusDetails

        // Then
        assertEquals(
            listOf(
                SplitFact.AmountSoFar(
                    assignedMinor = 55_800L,
                    totalMinor = 64_200L,
                ),
                SplitFact.WithoutValue(listOf(person(PIERRE), person(BRUNO))),
            ),
            details,
        )
    }

    @Test
    fun `GIVEN Valores over with everyone valued WHEN reading the details THEN only says what was assigned`() {
        // Given
        val editor = editor(
            SplitMode.AMOUNT,
            totalMinor = 10_000L,
            row(ANA, valueText = "60,00"),
            row(PIERRE, valueText = "52,00"),
        )

        // When
        val details = editor.statusDetails

        // Then
        assertEquals(
            listOf(
                SplitFact.AmountSoFar(
                    assignedMinor = 11_200L,
                    totalMinor = 10_000L,
                ),
            ),
            details,
        )
    }

    @Test
    fun `GIVEN Valores with no total nor values WHEN reading the details THEN says the parts match the total`() {
        // Given
        val editor = editor(SplitMode.AMOUNT, totalMinor = 0L, row(ANA), row(PIERRE))

        // When
        val details = editor.statusDetails

        // Then
        assertEquals(listOf(SplitFact.PartsMatchTotal), details)
    }

    @Test
    fun `GIVEN percentages with everyone in WHEN reading the status details THEN says the total among everyone`() {
        // Given
        val editor = editor(
            SplitMode.PERCENT,
            totalMinor = 10_000L,
            row(ANA, valueText = "60"),
            row(PIERRE, valueText = "40"),
        )

        // When
        val details = editor.statusDetails

        // Then
        assertEquals(
            listOf(
                SplitFact.Among(
                    totalMinor = 10_000L,
                    count = 2,
                ),
            ),
            details,
        )
    }

    @Test
    fun `GIVEN percent over 100 with me at zero WHEN reading the details THEN says among those in and who is out`() {
        // Given
        val editor = editor(
            SplitMode.PERCENT,
            totalMinor = 10_000L,
            row(ANA, valueText = "70"),
            row(PIERRE, valueText = "0"),
            row(BRUNO, valueText = "45"),
            row(CARLA, valueText = ""),
        )

        // When
        val details = editor.statusDetails

        // Then
        assertEquals(
            listOf(
                SplitFact.Among(
                    totalMinor = 10_000L,
                    count = 2,
                ),
                SplitFact.Out(listOf(person(PIERRE), person(CARLA))),
            ),
            details,
        )
    }

    @Test
    fun `GIVEN Partes with no parts WHEN reading the status details THEN asks to give someone a part of the total`() {
        // Given
        val editor = editor(SplitMode.SHARES, totalMinor = 31_200L, row(ANA, parts = 0L), row(PIERRE, parts = 0L))

        // When
        val details = editor.statusDetails

        // Then
        assertEquals(listOf(SplitFact.NobodyHasParts(31_200L)), details)
    }

    @Test
    fun `GIVEN Partes with one part each WHEN reading the status details THEN says the total among everyone`() {
        // Given
        val editor = editor(
            SplitMode.SHARES,
            totalMinor = 31_200L,
            row(ANA, parts = 1L),
            row(PIERRE, parts = 1L),
            row(BRUNO, parts = 1L),
        )

        // When
        val details = editor.statusDetails

        // Then
        assertEquals(
            listOf(
                SplitFact.Among(
                    totalMinor = 31_200L,
                    count = 3,
                ),
            ),
            details,
        )
    }

    @Test
    fun `GIVEN Partes with several parts and someone out WHEN reading the details THEN lists them in order then out`() {
        // Given
        val editor = editor(
            SplitMode.SHARES,
            totalMinor = 31_200L,
            row(ANA, parts = 2L),
            row(PIERRE, parts = 3L),
            row(BRUNO, parts = 0L),
            row(CARLA, parts = 1L),
        )

        // When
        val details = editor.statusDetails

        // Then
        assertEquals(
            listOf(
                SplitFact.PersonParts(
                    person = person(ANA),
                    parts = 2L,
                ),
                SplitFact.PersonParts(
                    person = person(PIERRE),
                    parts = 3L,
                ),
                SplitFact.Out(listOf(person(BRUNO))),
            ),
            details,
        )
    }

    @Test
    fun `GIVEN Partes with one part each and the payer out WHEN reading the details THEN only says who is out`() {
        // Given
        val editor = editor(
            SplitMode.SHARES,
            totalMinor = 31_200L,
            row(ANA, parts = 1L),
            row(PIERRE, parts = 0L),
            row(BRUNO, parts = 1L),
        )

        // When
        val details = editor.statusDetails

        // Then
        assertEquals(listOf(SplitFact.Out(listOf(person(PIERRE)))), details)
    }

    @Test
    fun `GIVEN an equal split with everyone in WHEN reading the summary THEN says the amount each`() {
        // Given
        val editor = editor(SplitMode.EQUAL, totalMinor = 10_001L, row(ANA), row(PIERRE), row(BRUNO))

        // When
        val summary = editor.summary

        // Then
        assertEquals(listOf(SplitFact.Each(3_333L)), summary)
    }

    @Test
    fun `GIVEN an equal split without me WHEN reading the summary THEN says the amount each and who is out`() {
        // Given
        val editor =
            editor(SplitMode.EQUAL, totalMinor = 10_000L, row(ANA), row(PIERRE, isIncluded = false), row(BRUNO))

        // When
        val summary = editor.summary

        // Then
        assertEquals(listOf(SplitFact.Each(5_000L), SplitFact.Out(listOf(person(PIERRE)))), summary)
    }

    @Test
    fun `GIVEN an equal split with no total WHEN reading the summary THEN says nothing`() {
        // Given
        val editor = editor(SplitMode.EQUAL, totalMinor = 0L, row(ANA), row(PIERRE))

        // When
        val summary = editor.summary

        // Then
        assertTrue(summary.isEmpty())
    }

    @Test
    fun `GIVEN an equal split with no total and someone out WHEN reading the summary THEN only says who is out`() {
        // Given
        val editor = editor(SplitMode.EQUAL, totalMinor = 0L, row(ANA), row(PIERRE), row(BRUNO, isIncluded = false))

        // When
        val summary = editor.summary

        // Then
        assertEquals(listOf(SplitFact.Out(listOf(person(BRUNO)))), summary)
    }

    @Test
    fun `GIVEN an equal split with nobody checked WHEN reading the summary THEN only says everyone is out`() {
        // Given
        val editor = editor(
            SplitMode.EQUAL,
            totalMinor = 10_000L,
            row(ANA, isIncluded = false),
            row(PIERRE, isIncluded = false),
        )

        // When
        val summary = editor.summary

        // Then
        assertEquals(listOf(SplitFact.Out(listOf(person(ANA), person(PIERRE)))), summary)
    }

    @Test
    fun `GIVEN Valores under with a blank field WHEN reading the summary THEN says what is missing and who is out`() {
        // Given
        val editor = editor(
            SplitMode.AMOUNT,
            totalMinor = 10_000L,
            row(ANA, valueText = "60,00"),
            row(PIERRE, valueText = ""),
        )

        // When
        val summary = editor.summary

        // Then
        assertEquals(listOf(SplitFact.AmountLeft(4_000L), SplitFact.Out(listOf(person(PIERRE)))), summary)
    }

    @Test
    fun `GIVEN Valores over the total WHEN reading the summary THEN says how much is over as a positive amount`() {
        // Given
        val editor = editor(
            SplitMode.AMOUNT,
            totalMinor = 10_000L,
            row(ANA, valueText = "60,00"),
            row(PIERRE, valueText = "52,00"),
        )

        // When
        val summary = editor.summary

        // Then
        assertEquals(listOf(SplitFact.AmountOver(1_200L)), summary)
    }

    @Test
    fun `GIVEN Valores adding up WHEN reading the summary THEN says nothing`() {
        // Given
        val editor = editor(
            SplitMode.AMOUNT,
            totalMinor = 10_000L,
            row(ANA, valueText = "60,00"),
            row(PIERRE, valueText = "40,00"),
        )

        // When
        val summary = editor.summary

        // Then
        assertTrue(summary.isEmpty())
    }

    @Test
    fun `GIVEN Valores adding up with someone at zero WHEN reading the summary THEN only says who is out`() {
        // Given
        val editor = editor(
            SplitMode.AMOUNT,
            totalMinor = 10_000L,
            row(ANA, valueText = "60,00"),
            row(PIERRE, valueText = "40,00"),
            row(BRUNO, valueText = "0"),
        )

        // When
        val summary = editor.summary

        // Then
        assertEquals(listOf(SplitFact.Out(listOf(person(BRUNO)))), summary)
    }

    @Test
    fun `GIVEN percentages under 100 WHEN reading the summary THEN says the percent missing`() {
        // Given
        val editor = editor(
            SplitMode.PERCENT,
            totalMinor = 10_000L,
            row(ANA, valueText = "50"),
            row(PIERRE, valueText = "35"),
        )

        // When
        val summary = editor.summary

        // Then
        assertEquals(listOf(SplitFact.PercentLeft(15L)), summary)
    }

    @Test
    fun `GIVEN percent over 100 with someone at zero WHEN reading the summary THEN says the over and who is out`() {
        // Given
        val editor = editor(
            SplitMode.PERCENT,
            totalMinor = 10_000L,
            row(ANA, valueText = "70"),
            row(PIERRE, valueText = "45"),
            row(BRUNO, valueText = "0"),
        )

        // When
        val summary = editor.summary

        // Then
        assertEquals(listOf(SplitFact.PercentOver(15L), SplitFact.Out(listOf(person(BRUNO)))), summary)
    }

    @Test
    fun `GIVEN percentages adding up to 100 WHEN reading the summary THEN says nothing`() {
        // Given
        val editor = editor(
            SplitMode.PERCENT,
            totalMinor = 10_000L,
            row(ANA, valueText = "60"),
            row(PIERRE, valueText = "40"),
        )

        // When
        val summary = editor.summary

        // Then
        assertTrue(summary.isEmpty())
    }

    @Test
    fun `GIVEN Partes with everyone in WHEN reading the summary THEN says nothing`() {
        // Given
        val editor = editor(SplitMode.SHARES, totalMinor = 10_000L, row(ANA, parts = 2L), row(PIERRE, parts = 1L))

        // When
        val summary = editor.summary

        // Then
        assertTrue(summary.isEmpty())
    }

    @Test
    fun `GIVEN Partes with this device user at zero parts WHEN reading the summary THEN only says who is out`() {
        // Given
        val editor = editor(SplitMode.SHARES, totalMinor = 10_000L, row(ANA, parts = 2L), row(PIERRE, parts = 0L))

        // When
        val summary = editor.summary

        // Then
        assertEquals(listOf(SplitFact.Out(listOf(person(PIERRE)))), summary)
    }

    private fun editor(
        mode: SplitMode,
        totalMinor: Long,
        vararg rows: SplitRow,
    ): SplitEditor = SplitEditor(
        mode = mode,
        rows = rows.toList(),
        totalMinor = totalMinor,
        payerIds = listOf(PIERRE),
        currency = GroupCurrency.BRL,
    )

    private fun row(
        id: String,
        isIncluded: Boolean = true,
        valueText: String = "",
        parts: Long = 0L,
    ): SplitRow = SplitRow(
        person = person(id),
        isIncluded = isIncluded,
        valueText = valueText,
        parts = parts,
        shareMinor = null,
    )

    // Why: this device's user is PIERRE, who also pays, so "Você" is the payer.
    private fun person(id: String): BillPerson = BillPerson(
        id = id,
        name = id,
        initials = "",
        avatarColor = AvatarColor.BLUE,
        isMe = id == PIERRE,
        hasJoined = true,
    )

    private companion object {
        const val ANA = "member-1"
        const val PIERRE = "member-2"
        const val BRUNO = "member-3"
        const val CARLA = "member-4"
    }
}
