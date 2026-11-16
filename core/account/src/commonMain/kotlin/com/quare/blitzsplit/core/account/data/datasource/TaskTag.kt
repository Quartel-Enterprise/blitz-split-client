package com.quare.blitzsplit.core.account.data.datasource

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/*
 * Why: an anonymous account made by a development task's build carries the task's branch, so finish-task can delete
 * that task's accounts and the server expires a forgotten one after a day; a build outside a task sends nothing.
 * The composition root provides it, so a branch only changes that module's build, not the cached modules below it.
 */
class TaskTag(
    branch: String,
) {
    val metadata: JsonObject? = branch.trim().takeIf(String::isNotEmpty)?.let { task ->
        JsonObject(mapOf(METADATA_KEY to JsonPrimitive(task)))
    }

    private companion object {
        const val METADATA_KEY = "task"
    }
}
