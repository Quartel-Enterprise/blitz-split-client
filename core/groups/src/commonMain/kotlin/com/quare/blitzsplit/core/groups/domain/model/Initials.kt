package com.quare.blitzsplit.core.groups.domain.model

/*
 * Why: the letters a group or a member shows without a photo, as the design draws them: the first letter of the
 * first two words, skipping the short words that join them ("Churrasco de sábado" → "CS"), or only the first letter
 * of a single word ("Larissa" → "L").
 */
object Initials {
    private val joiningWords = setOf("de", "da", "do", "das", "dos", "e", "of", "the", "and")
    private val whitespace = Regex("\\s+")

    fun of(name: String): String {
        val words = name.trim().split(whitespace).filter { it.isNotEmpty() }
        val significant = words.filterNot { it.lowercase() in joiningWords }.ifEmpty { words }
        return significant.take(2).joinToString("") { it.takeLetters(1) }.uppercase()
    }

    // Why: the one letter a member's face shows ("Ana" → "A").
    fun firstOf(name: String): String = name.trim().takeLetters(1).uppercase()

    // Why: the device's user shows one letter among the group's members, everyone else their initials.
    fun ofMember(
        name: String,
        isMe: Boolean,
    ): String = if (isMe) firstOf(name) else of(name)
}
