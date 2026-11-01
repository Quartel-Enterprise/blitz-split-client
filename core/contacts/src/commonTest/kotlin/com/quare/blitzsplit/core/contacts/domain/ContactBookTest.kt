package com.quare.blitzsplit.core.contacts.domain

import com.quare.blitzsplit.core.contacts.bill
import com.quare.blitzsplit.core.contacts.domain.model.ContactBook
import com.quare.blitzsplit.core.contacts.domain.model.ContactGroup
import com.quare.blitzsplit.core.contacts.domain.model.ContactNet
import com.quare.blitzsplit.core.contacts.domain.model.HiddenContact
import com.quare.blitzsplit.core.contacts.group
import com.quare.blitzsplit.core.contacts.me
import com.quare.blitzsplit.core.contacts.meId
import com.quare.blitzsplit.core.contacts.member
import com.quare.blitzsplit.core.contacts.moneyOf
import com.quare.blitzsplit.core.contacts.owes
import com.quare.blitzsplit.core.contacts.payer
import com.quare.blitzsplit.core.contacts.payment
import com.quare.blitzsplit.core.contacts.share
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContactBookTest {
    @Test
    fun `GIVEN a bill you paid all on Carla WHEN listing the contacts THEN Carla owes you that much`() {
        // Given
        val groups = listOf(group("floripa", me("floripa"), carla("floripa")))
        val money = mapOf("floripa" to moneyOf(listOf(owes("floripa", "carla-floripa", meId("floripa"), 9_840L))))

        // When
        val contacts = ContactBook.of(groups, money, hidden = emptyList())

        // Then
        val contact = contacts.single()
        assertEquals("user:carla", contact.key)
        assertEquals("Carla", contact.name)
        assertEquals("carla", contact.userId)
        assertFalse(contact.isGuest)
        assertEquals(listOf(ContactNet(GroupCurrency.BRL, 9_840L)), contact.nets)
    }

    @Test
    fun `GIVEN a bill Carla paid all on you WHEN listing the contacts THEN you owe Carla that much`() {
        // Given
        val groups = listOf(group("floripa", me("floripa"), carla("floripa")))
        val money = mapOf("floripa" to moneyOf(listOf(owes("floripa", meId("floripa"), "carla-floripa", 9_800L))))

        // When
        val contacts = ContactBook.of(groups, money, hidden = emptyList())

        // Then
        assertEquals(-9_800L, contacts.single().net.amountMinor)
    }

    @Test
    fun `GIVEN only you in a group WHEN listing the contacts THEN you are never your own contact`() {
        // Given
        val groups = listOf(group("solo", me("solo")))

        // When
        val contacts = ContactBook.of(
            groups,
            money = emptyMap(),
            hidden = emptyList(),
        )

        // Then
        assertTrue(contacts.isEmpty())
    }

    @Test
    fun `GIVEN a group you are not a member of WHEN listing the contacts THEN nobody of it is listed`() {
        // Given
        val groups = listOf(group("other", carla("other"), bruno("other")))

        // When
        val contacts = ContactBook.of(
            groups,
            money = emptyMap(),
            hidden = emptyList(),
        )

        // Then
        assertTrue(contacts.isEmpty())
    }

    @Test
    fun `GIVEN Carla owes you in one group and you owe her in another WHEN listing THEN shows her once with the net`() {
        // Given
        val groups = listOf(
            group("floripa", me("floripa"), carla("floripa")),
            group("ape", me("ape"), carla("ape")),
            group("churrasco", me("churrasco"), carla("churrasco")),
        )
        val money = mapOf(
            "floripa" to moneyOf(listOf(owes("floripa", "carla-floripa", meId("floripa"), 9_840L))),
            "ape" to moneyOf(listOf(owes("ape", meId("ape"), "carla-ape", 9_800L))),
            "churrasco" to moneyOf(listOf(owes("churrasco", "carla-churrasco", meId("churrasco"), 3_250L))),
        )

        // When
        val contacts = ContactBook.of(groups, money, hidden = emptyList())

        // Then
        val contact = contacts.single()
        assertEquals(listOf(ContactNet(GroupCurrency.BRL, 3_290L)), contact.nets)
        assertEquals(listOf("floripa", "ape", "churrasco"), contact.groups.map { it.groupId })
        assertEquals(listOf(9_840L, -9_800L, 3_250L), contact.groups.map { it.amountMinor })
    }

    @Test
    fun `GIVEN Carla paid part of what she owes WHEN listing the contacts THEN counts what is still open`() {
        // Given
        val groups = listOf(group("floripa", me("floripa"), carla("floripa")))
        val money = mapOf(
            "floripa" to moneyOf(
                expenses = listOf(owes("floripa", "carla-floripa", meId("floripa"), 9_840L)),
                settlements = listOf(payment("floripa", "carla-floripa", meId("floripa"), 4_000L)),
            ),
        )

        // When
        val contacts = ContactBook.of(groups, money, hidden = emptyList())

        // Then
        assertEquals(5_840L, contacts.single().net.amountMinor)
    }

    @Test
    fun `GIVEN Carla owes Bruno and not you WHEN listing the contacts THEN neither owes you anything`() {
        // Given
        val groups = listOf(group("floripa", me("floripa"), carla("floripa"), bruno("floripa")))
        val money = mapOf("floripa" to moneyOf(listOf(owes("floripa", "carla-floripa", "bruno-floripa", 5_000L))))

        // When
        val contacts = ContactBook.of(groups, money, hidden = emptyList())

        // Then
        assertEquals(listOf(0L, 0L), contacts.map { it.net.amountMinor })
        assertTrue(contacts.all { it.isSettled })
    }

    @Test
    fun `GIVEN names in mixed case and accents WHEN listing the contacts THEN sorts them A to Z ignoring both`() {
        // Given
        val groups = listOf(
            group(
                "ape",
                me("ape"),
                member("m1", "bruno"),
                member("m2", "Carla"),
                member("m3", "Álvaro"),
                member("m4", "ana"),
                member("m5", "Éder"),
            ),
        )

        // When
        val contacts = ContactBook.of(
            groups,
            money = emptyMap(),
            hidden = emptyList(),
        )

        // Then
        assertEquals(listOf("Álvaro", "ana", "bruno", "Carla", "Éder"), contacts.map { it.name })
    }

    @Test
    fun `GIVEN a name nobody joined as in two groups written in different case WHEN listing THEN it is one guest`() {
        // Given
        val diegoHere = member("d1", "Diego")
        val diegoThere = member("d2", " diego ")
        val groups = listOf(
            group("churrasco", me("churrasco"), diegoHere),
            group("futebol", me("futebol"), diegoThere),
        )
        val money = mapOf(
            "churrasco" to moneyOf(listOf(owes("churrasco", diegoHere.id, meId("churrasco"), 4_500L))),
            "futebol" to moneyOf(listOf(owes("futebol", diegoThere.id, meId("futebol"), 1_000L))),
        )

        // When
        val contacts = ContactBook.of(groups, money, hidden = emptyList())

        // Then
        val contact = contacts.single()
        assertEquals("name:diego", contact.key)
        assertEquals("Diego", contact.name)
        assertTrue(contact.isGuest)
        assertNull(contact.userId)
        assertEquals(listOf("d1", "d2"), contact.groups.map { it.memberId })
        assertEquals(5_500L, contact.net.amountMinor)
    }

    @Test
    fun `GIVEN a guest name written with and without accents WHEN listing THEN counts them as Saldos does`() {
        // Given
        val groups = listOf(
            group("churrasco", me("churrasco"), member("j1", "José")),
            group("futebol", me("futebol"), member("j2", "Jose")),
        )

        // When
        val contacts = ContactBook.of(
            groups,
            money = emptyMap(),
            hidden = emptyList(),
        )

        // Then
        assertEquals(listOf("name:jose", "name:josé"), contacts.map { it.key }.sorted())
    }

    @Test
    fun `GIVEN Ana joined in one group and is a guest name in another WHEN listing THEN they are two people`() {
        // Given
        val anaAccount = member("a1", "Ana", userId = "ana")
        val anaGuest = member("a2", "Ana")
        val groups = listOf(
            group("floripa", me("floripa"), anaAccount),
            group("ape", me("ape"), anaGuest),
        )
        val money = mapOf(
            "floripa" to moneyOf(listOf(owes("floripa", anaAccount.id, meId("floripa"), 2_000L))),
            "ape" to moneyOf(listOf(owes("ape", anaGuest.id, meId("ape"), 1_000L))),
        )

        // When
        val contacts = ContactBook.of(groups, money, hidden = emptyList())

        // Then
        val account = contacts.single { it.key == "user:ana" }
        val guest = contacts.single { it.key == "name:ana" }
        assertEquals(2_000L, account.net.amountMinor)
        assertEquals(listOf("floripa"), account.groups.map { it.groupId })
        assertEquals(1_000L, guest.net.amountMinor)
        assertEquals(listOf("ape"), guest.groups.map { it.groupId })
        assertTrue(guest.isGuest)
    }

    @Test
    fun `GIVEN Carla named differently in two groups WHEN listing THEN names her as the latest active group`() {
        // Given
        val groups = listOf(
            group(
                "ape",
                me("ape"),
                member(
                    "carla-ape",
                    "Carlinha",
                    userId = "carla",
                    color = AvatarColor.SAND,
                ),
            ),
            group(
                "floripa",
                me("floripa"),
                member(
                    "carla-floripa",
                    "Carla",
                    userId = "carla",
                    color = AvatarColor.PEACH,
                ),
            ),
        )

        // When
        val contacts = ContactBook.of(
            groups,
            money = emptyMap(),
            hidden = emptyList(),
        )

        // Then
        val contact = contacts.single()
        assertEquals("Carlinha", contact.name)
        assertEquals(AvatarColor.SAND, contact.color)
        assertEquals(listOf("ape", "floripa"), contact.groups.map { it.groupId })
    }

    @Test
    fun `GIVEN a guest with invite codes WHEN listing THEN the group in common carries the personal link codes`() {
        // Given
        val diego = member("diego-ape", "Diego", inviteCode = "dg1234")
        val groups = listOf(
            group(
                "ape",
                me("ape", inviteCode = "me1234"),
                diego,
                name = "Apê 302",
                currency = GroupCurrency.EUR,
                inviteCode = "Code1234",
                color = AvatarColor.PEACH,
            ),
        )
        val money = mapOf("ape" to moneyOf(listOf(owes("ape", diego.id, meId("ape"), 1_500L))))

        // When
        val contacts = ContactBook.of(groups, money, hidden = emptyList())

        // Then
        assertEquals(
            ContactGroup(
                groupId = "ape",
                groupName = "Apê 302",
                groupInitials = Initials.of("Apê 302"),
                groupColor = AvatarColor.PEACH,
                currency = GroupCurrency.EUR,
                memberId = "diego-ape",
                meMemberId = meId("ape"),
                groupInviteCode = "Code1234",
                memberInviteCode = "dg1234",
                meInviteCode = "me1234",
                amountMinor = 1_500L,
            ),
            contacts.single().groups.single(),
        )
    }

    @Test
    fun `GIVEN Carla in groups of two currencies WHEN listing THEN keeps a net per currency never added up`() {
        // Given
        val groups = listOf(
            group("lisboa", me("lisboa"), carla("lisboa"), currency = GroupCurrency.EUR),
            group("floripa", me("floripa"), carla("floripa")),
            group("ape", me("ape"), carla("ape")),
        )
        val money = mapOf(
            "lisboa" to moneyOf(listOf(owes("lisboa", "carla-lisboa", meId("lisboa"), 700L))),
            "floripa" to moneyOf(listOf(owes("floripa", "carla-floripa", meId("floripa"), 2_000L))),
            "ape" to moneyOf(listOf(owes("ape", meId("ape"), "carla-ape", 500L))),
        )

        // When
        val contacts = ContactBook.of(groups, money, hidden = emptyList())

        // Then
        assertEquals(
            listOf(ContactNet(GroupCurrency.BRL, 1_500L), ContactNet(GroupCurrency.EUR, 700L)),
            contacts.single().nets,
        )
    }

    @Test
    fun `GIVEN Carla in more groups of the second currency WHEN listing THEN that currency comes first`() {
        // Given
        val groups = listOf(
            group("floripa", me("floripa"), carla("floripa")),
            group("lisboa", me("lisboa"), carla("lisboa"), currency = GroupCurrency.EUR),
            group("porto", me("porto"), carla("porto"), currency = GroupCurrency.EUR),
        )

        // When
        val contacts = ContactBook.of(
            groups,
            money = emptyMap(),
            hidden = emptyList(),
        )

        // Then
        assertEquals(listOf(GroupCurrency.EUR, GroupCurrency.BRL), contacts.single().nets.map { it.currency })
    }

    @Test
    fun `GIVEN Carla in one group of each of two currencies WHEN listing THEN orders them by currency code`() {
        // Given
        val groups = listOf(
            group("lisboa", me("lisboa"), carla("lisboa"), currency = GroupCurrency.EUR),
            group("floripa", me("floripa"), carla("floripa")),
        )

        // When
        val contacts = ContactBook.of(
            groups,
            money = emptyMap(),
            hidden = emptyList(),
        )

        // Then
        assertEquals(listOf(GroupCurrency.BRL, GroupCurrency.EUR), contacts.single().nets.map { it.currency })
    }

    @Test
    fun `GIVEN a debt with someone no longer in the group WHEN listing THEN that group does not list them`() {
        // Given
        val groups = listOf(
            group("floripa", me("floripa"), carla("floripa")),
            group("ape", me("ape"), bruno("ape")),
        )
        val money = mapOf(
            "floripa" to moneyOf(listOf(owes("floripa", "bruno-floripa", meId("floripa"), 3_000L))),
        )

        // When
        val contacts = ContactBook.of(groups, money, hidden = emptyList())

        // Then
        val bruno = contacts.single { it.key == "user:bruno" }
        assertEquals(listOf("ape"), bruno.groups.map { it.groupId })
        assertEquals(0L, bruno.net.amountMinor)
        assertEquals(listOf("user:bruno", "user:carla"), contacts.map { it.key })
    }

    @Test
    fun `GIVEN Carla hidden and settled with no bill since WHEN listing THEN she is listed as hidden`() {
        // Given
        val groups = listOf(group("floripa", me("floripa"), carla("floripa")))
        val money = mapOf(
            "floripa" to moneyOf(
                expenses = listOf(owes("floripa", "carla-floripa", meId("floripa"), 5_000L, updatedAt = 100L)),
                settlements = listOf(payment("floripa", "carla-floripa", meId("floripa"), 5_000L, updatedAt = 150L)),
            ),
        )
        val hidden = listOf(
            HiddenContact(
                "user:carla",
                isHidden = true,
                hiddenAt = 200L,
            ),
        )

        // When
        val contacts = ContactBook.of(groups, money, hidden)

        // Then
        val contact = contacts.single()
        assertTrue(contact.isSettled)
        assertTrue(contact.isHidden)
    }

    @Test
    fun `GIVEN Carla hidden with no bill ever WHEN listing THEN she is listed as hidden`() {
        // Given
        val groups = listOf(group("floripa", me("floripa"), carla("floripa")))
        val hidden = listOf(
            HiddenContact(
                "user:carla",
                isHidden = true,
                hiddenAt = 200L,
            ),
        )

        // When
        val contacts = ContactBook.of(
            groups,
            money = emptyMap(),
            hidden = hidden,
        )

        // Then
        assertTrue(contacts.single().isHidden)
    }

    @Test
    fun `GIVEN Carla shown again after being hidden WHEN listing THEN she is not hidden`() {
        // Given
        val groups = listOf(group("floripa", me("floripa"), carla("floripa")))
        val hidden = listOf(
            HiddenContact(
                "user:carla",
                isHidden = false,
                hiddenAt = 200L,
            ),
        )

        // When
        val contacts = ContactBook.of(
            groups,
            money = emptyMap(),
            hidden = hidden,
        )

        // Then
        assertFalse(contacts.single().isHidden)
    }

    @Test
    fun `GIVEN someone else hidden WHEN listing THEN Carla is not hidden`() {
        // Given
        val groups = listOf(group("floripa", me("floripa"), carla("floripa")))
        val hidden = listOf(
            HiddenContact(
                "user:bruno",
                isHidden = true,
                hiddenAt = 200L,
            ),
        )

        // When
        val contacts = ContactBook.of(
            groups,
            money = emptyMap(),
            hidden = hidden,
        )

        // Then
        assertFalse(contacts.single().isHidden)
    }

    @Test
    fun `GIVEN Carla hidden whose balance is open again WHEN listing THEN she comes back by herself`() {
        // Given
        val groups = listOf(group("floripa", me("floripa"), carla("floripa")))
        val money = mapOf(
            "floripa" to moneyOf(
                listOf(owes("floripa", "carla-floripa", meId("floripa"), 5_000L, updatedAt = 100L)),
            ),
        )
        val hidden = listOf(
            HiddenContact(
                "user:carla",
                isHidden = true,
                hiddenAt = 200L,
            ),
        )

        // When
        val contacts = ContactBook.of(groups, money, hidden)

        // Then
        assertFalse(contacts.single().isHidden)
    }

    @Test
    fun `GIVEN Carla hidden and open only in another currency WHEN listing THEN she comes back by herself`() {
        // Given
        val groups = listOf(
            group("floripa", me("floripa"), carla("floripa")),
            group("lisboa", me("lisboa"), carla("lisboa"), currency = GroupCurrency.EUR),
        )
        val money = mapOf("lisboa" to moneyOf(listOf(owes("lisboa", "carla-lisboa", meId("lisboa"), 700L))))
        val hidden = listOf(
            HiddenContact(
                "user:carla",
                isHidden = true,
                hiddenAt = 200L,
            ),
        )

        // When
        val contacts = ContactBook.of(groups, money, hidden)

        // Then
        assertFalse(contacts.single().isHidden)
    }

    @Test
    fun `GIVEN Carla hidden and a bill with her saved since and settled WHEN listing THEN she comes back by herself`() {
        // Given
        val groups = listOf(group("floripa", me("floripa"), carla("floripa")))
        val money = mapOf(
            "floripa" to moneyOf(
                expenses = listOf(owes("floripa", "carla-floripa", meId("floripa"), 5_000L, updatedAt = 300L)),
                settlements = listOf(payment("floripa", "carla-floripa", meId("floripa"), 5_000L, updatedAt = 310L)),
            ),
        )
        val hidden = listOf(
            HiddenContact(
                "user:carla",
                isHidden = true,
                hiddenAt = 200L,
            ),
        )

        // When
        val contacts = ContactBook.of(groups, money, hidden)

        // Then
        val contact = contacts.single()
        assertTrue(contact.isSettled)
        assertFalse(contact.isHidden)
    }

    @Test
    fun `GIVEN Carla hidden and a bill Bruno paid for both of you saved since WHEN listing THEN she comes back`() {
        // Given
        val groups = listOf(group("floripa", me("floripa"), carla("floripa"), bruno("floripa")))
        val sharedByBoth = bill(
            id = "pizza",
            groupId = "floripa",
            payers = listOf(payer("bruno-floripa", 6_000L)),
            shares = listOf(share(meId("floripa"), 3_000L), share("carla-floripa", 3_000L)),
            updatedAt = 300L,
        )
        val money = mapOf("floripa" to moneyOf(listOf(sharedByBoth)))
        val hidden = listOf(
            HiddenContact(
                "user:carla",
                isHidden = true,
                hiddenAt = 200L,
            ),
        )

        // When
        val contacts = ContactBook.of(groups, money, hidden)

        // Then
        val carla = contacts.single { it.key == "user:carla" }
        assertTrue(carla.isSettled)
        assertFalse(carla.isHidden)
    }

    @Test
    fun `GIVEN Carla hidden and a bill saved since only between her and Bruno WHEN listing THEN she stays hidden`() {
        // Given
        val groups = listOf(group("floripa", me("floripa"), carla("floripa"), bruno("floripa")))
        val money = mapOf(
            "floripa" to moneyOf(
                expenses = listOf(owes("floripa", "carla-floripa", "bruno-floripa", 5_000L, updatedAt = 300L)),
            ),
        )
        val hidden = listOf(
            HiddenContact(
                "user:carla",
                isHidden = true,
                hiddenAt = 200L,
            ),
        )

        // When
        val contacts = ContactBook.of(groups, money, hidden)

        // Then
        assertTrue(contacts.single { it.key == "user:carla" }.isHidden)
    }

    @Test
    fun `GIVEN Carla hidden at the very moment a settled bill with her was saved WHEN listing THEN she stays hidden`() {
        // Given
        val groups = listOf(group("floripa", me("floripa"), carla("floripa")))
        val money = mapOf(
            "floripa" to moneyOf(
                expenses = listOf(owes("floripa", "carla-floripa", meId("floripa"), 5_000L, updatedAt = 200L)),
                settlements = listOf(payment("floripa", "carla-floripa", meId("floripa"), 5_000L, updatedAt = 150L)),
            ),
        )
        val hidden = listOf(
            HiddenContact(
                "user:carla",
                isHidden = true,
                hiddenAt = 200L,
            ),
        )

        // When
        val contacts = ContactBook.of(groups, money, hidden)

        // Then
        assertTrue(contacts.single().isHidden)
    }

    private fun carla(groupId: String): GroupMember = member("carla-$groupId", "Carla", userId = "carla")

    private fun bruno(groupId: String): GroupMember = member("bruno-$groupId", "Bruno", userId = "bruno")
}
