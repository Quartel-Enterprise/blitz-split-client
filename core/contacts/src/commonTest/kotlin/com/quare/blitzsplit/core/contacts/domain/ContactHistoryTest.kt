package com.quare.blitzsplit.core.contacts.domain

import com.quare.blitzsplit.core.contacts.bill
import com.quare.blitzsplit.core.contacts.contact
import com.quare.blitzsplit.core.contacts.contactGroup
import com.quare.blitzsplit.core.contacts.domain.model.Contact
import com.quare.blitzsplit.core.contacts.domain.model.ContactHistory
import com.quare.blitzsplit.core.contacts.domain.model.ContactHistoryItem
import com.quare.blitzsplit.core.contacts.group
import com.quare.blitzsplit.core.contacts.me
import com.quare.blitzsplit.core.contacts.meId
import com.quare.blitzsplit.core.contacts.member
import com.quare.blitzsplit.core.contacts.moneyOf
import com.quare.blitzsplit.core.contacts.owes
import com.quare.blitzsplit.core.contacts.payer
import com.quare.blitzsplit.core.contacts.payment
import com.quare.blitzsplit.core.contacts.share
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.model.GroupCurrency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContactHistoryTest {
    @Test
    fun `GIVEN a bill you paid that Carla shares WHEN reading her history THEN shows it with her part on her`() {
        // Given
        val dinner = bill(
            id = "dinner",
            groupId = "floripa",
            payers = listOf(payer(meId("floripa"), 19_600L)),
            shares = listOf(share(meId("floripa"), 9_800L), share("carla-floripa", 9_800L)),
            spentAt = 500L,
            description = "Jantar",
        )
        val money = mapOf("floripa" to moneyOf(listOf(dinner)))

        // When
        val history = ContactHistory.of(carlaIn("floripa"), groups, money)

        // Then
        assertEquals(
            listOf(
                ContactHistoryItem.Bill(
                    id = "dinner",
                    groupName = "Viagem Floripa",
                    currency = GroupCurrency.BRL,
                    at = 500L,
                    description = "Jantar",
                    payerName = "Pierre",
                    isPaidByMe = true,
                    payerCount = 1,
                    totalMinor = 19_600L,
                    effectMinor = 9_800L,
                ),
            ),
            history,
        )
    }

    @Test
    fun `GIVEN a bill Carla paid that you share WHEN reading her history THEN shows your part on you`() {
        // Given
        val dinner = bill(
            id = "dinner",
            groupId = "floripa",
            payers = listOf(payer("carla-floripa", 13_000L)),
            shares = listOf(share(meId("floripa"), 6_500L), share("carla-floripa", 6_500L)),
        )
        val money = mapOf("floripa" to moneyOf(listOf(dinner)))

        // When
        val history = ContactHistory.of(carlaIn("floripa"), groups, money)

        // Then
        val item = assertIs<ContactHistoryItem.Bill>(history.single())
        assertEquals("Carla", item.payerName)
        assertFalse(item.isPaidByMe)
        assertEquals(-6_500L, item.effectMinor)
    }

    @Test
    fun `GIVEN a bill only between Carla and Bruno WHEN reading her history THEN it is not there`() {
        // Given
        val money = mapOf("floripa" to moneyOf(listOf(owes("floripa", "carla-floripa", "bruno-floripa", 5_000L))))

        // When
        val history = ContactHistory.of(carlaIn("floripa"), groups, money)

        // Then
        assertTrue(history.isEmpty())
    }

    @Test
    fun `GIVEN a bill Bruno paid for both of you WHEN reading her history THEN leaves it out`() {
        // Given
        val pizza = bill(
            id = "pizza",
            groupId = "floripa",
            payers = listOf(payer("bruno-floripa", 6_000L)),
            shares = listOf(share(meId("floripa"), 3_000L), share("carla-floripa", 3_000L)),
        )
        val money = mapOf("floripa" to moneyOf(listOf(pizza)))

        // When
        val history = ContactHistory.of(carlaIn("floripa"), groups, money)

        // Then
        assertEquals(emptyList(), history)
    }

    @Test
    fun `GIVEN a bill you and Carla paid together WHEN reading her history THEN names no single payer`() {
        // Given
        val groceries = bill(
            id = "groceries",
            groupId = "floripa",
            payers = listOf(payer(meId("floripa"), 25_000L), payer("carla-floripa", 5_000L)),
            shares = listOf(
                share(meId("floripa"), 10_000L),
                share("carla-floripa", 10_000L),
                share("bruno-floripa", 10_000L),
            ),
        )
        val money = mapOf("floripa" to moneyOf(listOf(groceries)))

        // When
        val history = ContactHistory.of(carlaIn("floripa"), groups, money)

        // Then
        val item = assertIs<ContactHistoryItem.Bill>(history.single())
        assertNull(item.payerName)
        assertFalse(item.isPaidByMe)
        assertEquals(2, item.payerCount)
        assertEquals(30_000L, item.totalMinor)
        assertEquals(5_000L, item.effectMinor)
    }

    @Test
    fun `GIVEN Carla paid you WHEN reading her history THEN shows the payment as received`() {
        // Given
        val money = mapOf(
            "floripa" to moneyOf(
                settlements = listOf(
                    payment(
                        "floripa",
                        "carla-floripa",
                        meId("floripa"),
                        5_000L,
                        id = "pay-1",
                        paidAt = 700L,
                        method = SettlementMethod.CASH,
                    ),
                ),
            ),
        )

        // When
        val history = ContactHistory.of(carlaIn("floripa"), groups, money)

        // Then
        assertEquals(
            listOf(
                ContactHistoryItem.Payment(
                    id = "pay-1",
                    groupName = "Viagem Floripa",
                    currency = GroupCurrency.BRL,
                    at = 700L,
                    isFromMe = false,
                    amountMinor = 5_000L,
                    method = SettlementMethod.CASH,
                ),
            ),
            history,
        )
    }

    @Test
    fun `GIVEN you paid Carla WHEN reading her history THEN shows the payment as yours`() {
        // Given
        val money = mapOf(
            "floripa" to moneyOf(settlements = listOf(payment("floripa", meId("floripa"), "carla-floripa", 2_000L))),
        )

        // When
        val history = ContactHistory.of(carlaIn("floripa"), groups, money)

        // Then
        val item = assertIs<ContactHistoryItem.Payment>(history.single())
        assertTrue(item.isFromMe)
        assertEquals(2_000L, item.amountMinor)
    }

    @Test
    fun `GIVEN Carla paid Bruno WHEN reading her history THEN the payment is not there`() {
        // Given
        val money = mapOf(
            "floripa" to moneyOf(settlements = listOf(payment("floripa", "carla-floripa", "bruno-floripa", 2_000L))),
        )

        // When
        val history = ContactHistory.of(carlaIn("floripa"), groups, money)

        // Then
        assertTrue(history.isEmpty())
    }

    @Test
    fun `GIVEN bills and payments in two groups WHEN reading her history THEN lists them by when they happened`() {
        // Given
        val carla = contact(
            key = "user:carla",
            name = "Carla",
            userId = "carla",
            groups = listOf(
                contactGroup("floripa", "carla-floripa", groupName = "Viagem Floripa"),
                contactGroup(
                    "lisboa",
                    "carla-lisboa",
                    groupName = "Lisboa",
                    currency = GroupCurrency.EUR,
                ),
            ),
        )
        val money = mapOf(
            "floripa" to moneyOf(
                expenses = listOf(
                    owes(
                        "floripa",
                        "carla-floripa",
                        meId("floripa"),
                        1_000L,
                        id = "old",
                        spentAt = 100L,
                        updatedAt = 9L,
                    ),
                    owes(
                        "floripa",
                        "carla-floripa",
                        meId("floripa"),
                        1_000L,
                        id = "new",
                        spentAt = 300L,
                        updatedAt = 1L,
                    ),
                ),
            ),
            "lisboa" to moneyOf(
                settlements = listOf(
                    payment(
                        "lisboa",
                        "carla-lisboa",
                        meId("lisboa"),
                        500L,
                        id = "paid",
                        paidAt = 200L,
                        updatedAt = 99L,
                    ),
                ),
            ),
        )

        // When
        val history = ContactHistory.of(carla, groups, money)

        // Then
        assertEquals(listOf("new", "paid", "old"), history.map { it.id })
        assertEquals(listOf("Viagem Floripa", "Lisboa", "Viagem Floripa"), history.map { it.groupName })
        assertEquals(GroupCurrency.EUR, history[1].currency)
    }

    @Test
    fun `GIVEN a group in common no longer on the device WHEN reading her history THEN skips it`() {
        // Given
        val carla = contact(
            key = "user:carla",
            name = "Carla",
            userId = "carla",
            groups = listOf(contactGroup("gone", "carla-gone")),
        )
        val money = mapOf("gone" to moneyOf(listOf(owes("gone", "carla-gone", meId("gone"), 1_000L))))

        // When
        val history = ContactHistory.of(carla, groups, money)

        // Then
        assertTrue(history.isEmpty())
    }

    private val groups = listOf(
        group(
            "floripa",
            me("floripa"),
            member("carla-floripa", "Carla", userId = "carla"),
            member("bruno-floripa", "Bruno", userId = "bruno"),
            name = "Viagem Floripa",
        ),
        group(
            "lisboa",
            me("lisboa"),
            member("carla-lisboa", "Carla", userId = "carla"),
            name = "Lisboa",
            currency = GroupCurrency.EUR,
        ),
    )

    private fun carlaIn(groupId: String): Contact = contact(
        key = "user:carla",
        name = "Carla",
        userId = "carla",
        groups = listOf(contactGroup(groupId, "carla-$groupId", groupName = "Viagem Floripa")),
    )
}
