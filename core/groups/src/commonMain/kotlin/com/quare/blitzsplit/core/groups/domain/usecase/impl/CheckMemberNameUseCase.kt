package com.quare.blitzsplit.core.groups.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.model.GroupNameRules
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.model.MemberNameCheck
import com.quare.blitzsplit.core.groups.domain.model.MemberNameRules
import com.quare.blitzsplit.core.groups.domain.usecase.CheckMemberName

/*
 * Why: two members of a group can't have the same name, or nobody could tell them apart in an expense. Names
 * match whatever their case or spacing, and the nicknames offered are free in the group: the name with the
 * initial of whoever is adding it ("Ana P.") and with the first free number ("Ana 2").
 */
class CheckMemberNameUseCase : CheckMemberName {
    private val whitespace = Regex("\\s+")

    override fun invoke(
        name: String,
        takenNames: List<String>,
        adderName: String,
    ): MemberNameCheck {
        val normalized = normalize(name)
        if (normalized.isEmpty()) return MemberNameCheck.Blank
        if (!MemberNameRules.isLongEnough(normalized)) return MemberNameCheck.TooShort
        val taken = takenNames.map(::normalize)
        val existingIndex = taken.indexOfFirst { it.equals(normalized, ignoreCase = true) }
        if (existingIndex < 0) return MemberNameCheck.Available(normalized)
        val existingName = taken[existingIndex]
        return MemberNameCheck.Taken(
            existingName = existingName,
            suggestions = suggestionsFor(existingName, taken, adderName),
        )
    }

    private fun suggestionsFor(
        existingName: String,
        taken: List<String>,
        adderName: String,
    ): List<String> {
        fun isFree(candidate: String): Boolean =
            GroupNameRules.isWithinLimit(candidate) && taken.none { it.equals(candidate, ignoreCase = true) }

        val initial = Initials.firstOf(adderName).ifEmpty { null }
        val withInitial = initial?.let { "$existingName $it." }?.takeIf(::isFree)
        val withNumber = generateSequence(2) { it + 1 }
            .map { "$existingName $it" }
            .take(taken.size + 1)
            .firstOrNull(::isFree)
        return listOfNotNull(withInitial, withNumber)
    }

    private fun normalize(name: String): String = name.trim().split(whitespace).joinToString(" ")
}
