package com.quare.blitzsplit.feature.members

import com.quare.blitzsplit.feature.members.presentation.model.MemberNameEditor
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MemberNameEditorTest {
    @Test
    fun `GIVEN no member WHEN reading the editor THEN it adds someone`() {
        // Given
        val editor = editor(memberId = null)

        // When
        val isAdding = editor.isAdding

        // Then
        assertTrue(isAdding)
    }

    @Test
    fun `GIVEN a member WHEN reading the editor THEN it renames them`() {
        // Given
        val editor = editor(memberId = "member-2")

        // When
        val isAdding = editor.isAdding

        // Then
        assertFalse(isAdding)
    }

    @Test
    fun `GIVEN a free name WHEN reading the editor THEN it can save`() {
        // Given
        val editor = editor(draft = "Diego")

        // When
        val canSave = editor.canSave

        // Then
        assertTrue(canSave)
    }

    @Test
    fun `GIVEN an empty draft WHEN reading the editor THEN it cannot save`() {
        // Given
        val editor = editor(draft = "")

        // When
        val canSave = editor.canSave

        // Then
        assertFalse(canSave)
    }

    @Test
    fun `GIVEN a draft of only spaces WHEN reading the editor THEN it cannot save`() {
        // Given
        val editor = editor(draft = "   ")

        // When
        val canSave = editor.canSave

        // Then
        assertFalse(canSave)
    }

    @Test
    fun `GIVEN a name another member has WHEN reading the editor THEN it cannot save`() {
        // Given
        val editor = editor(
            draft = "ana",
            takenName = "Ana",
        )

        // When
        val canSave = editor.canSave

        // Then
        assertFalse(canSave)
    }

    private fun editor(
        memberId: String? = null,
        draft: String = "Diego",
        takenName: String? = null,
    ): MemberNameEditor = MemberNameEditor(
        memberId = memberId,
        draft = draft,
        takenName = takenName,
        nicknameSuggestions = if (takenName == null) emptyList() else listOf("$takenName P.", "$takenName 2"),
    )
}
