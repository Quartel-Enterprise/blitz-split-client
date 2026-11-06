package com.quare.blitzsplit.core.contacts.domain

import com.quare.blitzsplit.core.contacts.domain.model.ContactBook
import com.quare.blitzsplit.core.contacts.domain.model.ContactNet
import com.quare.blitzsplit.core.contacts.domain.model.HiddenContact
import com.quare.blitzsplit.core.contacts.group
import com.quare.blitzsplit.core.contacts.me
import com.quare.blitzsplit.core.contacts.meId
import com.quare.blitzsplit.core.contacts.member
import com.quare.blitzsplit.core.contacts.moneyOf
import com.quare.blitzsplit.core.contacts.owes
import com.quare.blitzsplit.core.contacts.payment
import com.quare.blitzsplit.core.model.GroupCurrency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContactBookCurrenciesTest {
    private val carla = member("carla-floripa", "Carla", userId = "carla")

    @Test
    fun `GIVEN one group with reais and dollar bills WHEN listing the contacts THEN Carla has a net per currency`() {
        // Given
        val groups = listOf(group("floripa", me("floripa"), carla))
        val money = mapOf(
            "floripa" to moneyOf(
                listOf(
                    owes("floripa", carla.id, meId("floripa"), 1_000L, id = "b1"),
                    owes(
                        "floripa",
                        carla.id,
                        meId("floripa"),
                        500L,
                        id = "b2",
                        currency = GroupCurrency.USD,
                    ),
                ),
            ),
        )

        // When
        val contact = ContactBook.of(groups, money, hidden = emptyList()).single()

        // Then
        assertEquals(
            listOf(ContactNet(GroupCurrency.BRL, 1_000L), ContactNet(GroupCurrency.USD, 500L)),
            contact.nets,
        )
        assertEquals(listOf(GroupCurrency.BRL, GroupCurrency.USD), contact.groups.map { it.currency })
        assertEquals(listOf(1_000L, 500L), contact.groups.map { it.amountMinor })
        assertFalse(contact.isSettled)
    }

    @Test
    fun `GIVEN a debt in reais and a credit in dollars WHEN listing the contacts THEN never nets them`() {
        // Given
        val groups = listOf(group("floripa", me("floripa"), carla))
        val money = mapOf(
            "floripa" to moneyOf(
                listOf(
                    owes("floripa", carla.id, meId("floripa"), 1_000L, id = "b1"),
                    owes(
                        "floripa",
                        meId("floripa"),
                        carla.id,
                        1_000L,
                        id = "b2",
                        currency = GroupCurrency.USD,
                    ),
                ),
            ),
        )

        // When
        val contact = ContactBook.of(groups, money, hidden = emptyList()).single()

        // Then
        assertEquals(
            listOf(ContactNet(GroupCurrency.BRL, 1_000L), ContactNet(GroupCurrency.USD, -1_000L)),
            contact.nets,
        )
        assertFalse(contact.isSettled)
    }

    @Test
    fun `GIVEN bills all in other currencies WHEN listing the contacts THEN includes the group currency at zero`() {
        // Given
        val groups = listOf(group("lisboa", me("lisboa"), carla, currency = GroupCurrency.EUR))
        val money = mapOf(
            "lisboa" to moneyOf(
                listOf(
                    owes(
                        "lisboa",
                        carla.id,
                        meId("lisboa"),
                        1_000L,
                        id = "b1",
                        currency = GroupCurrency.BRL,
                    ),
                    owes(
                        "lisboa",
                        carla.id,
                        meId("lisboa"),
                        300L,
                        id = "b2",
                        currency = GroupCurrency.USD,
                    ),
                ),
            ),
        )

        // When
        val contact = ContactBook.of(groups, money, hidden = emptyList()).single()

        // Then
        assertEquals(
            listOf(
                ContactNet(GroupCurrency.BRL, 1_000L),
                ContactNet(GroupCurrency.EUR, 0L),
                ContactNet(GroupCurrency.USD, 300L),
            ),
            contact.nets,
        )
        assertEquals(GroupCurrency.BRL, contact.net.currency)
    }

    @Test
    fun `GIVEN a debt in reais WHEN Carla pays the same amount in euros THEN the debt in reais stays open`() {
        // Given
        val groups = listOf(group("floripa", me("floripa"), carla))
        val money = mapOf(
            "floripa" to moneyOf(
                expenses = listOf(owes("floripa", carla.id, meId("floripa"), 1_000L)),
                settlements = listOf(
                    payment("floripa", carla.id, meId("floripa"), 1_000L, currency = GroupCurrency.EUR),
                ),
            ),
        )

        // When
        val contact = ContactBook.of(groups, money, hidden = emptyList()).single()

        // Then
        assertEquals(
            listOf(ContactNet(GroupCurrency.BRL, 1_000L), ContactNet(GroupCurrency.EUR, -1_000L)),
            contact.nets,
        )
    }

    @Test
    fun `GIVEN debts each paid back in their currency WHEN listing the contacts THEN Carla is settled`() {
        // Given
        val groups = listOf(group("floripa", me("floripa"), carla))
        val money = mapOf(
            "floripa" to moneyOf(
                expenses = listOf(
                    owes("floripa", carla.id, meId("floripa"), 1_000L, id = "b1"),
                    owes(
                        "floripa",
                        carla.id,
                        meId("floripa"),
                        500L,
                        id = "b2",
                        currency = GroupCurrency.USD,
                    ),
                ),
                settlements = listOf(
                    payment("floripa", carla.id, meId("floripa"), 1_000L, id = "p1"),
                    payment(
                        "floripa",
                        carla.id,
                        meId("floripa"),
                        500L,
                        id = "p2",
                        currency = GroupCurrency.USD,
                    ),
                ),
            ),
        )

        // When
        val contact = ContactBook.of(groups, money, hidden = emptyList()).single()

        // Then
        assertEquals(
            listOf(ContactNet(GroupCurrency.BRL, 0L), ContactNet(GroupCurrency.USD, 0L)),
            contact.nets,
        )
        assertTrue(contact.isSettled)
    }

    @Test
    fun `GIVEN reais settled but dollars open WHEN the contact was hidden THEN it comes back`() {
        // Given
        val groups = listOf(group("floripa", me("floripa"), carla))
        val money = mapOf(
            "floripa" to moneyOf(
                expenses = listOf(
                    owes("floripa", carla.id, meId("floripa"), 1_000L, id = "b1"),
                    owes(
                        "floripa",
                        carla.id,
                        meId("floripa"),
                        500L,
                        id = "b2",
                        currency = GroupCurrency.USD,
                    ),
                ),
                settlements = listOf(payment("floripa", carla.id, meId("floripa"), 1_000L)),
            ),
        )
        val hidden = listOf(
            HiddenContact(
                "user:carla",
                isHidden = true,
                hiddenAt = 10L,
            ),
        )

        // When
        val contact = ContactBook.of(groups, money, hidden).single()

        // Then
        assertEquals(listOf(ContactNet(GroupCurrency.BRL, 0L), ContactNet(GroupCurrency.USD, 500L)), contact.nets)
        assertFalse(contact.isHidden)
    }
}
