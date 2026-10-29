package com.quare.blitzsplit.core.groups.domain

import com.quare.blitzsplit.core.groups.domain.model.MemberNameCheck
import com.quare.blitzsplit.core.groups.domain.usecase.impl.CheckMemberNameUseCase
import kotlin.test.Test
import kotlin.test.assertEquals

class CheckMemberNameUseCaseTest {
    private val useCase = CheckMemberNameUseCase()

    @Test
    fun `GIVEN an empty draft WHEN checking the name THEN it is blank`() {
        // Given
        val name = ""

        // When
        val check = useCase(
            name,
            takenNames = listOf("Pierre"),
            creatorName = "Pierre",
        )

        // Then
        assertEquals(MemberNameCheck.Blank, check)
    }

    @Test
    fun `GIVEN a draft of only spaces WHEN checking the name THEN it is blank`() {
        // Given
        val name = "   "

        // When
        val check = useCase(
            name,
            takenNames = listOf("Pierre"),
            creatorName = "Pierre",
        )

        // Then
        assertEquals(MemberNameCheck.Blank, check)
    }

    @Test
    fun `GIVEN a name no member has WHEN checking the name THEN it is available`() {
        // Given
        val name = "Ana"

        // When
        val check = useCase(
            name,
            takenNames = listOf("Pierre", "Bruno"),
            creatorName = "Pierre",
        )

        // Then
        assertEquals(MemberNameCheck.Available("Ana"), check)
    }

    @Test
    fun `GIVEN extra spaces in a new name WHEN checking it THEN it is available trimmed with single spaces`() {
        // Given
        val name = "  Ana    Paula "

        // When
        val check = useCase(
            name,
            takenNames = listOf("Pierre"),
            creatorName = "Pierre",
        )

        // Then
        assertEquals(MemberNameCheck.Available("Ana Paula"), check)
    }

    @Test
    fun `GIVEN an empty group WHEN checking a name THEN it is available`() {
        // Given
        val name = "Ana"

        // When
        val check = useCase(
            name,
            takenNames = emptyList(),
            creatorName = "Pierre",
        )

        // Then
        assertEquals(MemberNameCheck.Available("Ana"), check)
    }

    @Test
    fun `GIVEN a name already in the group WHEN checking it THEN it is taken with both nicknames`() {
        // Given
        val name = "Ana"

        // When
        val check = useCase(
            name,
            takenNames = listOf("Pierre", "Ana"),
            creatorName = "Pierre",
        )

        // Then
        assertEquals(
            MemberNameCheck.Taken(
                existingName = "Ana",
                suggestions = listOf("Ana P.", "Ana 2"),
            ),
            check,
        )
    }

    @Test
    fun `GIVEN the name in another case WHEN checking it THEN it is taken as the group spells it`() {
        // Given
        val name = "ana"

        // When
        val check = useCase(
            name,
            takenNames = listOf("Pierre", "Ana"),
            creatorName = "Pierre",
        )

        // Then
        assertEquals(
            MemberNameCheck.Taken(
                existingName = "Ana",
                suggestions = listOf("Ana P.", "Ana 2"),
            ),
            check,
        )
    }

    @Test
    fun `GIVEN the name with extra spaces WHEN checking it THEN it is taken as the group spells it`() {
        // Given
        val name = " Ana  "

        // When
        val check = useCase(
            name,
            takenNames = listOf("Pierre", "Ana"),
            creatorName = "Pierre",
        )

        // Then
        assertEquals(
            MemberNameCheck.Taken(
                existingName = "Ana",
                suggestions = listOf("Ana P.", "Ana 2"),
            ),
            check,
        )
    }

    @Test
    fun `GIVEN a name of two words with other spacing and case WHEN checking it THEN it is taken`() {
        // Given
        val name = "ana   PAULA"

        // When
        val check = useCase(
            name,
            takenNames = listOf("Pierre", "Ana Paula"),
            creatorName = "Pierre",
        )

        // Then
        assertEquals(
            MemberNameCheck.Taken(
                existingName = "Ana Paula",
                suggestions = listOf("Ana Paula P.", "Ana Paula 2"),
            ),
            check,
        )
    }

    @Test
    fun `GIVEN Ana 2 already in the group WHEN checking Ana THEN suggests Ana 3`() {
        // Given
        val name = "Ana"

        // When
        val check = useCase(
            name,
            takenNames = listOf("Pierre", "Ana", "Ana 2"),
            creatorName = "Pierre",
        )

        // Then
        assertEquals(
            MemberNameCheck.Taken(
                existingName = "Ana",
                suggestions = listOf("Ana P.", "Ana 3"),
            ),
            check,
        )
    }

    @Test
    fun `GIVEN Ana 2 and Ana 3 already in the group WHEN checking Ana THEN suggests Ana 4`() {
        // Given
        val name = "Ana"

        // When
        val check = useCase(
            name,
            takenNames = listOf("Pierre", "Ana", "Ana 3", "Ana 2"),
            creatorName = "Pierre",
        )

        // Then
        assertEquals(
            MemberNameCheck.Taken(
                existingName = "Ana",
                suggestions = listOf("Ana P.", "Ana 4"),
            ),
            check,
        )
    }

    @Test
    fun `GIVEN a numbered nickname taken in another case WHEN checking the name THEN skips that number`() {
        // Given
        val name = "Ana"

        // When
        val check = useCase(
            name,
            takenNames = listOf("Pierre", "Ana", "ana  2"),
            creatorName = "Pierre",
        )

        // Then
        assertEquals(
            MemberNameCheck.Taken(
                existingName = "Ana",
                suggestions = listOf("Ana P.", "Ana 3"),
            ),
            check,
        )
    }

    @Test
    fun `GIVEN the initial nickname already in the group WHEN checking the name THEN leaves it out`() {
        // Given
        val name = "Ana"

        // When
        val check = useCase(
            name,
            takenNames = listOf("Pierre", "Ana", "Ana P."),
            creatorName = "Pierre",
        )

        // Then
        assertEquals(
            MemberNameCheck.Taken(
                existingName = "Ana",
                suggestions = listOf("Ana 2"),
            ),
            check,
        )
    }

    @Test
    fun `GIVEN the initial nickname taken in lower case WHEN checking the name THEN leaves it out`() {
        // Given
        val name = "Ana"

        // When
        val check = useCase(
            name,
            takenNames = listOf("Pierre", "Ana", "ana p."),
            creatorName = "Pierre",
        )

        // Then
        assertEquals(
            MemberNameCheck.Taken(
                existingName = "Ana",
                suggestions = listOf("Ana 2"),
            ),
            check,
        )
    }

    @Test
    fun `GIVEN a creator in lower case WHEN checking a taken name THEN the initial is upper case`() {
        // Given
        val name = "Ana"

        // When
        val check = useCase(
            name,
            takenNames = listOf("pierre", "Ana"),
            creatorName = "  pierre",
        )

        // Then
        assertEquals(
            MemberNameCheck.Taken(
                existingName = "Ana",
                suggestions = listOf("Ana P.", "Ana 2"),
            ),
            check,
        )
    }

    @Test
    fun `GIVEN a creator without a name WHEN checking a taken name THEN suggests only the number nickname`() {
        // Given
        val name = "Ana"

        // When
        val check = useCase(
            name,
            takenNames = listOf("Ana"),
            creatorName = " ",
        )

        // Then
        assertEquals(
            MemberNameCheck.Taken(
                existingName = "Ana",
                suggestions = listOf("Ana 2"),
            ),
            check,
        )
    }

    @Test
    fun `GIVEN a taken name of 37 letters WHEN checking it THEN both nicknames fit in 40`() {
        // Given
        val existing = "A".repeat(37)

        // When
        val check = useCase(
            existing,
            takenNames = listOf(existing),
            creatorName = "Pierre",
        )

        // Then
        assertEquals(
            MemberNameCheck.Taken(
                existingName = existing,
                suggestions = listOf("$existing P.", "$existing 2"),
            ),
            check,
        )
    }

    @Test
    fun `GIVEN a taken name of 38 letters WHEN checking it THEN leaves out the initial nickname that passes 40`() {
        // Given
        val existing = "A".repeat(38)

        // When
        val check = useCase(
            existing,
            takenNames = listOf(existing),
            creatorName = "Pierre",
        )

        // Then
        assertEquals(
            MemberNameCheck.Taken(
                existingName = existing,
                suggestions = listOf("$existing 2"),
            ),
            check,
        )
    }

    @Test
    fun `GIVEN a taken name of 40 letters WHEN checking it THEN suggests no nickname`() {
        // Given
        val existing = "A".repeat(40)

        // When
        val check = useCase(
            existing,
            takenNames = listOf(existing),
            creatorName = "Pierre",
        )

        // Then
        assertEquals(
            MemberNameCheck.Taken(
                existingName = existing,
                suggestions = emptyList(),
            ),
            check,
        )
    }

    @Test
    fun `GIVEN 38 letters and numbers 2 to 9 taken WHEN checking it THEN suggests no number past 40`() {
        // Given
        val existing = "A".repeat(38)
        val taken = listOf(existing) + (2..9).map { "$existing $it" }

        // When
        val check = useCase(
            existing,
            takenNames = taken,
            creatorName = "Pierre",
        )

        // Then
        assertEquals(
            MemberNameCheck.Taken(
                existingName = existing,
                suggestions = emptyList(),
            ),
            check,
        )
    }

    @Test
    fun `GIVEN a creator name starting with an emoji WHEN checking a taken name THEN the initial is that emoji`() {
        // Given
        val name = "Ana"

        // When
        val check = useCase(
            name,
            takenNames = listOf("\uD83D\uDE00 Pierre", "Ana"),
            creatorName = "\uD83D\uDE00 Pierre",
        )

        // Then
        assertEquals(
            MemberNameCheck.Taken(
                existingName = "Ana",
                suggestions = listOf("Ana \uD83D\uDE00.", "Ana 2"),
            ),
            check,
        )
    }

    @Test
    fun `GIVEN a creator with an empty name WHEN checking a taken name THEN suggests only the number nickname`() {
        // Given
        val name = "Ana"

        // When
        val check = useCase(
            name,
            takenNames = listOf("Ana"),
            creatorName = "",
        )

        // Then
        assertEquals(
            MemberNameCheck.Taken(
                existingName = "Ana",
                suggestions = listOf("Ana 2"),
            ),
            check,
        )
    }
}
