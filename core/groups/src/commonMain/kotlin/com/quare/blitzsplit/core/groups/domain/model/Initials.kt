package com.quare.blitzsplit.core.groups.domain.model

import com.quare.blitzsplit.core.utils.toAppLanguageUppercase

/*
 * Why: the letters a group or a member shows without a photo, as the design draws them: the first letter of the
 * first two words, skipping the short words that join them ("Churrasco de sábado" → "CS"), or only the first letter
 * of a single word ("Larissa" → "L"), capitalized as the app's language writes them ("ilker" → "İ" in Turkish).
 */
object Initials {
    private val joiningWords = setOf("de", "da", "do", "das", "dos", "e", "of", "the", "and")
    private val whitespace = Regex("\\s+")

    fun of(name: String): String {
        val words = name.trim().split(whitespace).filter { it.isNotEmpty() }
        val significant = words.filterNot { it.lowercase() in joiningWords }.ifEmpty { words }
        return significant.take(2).joinToString("") { it.takeLetters(1) }.toAppLanguageUppercase()
    }

    // Why: the one letter a member's face shows ("Ana" → "A").
    fun firstOf(name: String): String = name.trim().takeLetters(1).toAppLanguageUppercase()

    // Why: the device's user shows one letter among the group's members, everyone else their initials.
    fun ofMember(
        name: String,
        isMe: Boolean,
    ): String = if (isMe) firstOf(name) else of(name)
}
