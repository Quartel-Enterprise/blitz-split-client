package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.utils.TextFold

/*
 * Why: the map's rule (flow 9): a term matches whole words, ignoring case and accents ("Farmacia" is "farmácia", but
 * "bar" never "barbearia"), a phrase matches the whole phrase ("uber eats"). With several terms in the title the longest
 * wins, and on a tie the one that comes first in it ("Uber Eats no hotel" is Delivery).
 */
object CategoryMatcher {
    fun matchOrNull(
        title: String,
        terms: List<CategoryTerm>,
    ): CategoryMatch? {
        val words = wordsOf(title)
        if (words.isEmpty()) return null
        return terms
            .mapNotNull { term -> firstMatchOrNull(words, term) }
            .sortedWith(compareByDescending<CategoryMatch> { keyOf(it.term).length }.thenBy { it.start })
            .firstOrNull()
    }

    // Why: what a term is matched as, lowercase without accents and single spaces, also the map's key.
    fun keyOf(term: String): String = wordsOf(term).joinToString(" ") { it.text }

    // Why: "letras, números e espaço", 2 to 30 of them (9.2d).
    fun isTermValid(term: String): Boolean {
        val trimmed = term.trim()
        return trimmed.length in MIN_TERM_LENGTH..MAX_TERM_LENGTH &&
            trimmed.all { it.isLetterOrDigit() || it == ' ' } && keyOf(trimmed).length >= MIN_TERM_LENGTH
    }

    private fun firstMatchOrNull(
        words: List<Word>,
        term: CategoryTerm,
    ): CategoryMatch? {
        val termWords = wordsOf(term.term).map { it.text }
        if (termWords.isEmpty()) return null
        val last = words.size - termWords.size
        for (index in 0..last) {
            val isMatch = termWords.indices.all { offset -> words[index + offset].text == termWords[offset] }
            if (isMatch) {
                return CategoryMatch(
                    term = term.term,
                    categoryId = term.categoryId,
                    start = words[index].start,
                    end = words[index + termWords.size - 1].end,
                )
            }
        }
        return null
    }

    private fun wordsOf(text: String): List<Word> {
        val folded = TextFold.toFolded(text)
        val words = mutableListOf<Word>()
        var start = -1
        folded.forEachIndexed { index, char ->
            val isWordChar = char.isLetterOrDigit()
            if (isWordChar && start < 0) start = index
            if (!isWordChar && start >= 0) {
                words += Word(folded.substring(start, index), start, index)
                start = -1
            }
        }
        if (start >= 0) words += Word(folded.substring(start), start, folded.length)
        return words
    }

    private data class Word(
        val text: String,
        val start: Int,
        val end: Int,
    )

    const val MIN_TERM_LENGTH = 2
    const val MAX_TERM_LENGTH = 30
}
