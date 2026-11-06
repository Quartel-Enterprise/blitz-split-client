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
        val check = useCase(name)

        // Then
        assertEquals(MemberNameCheck.Blank, check)
    }

    @Test
    fun `GIVEN a draft of only spaces WHEN checking the name THEN it is blank`() {
        // Given
        val name = "   "

        // When
        val check = useCase(name)

        // Then
        assertEquals(MemberNameCheck.Blank, check)
    }

    @Test
    fun `GIVEN a single letter between spaces WHEN checking the name THEN it is too short`() {
        // Given
        val name = " J "

        // When
        val check = useCase(name)

        // Then
        assertEquals(MemberNameCheck.TooShort, check)
    }

    @Test
    fun `GIVEN two emoji WHEN checking the name THEN it is available`() {
        // Given
        val name = "\uD83D\uDE00\uD83D\uDE00"

        // When
        val check = useCase(name)

        // Then
        assertEquals(MemberNameCheck.Available(name), check)
    }

    @Test
    fun `GIVEN a name WHEN checking it THEN it is available`() {
        // Given
        val name = "Ana"

        // When
        val check = useCase(name)

        // Then
        assertEquals(MemberNameCheck.Available("Ana"), check)
    }

    @Test
    fun `GIVEN extra spaces in a new name WHEN checking it THEN it is available trimmed with single spaces`() {
        // Given
        val name = "  Ana    Paula "

        // When
        val check = useCase(name)

        // Then
        assertEquals(MemberNameCheck.Available("Ana Paula"), check)
    }
}
