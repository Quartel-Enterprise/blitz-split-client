package com.quare.blitzsplit.feature.creategroup

import com.quare.blitzsplit.core.contacts.domain.model.Contact
import com.quare.blitzsplit.core.contacts.domain.model.ContactNet
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.creategroup.presentation.model.ContactSuggestions
import com.quare.blitzsplit.feature.creategroup.presentation.model.CreateGroupSuggestion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContactSuggestionsTest {
    @Test
    fun `GIVEN contacts WHEN nothing is typed THEN suggests nobody`() {
        // Given
        val contacts = listOf(contact("Ana Paula"), contact("Bruno"))

        // When
        val suggestions = ContactSuggestions.of(
            contacts,
            draft = "",
            addedKeys = emptySet(),
        )

        // Then
        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun `GIVEN contacts WHEN only spaces are typed THEN suggests nobody`() {
        // Given
        val contacts = listOf(contact("Ana Paula"), contact("Bruno"))

        // When
        val suggestions = ContactSuggestions.of(
            contacts,
            draft = "   ",
            addedKeys = emptySet(),
        )

        // Then
        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun `GIVEN Ana Paula WHEN typing the start of her name THEN suggests her`() {
        // Given
        val contacts = listOf(contact("Ana Paula"))

        // When
        val suggestions = ContactSuggestions.of(
            contacts,
            draft = "ana",
            addedKeys = emptySet(),
        )

        // Then
        assertEquals(listOf("Ana Paula"), suggestions.map { it.name })
    }

    @Test
    fun `GIVEN Maria Ana WHEN typing the start of her second name THEN suggests her`() {
        // Given
        val contacts = listOf(contact("Maria Ana"))

        // When
        val suggestions = ContactSuggestions.of(
            contacts,
            draft = "ana",
            addedKeys = emptySet(),
        )

        // Then
        assertEquals(listOf("Maria Ana"), suggestions.map { it.name })
    }

    @Test
    fun `GIVEN Mariana WHEN typing letters from the middle of her name THEN suggests nobody`() {
        // Given
        val contacts = listOf(contact("Mariana"))

        // When
        val suggestions = ContactSuggestions.of(
            contacts,
            draft = "ana",
            addedKeys = emptySet(),
        )

        // Then
        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun `GIVEN Jose written with an accent WHEN typing jose without it THEN suggests him`() {
        // Given
        val contacts = listOf(contact("José"))

        // When
        val suggestions = ContactSuggestions.of(
            contacts,
            draft = "jose",
            addedKeys = emptySet(),
        )

        // Then
        assertEquals(listOf("José"), suggestions.map { it.name })
    }

    @Test
    fun `GIVEN Jose written without an accent WHEN typing it in capitals with one THEN suggests him`() {
        // Given
        val contacts = listOf(contact("Jose"))

        // When
        val suggestions = ContactSuggestions.of(
            contacts,
            draft = "JOSÉ",
            addedKeys = emptySet(),
        )

        // Then
        assertEquals(listOf("Jose"), suggestions.map { it.name })
    }

    @Test
    fun `GIVEN Ana Paula WHEN typing her names with extra spaces THEN suggests her`() {
        // Given
        val contacts = listOf(contact("Ana Paula"), contact("Ana Bia"))

        // When
        val suggestions = ContactSuggestions.of(
            contacts,
            draft = "  ana   p",
            addedKeys = emptySet(),
        )

        // Then
        assertEquals(listOf("Ana Paula"), suggestions.map { it.name })
    }

    @Test
    fun `GIVEN Maria Ana Souza WHEN typing her last two names THEN suggests her`() {
        // Given
        val contacts = listOf(contact("Maria Ana Souza"))

        // When
        val suggestions = ContactSuggestions.of(
            contacts,
            draft = "ana so",
            addedKeys = emptySet(),
        )

        // Then
        assertEquals(listOf("Maria Ana Souza"), suggestions.map { it.name })
    }

    @Test
    fun `GIVEN contacts from A to Z WHEN typing ana THEN names starting with it come first each group from A to Z`() {
        // Given
        val contacts = listOf(
            contact("Ana Paula"),
            contact("Anabela"),
            contact("Bia Ana"),
            contact("Carla"),
            contact("Maria Ana"),
        )

        // When
        val suggestions = ContactSuggestions.of(
            contacts,
            draft = "ana",
            addedKeys = emptySet(),
        )

        // Then
        assertEquals(listOf("Ana Paula", "Anabela", "Bia Ana", "Maria Ana"), suggestions.map { it.name })
    }

    @Test
    fun `GIVEN seven matching contacts WHEN typing THEN suggests the first five`() {
        // Given
        val contacts = listOf("Ana A", "Ana B", "Ana C", "Ana D", "Ana E", "Ana F", "Ana G").map(::contact)

        // When
        val suggestions = ContactSuggestions.of(
            contacts,
            draft = "ana",
            addedKeys = emptySet(),
        )

        // Then
        assertEquals(listOf("Ana A", "Ana B", "Ana C", "Ana D", "Ana E"), suggestions.map { it.name })
    }

    @Test
    fun `GIVEN word matches listed before a name match WHEN typing THEN the name match comes first`() {
        // Given
        val contacts = listOf("Bia Ana", "Caio Ana", "Davi Ana", "Edu Ana", "Fabi Ana", "Gil Ana", "Lia Ana", "Zana")
            .map(::contact) + contact("Ana Zulu")

        // When
        val suggestions = ContactSuggestions.of(
            contacts,
            draft = "ana",
            addedKeys = emptySet(),
        )

        // Then
        assertEquals(listOf("Ana Zulu", "Bia Ana", "Caio Ana", "Davi Ana", "Edu Ana"), suggestions.map { it.name })
    }

    @Test
    fun `GIVEN a hidden contact WHEN typing her name THEN suggests nobody`() {
        // Given
        val contacts = listOf(contact("Ana Paula", isHidden = true))

        // When
        val suggestions = ContactSuggestions.of(
            contacts,
            draft = "ana",
            addedKeys = emptySet(),
        )

        // Then
        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun `GIVEN Ana already added WHEN typing her name THEN suggests only the others`() {
        // Given
        val contacts = listOf(contact("Ana Paula", key = "user:u-ana"), contact("Ana Bia", key = "name:ana bia"))

        // When
        val suggestions = ContactSuggestions.of(
            contacts,
            draft = "ana",
            addedKeys = setOf("user:u-ana"),
        )

        // Then
        assertEquals(listOf("Ana Bia"), suggestions.map { it.name })
    }

    @Test
    fun `GIVEN a contact named with a single letter WHEN typing it THEN suggests nobody`() {
        // Given
        val contacts = listOf(contact("A"))

        // When
        val suggestions = ContactSuggestions.of(
            contacts,
            draft = "a",
            addedKeys = emptySet(),
        )

        // Then
        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun `GIVEN Ana Paula with a photo WHEN suggesting her THEN carries her key initials color and photo`() {
        // Given
        val contacts = listOf(
            contact(
                name = "Ana Paula",
                key = "user:u-ana",
                color = AvatarColor.GREEN,
                photoUrl = "https://cdn.example/ana.jpg",
            ),
        )

        // When
        val suggestions = ContactSuggestions.of(
            contacts,
            draft = "ana",
            addedKeys = emptySet(),
        )

        // Then
        assertEquals(
            listOf(
                CreateGroupSuggestion(
                    key = "user:u-ana",
                    name = "Ana Paula",
                    initials = "AP",
                    avatarColor = AvatarColor.GREEN,
                    photoUrl = "https://cdn.example/ana.jpg",
                ),
            ),
            suggestions,
        )
    }

    private fun contact(
        name: String,
        key: String = "name:${name.lowercase()}",
        color: AvatarColor = AvatarColor.BLUE,
        photoUrl: String? = null,
        isHidden: Boolean = false,
    ): Contact = Contact(
        key = key,
        name = name,
        color = color,
        photoUrl = photoUrl,
        userId = null,
        groups = emptyList(),
        nets = listOf(ContactNet(GroupCurrency.BRL, 0L)),
        isHidden = isHidden,
    )
}
