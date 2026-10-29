package com.quare.blitzsplit.core.groups.domain.model

/*
 * Why: the letters a group or a member shows without a photo, as the design draws them: the first letter of the
 * first two words, skipping the short words that join them ("Churrasco de sábado" → "CS"), or the first two letters
 * of a single word ("Ana" → "AN").
 */
object Initials {
    private val joiningWords = setOf("de", "da", "do", "das", "dos", "e", "of", "the", "and")

    fun of(name: String): String {
        val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val significant = words.filterNot { it.lowercase() in joiningWords }.ifEmpty { words }
        val letters = when (significant.size) {
            0 -> ""
            1 -> significant.first().takeLetters(2)
            else -> significant.take(2).joinToString("") { it.takeLetters(1) }
        }
        return letters.uppercase()
    }

    // Why: the one letter a member's face shows ("Ana" → "A").
    fun firstOf(name: String): String = name.trim().takeLetters(1).uppercase()
}
