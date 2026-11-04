package com.quare.blitzsplit.feature.categorymap.presentation.model

/*
 * Why: a group on the device, the most recent activity first: "só no Viagem Floripa" beside its categories, and the
 * "Grupo" a category created in the map goes in (9.4).
 */
data class MapGroup(
    val id: String,
    val name: String,
    // Why: "Criada por Ana" in "Editar categoria", by member id.
    val memberNames: Map<String, String>,
    // Why: who creates a category from this device, in that group.
    val meId: String?,
)
