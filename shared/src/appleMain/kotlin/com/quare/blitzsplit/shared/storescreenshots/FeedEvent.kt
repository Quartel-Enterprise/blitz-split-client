package com.quare.blitzsplit.shared.storescreenshots

import kotlinx.serialization.json.JsonObject

// Why: one of Atividade's seeded events, before the snapshot fields every event carries are added to its data.
internal data class FeedEvent(
    val kind: String,
    val actor: Int,
    val occurredAt: Long,
    val data: JsonObject,
)
