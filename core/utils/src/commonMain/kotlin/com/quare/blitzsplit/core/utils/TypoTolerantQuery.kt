package com.quare.blitzsplit.core.utils

import kotlin.math.abs

/*
 * Why: the home's "Search" finds "Larissa" from "Larisa" (Pierre's beta feedback), ignoring case and accents
 * (TextFolding). The text holding the query as typed is an exact find; failing that, every word typed must start one of
 * the text's words, in any order, with at most one typo from 4 letters and two from 8 (a letter missing, extra, wrong
 * or two swapped: Damerau-Levenshtein's optimal string alignment). Against the whole word, or from 5 letters against its
 * start as long or a little longer, so a word still being typed finds too ("larsi" finds "Larissa") while a short one
 * doesn't find every word sharing its first letters ("mara" finds "Maria", not "Marcos"). Built once per query;
 * finding is a few small tables per word, fast over a device's few thousand texts.
 */
class TypoTolerantQuery private constructor(
    private val folded: String,
    private val words: List<String>,
    private val allowsTypos: Boolean,
) {
    // Why: where the text matched, end excluded, at the same place in the text shown since folding keeps the length.
    data class Found(
        val start: Int,
        val end: Int,
        // Why: 0 for the text holding what was typed, or each word typed starting one of its words.
        val typos: Int,
    ) {
        val isExact: Boolean
            get() = typos == 0
    }

    val isEmpty: Boolean
        get() = folded.isEmpty()

    // Why: only the exact finds, without the typo tables, so a search pays for typos only when nothing is exact.
    fun toExactOnly(): TypoTolerantQuery = TypoTolerantQuery(folded, words, allowsTypos = false)

    // Why: null when the query is empty or the text is too far from it.
    fun findIn(text: String): Found? {
        if (isEmpty) return null
        val foldedText = TextFolding.toFolded(text)
        val start = foldedText.indexOf(folded)
        if (start >= 0) return Found(start, start + folded.length, typos = 0)
        return findByWordsIn(foldedText)
    }

    // Why: each word typed finds its closest word in the text; the find spans from the first of them to the last.
    private fun findByWordsIn(text: String): Found? {
        val textWords = wordRangesOf(text)
        if (words.isEmpty() || textWords.isEmpty()) return null
        var start = Int.MAX_VALUE
        var end = Int.MIN_VALUE
        var typos = 0
        for (word in words) {
            val found = closestWordOf(word, text, textWords) ?: return null
            start = minOf(start, found.start)
            end = maxOf(end, found.end)
            typos += found.typos
        }
        return Found(start, end, typos)
    }

    private fun closestWordOf(
        word: String,
        text: String,
        textWords: List<IntRange>,
    ): Found? {
        val allowed = if (allowsTypos) allowedTyposOf(word.length) else 0
        var closest: Found? = null
        for (range in textWords) {
            val startsWithWord = range.last + 1 - range.first >= word.length &&
                text.regionMatches(range.first, word, 0, word.length)
            val typos = when {
                startsWithWord -> 0
                allowed == 0 -> continue
                else -> prefixTyposOf(word, text, range, allowed) ?: continue
            }
            if (closest == null || typos < closest.typos) closest = Found(range.first, range.last + 1, typos)
            if (typos == 0) break
        }
        return closest
    }

    companion object {
        private const val ONE_TYPO_FROM = 4
        private const val TWO_TYPOS_FROM = 8
        private const val PREFIX_TYPOS_FROM = 5

        fun of(query: String): TypoTolerantQuery {
            val folded = TextFolding.toFolded(query.trim())
            return TypoTolerantQuery(
                folded = folded,
                words = wordRangesOf(folded).map { folded.substring(it.first, it.last + 1) },
                allowsTypos = true,
            )
        }

        // Why: none under 4 letters, where one typo already finds half the names ("ana" would find "ane", "and").
        private fun allowedTyposOf(length: Int): Int = when {
            length >= TWO_TYPOS_FROM -> 2
            length >= ONE_TYPO_FROM -> 1
            else -> 0
        }

        // Why: the words are the runs of letters and digits, so "Apê 302" and "Ana-Clara" split as people read them.
        private fun wordRangesOf(text: String): List<IntRange> {
            val ranges = mutableListOf<IntRange>()
            var start = -1
            text.forEachIndexed { index, char ->
                if (char.isLetterOrDigit()) {
                    if (start < 0) start = index
                } else if (start >= 0) {
                    ranges += start until index
                    start = -1
                }
            }
            if (start >= 0) ranges += start until text.length
            return ranges
        }

        /*
         * Why: the fewest typos between the word typed and the text's word, whole (as long, give or take the typos) or,
         * from 5 letters, its start as long as the word typed or a little longer: the optimal string alignment table of
         * the word against the longest of those, whose last row holds the distance to every shorter one, three rows at
         * a time.
         */
        private fun prefixTyposOf(
            word: String,
            text: String,
            textWord: IntRange,
            allowed: Int,
        ): Int? {
            val textLength = textWord.last + 1 - textWord.first
            val lengths = buildSet {
                if (word.length >= PREFIX_TYPOS_FROM && textLength >= word.length) {
                    addAll(word.length..minOf(textLength, word.length + allowed))
                }
                if (abs(textLength - word.length) <= allowed) add(textLength)
            }
            val longest = lengths.maxOrNull() ?: return null
            val offset = textWord.first
            var beforePrevious = IntArray(longest + 1)
            var previous = IntArray(longest + 1) { it }
            var current = IntArray(longest + 1)
            for (i in 1..word.length) {
                current[0] = i
                for (j in 1..longest) {
                    val cost = if (word[i - 1] == text[offset + j - 1]) 0 else 1
                    var best = minOf(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + cost)
                    val isSwap =
                        i > 1 && j > 1 && word[i - 1] == text[offset + j - 2] && word[i - 2] == text[offset + j - 1]
                    if (isSwap) best = minOf(best, beforePrevious[j - 2] + 1)
                    current[j] = best
                }
                val recycled = beforePrevious
                beforePrevious = previous
                previous = current
                current = recycled
            }
            val fewest = lengths.minOf { previous[it] }
            return fewest.takeIf { it <= allowed }
        }
    }
}
