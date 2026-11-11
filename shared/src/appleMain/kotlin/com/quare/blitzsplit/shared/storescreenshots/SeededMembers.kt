package com.quare.blitzsplit.shared.storescreenshots

// Why: a seeded group and the member id each of the trip's people got in it, which CreateGroup makes up.
internal data class SeededMembers(
    val groupId: String,
    val idsByPerson: Map<Int, String>,
) {
    fun idOf(person: Int): String = idsByPerson.getValue(person)
}
