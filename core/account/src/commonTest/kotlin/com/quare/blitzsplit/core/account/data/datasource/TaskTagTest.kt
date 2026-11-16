package com.quare.blitzsplit.core.account.data.datasource

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class TaskTagTest {
    @Test
    fun `GIVEN a build outside a task WHEN reading the metadata THEN there is none`() {
        // Given
        val branches = listOf("", "  ")

        // When
        val metadata = branches.map { branch -> TaskTag(branch).metadata }

        // Then
        assertEquals(listOf<JsonObject?>(null, null), metadata)
    }

    @Test
    fun `GIVEN the build of a task WHEN reading the metadata THEN it carries the task branch`() {
        // Given
        val branch = " chore/clean-anonymous-test-users "

        // When
        val metadata = TaskTag(branch).metadata

        // Then
        assertEquals(JsonObject(mapOf("task" to JsonPrimitive("chore/clean-anonymous-test-users"))), metadata)
    }
}
