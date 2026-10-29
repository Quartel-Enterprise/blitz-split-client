package com.quare.blitzsplit.core.groups.domain.model

sealed interface MemberNameCheck {
    // Why: the name as it goes into the group, trimmed and with single spaces.
    data class Available(
        val name: String,
    ) : MemberNameCheck

    data object Blank : MemberNameCheck

    // Why: the member already called that, as the group spells it, and the nicknames that would tell them apart.
    data class Taken(
        val existingName: String,
        val suggestions: List<String>,
    ) : MemberNameCheck
}
