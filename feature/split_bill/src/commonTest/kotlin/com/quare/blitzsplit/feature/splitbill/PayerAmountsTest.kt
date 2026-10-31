package com.quare.blitzsplit.feature.splitbill

import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillPerson
import com.quare.blitzsplit.feature.splitbill.presentation.model.PayerAmountRow
import com.quare.blitzsplit.feature.splitbill.presentation.model.PayerAmounts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Why: "Várias pessoas pagaram", what it derives from the amounts typed in it.
class PayerAmountsTest {
    @Test
    fun `GIVEN amounts typed for some WHEN reading who paid THEN lists only those with an amount in row order`() {
        // Given
        val amounts = amounts(ANA to "200,00", PIERRE to "", BRUNO to "0", CARLA to "112,00")

        // When
        val payers = amounts.payers

        // Then
        assertEquals(listOf(ExpensePayer(ANA, 20_000L), ExpensePayer(CARLA, 11_200L)), payers)
        assertEquals(listOf(ANA, CARLA), amounts.paying.map { it.memberId })
    }

    @Test
    fun `GIVEN amounts written with a point and with a comma WHEN reading who paid THEN reads both in cents`() {
        // Given
        val amounts = amounts(ANA to "200.50", PIERRE to "111,50")

        // When
        val payers = amounts.payers

        // Then
        assertEquals(listOf(ExpensePayer(ANA, 20_050L), ExpensePayer(PIERRE, 11_150L)), payers)
    }

    @Test
    fun `GIVEN amounts adding up to the total WHEN reading them THEN they close with nothing missing`() {
        // Given
        val amounts = amounts(ANA to "200,00", PIERRE to "112,00")

        // When
        val addsUp = amounts.addsUp

        // Then
        assertTrue(addsUp)
        assertEquals(31_200L, amounts.paidMinor)
        assertEquals(0L, amounts.remainingMinor)
        assertFalse(amounts.showsMismatch)
    }

    @Test
    fun `GIVEN amounts under the total WHEN reading them THEN shows what is missing`() {
        // Given
        val amounts = amounts(ANA to "200,00")

        // When
        val remaining = amounts.remainingMinor

        // Then
        assertEquals(11_200L, remaining)
        assertEquals(20_000L, amounts.paidMinor)
        assertTrue(amounts.showsMismatch)
        assertFalse(amounts.addsUp)
    }

    @Test
    fun `GIVEN amounts over the total WHEN reading them THEN shows how much is over`() {
        // Given
        val amounts = amounts(ANA to "200,00", PIERRE to "124,00")

        // When
        val remaining = amounts.remainingMinor

        // Then
        assertEquals(-1_200L, remaining)
        assertTrue(amounts.showsMismatch)
        assertFalse(amounts.addsUp)
    }

    @Test
    fun `GIVEN everyone blank WHEN reading them THEN nobody paid and it does not close`() {
        // Given
        val amounts = amounts(ANA to "", PIERRE to "")

        // When
        val addsUp = amounts.addsUp

        // Then
        assertFalse(addsUp)
        assertTrue(amounts.payers.isEmpty())
        assertTrue(amounts.paying.isEmpty())
        assertTrue(amounts.showsMismatch)
    }

    @Test
    fun `GIVEN no valid total WHEN reading them THEN shows no mismatch and does not close`() {
        // Given
        val amounts = amounts(ANA to "200,00", totalMinor = 0L)

        // When
        val addsUp = amounts.addsUp

        // Then
        assertFalse(addsUp)
        assertFalse(amounts.showsMismatch)
    }

    @Test
    fun `GIVEN no valid total and nobody paid WHEN reading them THEN does not close`() {
        // Given
        val amounts = amounts(ANA to "", totalMinor = 0L)

        // When
        val addsUp = amounts.addsUp

        // Then
        assertFalse(addsUp)
        assertFalse(amounts.showsMismatch)
    }

    @Test
    fun `GIVEN a group in yen WHEN reading the amounts THEN reads them without decimals`() {
        // Given
        val amounts = amounts(
            ANA to "2000",
            PIERRE to "1.120",
            totalMinor = 3_120L,
            currency = GroupCurrency.JPY,
        )

        // When
        val payers = amounts.payers

        // Then
        assertEquals(listOf(ExpensePayer(ANA, 2_000L), ExpensePayer(PIERRE, 1_120L)), payers)
        assertTrue(amounts.addsUp)
    }

    @Test
    fun `GIVEN someone who left the group paid part WHEN reading who paid THEN they still count`() {
        // Given
        val amounts = PayerAmounts(
            rows = listOf(
                PayerAmountRow(ANA, person(ANA), "200,00"),
                PayerAmountRow(
                    FORMER,
                    person = null,
                    amountText = "112,00",
                ),
            ),
            totalMinor = 31_200L,
            currency = GroupCurrency.BRL,
        )

        // When
        val payers = amounts.payers

        // Then
        assertEquals(listOf(ExpensePayer(ANA, 20_000L), ExpensePayer(FORMER, 11_200L)), payers)
        assertTrue(amounts.addsUp)
    }

    private fun person(id: String): BillPerson = BillPerson(
        id = id,
        name = id,
        initials = "",
        avatarColor = AvatarColor.BLUE,
        isMe = false,
        hasJoined = true,
    )

    // Why: "Várias pessoas pagaram" of an R$ 312,00 bill, one row per member typed as given.
    private fun amounts(
        vararg typed: Pair<String, String>,
        totalMinor: Long = 31_200L,
        currency: GroupCurrency = GroupCurrency.BRL,
    ): PayerAmounts = PayerAmounts(
        rows = typed.map { (memberId, text) -> PayerAmountRow(memberId, person(memberId), text) },
        totalMinor = totalMinor,
        currency = currency,
    )

    private companion object {
        const val ANA = "member-1"
        const val PIERRE = "member-2"
        const val BRUNO = "member-3"
        const val CARLA = "member-4"
        const val FORMER = "member-9"
    }
}
