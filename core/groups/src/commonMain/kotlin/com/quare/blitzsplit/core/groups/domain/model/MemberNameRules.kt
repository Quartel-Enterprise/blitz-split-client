package com.quare.blitzsplit.core.groups.domain.model

/*
 * Why: the same bounds as a profile's name, since joining a group without a profile makes one from the member's
 * name. GroupNameRules caps the length, as for a group's name.
 */
object MemberNameRules {
    const val MIN_LENGTH: Int = 2

    fun isLongEnough(name: String): Boolean = name.trim().letterCount >= MIN_LENGTH
}
