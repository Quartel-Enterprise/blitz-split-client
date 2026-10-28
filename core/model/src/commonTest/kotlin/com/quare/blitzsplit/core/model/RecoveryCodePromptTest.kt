package com.quare.blitzsplit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class RecoveryCodePromptTest {
    @Test
    fun `GIVEN each prompt WHEN checking whether the sheet shows THEN only a due or reviewed code does`() {
        // Given
        val prompts = RecoveryCodePrompt.entries

        // When
        val shown = prompts.filter { it.isShown }

        // Then
        assertEquals(listOf(RecoveryCodePrompt.DUE, RecoveryCodePrompt.REVIEW), shown)
    }
}
