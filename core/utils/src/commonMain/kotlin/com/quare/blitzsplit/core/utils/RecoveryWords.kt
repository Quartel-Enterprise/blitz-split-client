package com.quare.blitzsplit.core.utils

/*
 * Why: mirrors _shared/recovery_code.ts of the API, so a word that isn't in the list is caught before any request. The
 * code is 12 words of the BIP39 English list, stored and sent as the full words joined by single spaces. Only the first
 * four letters of a word count (BIP39 tells its words apart by them), so "abandom" still reads as "abandon". The
 * checksum is the server's to check.
 */
object RecoveryWords {
    const val COUNT: Int = 12
    private const val PREFIX_LENGTH = 4
    private const val SUGGESTION_LIMIT = 3

    private val wordByPrefix: Map<String, String> = BIP39_ENGLISH.associateBy { it.take(PREFIX_LENGTH) }
    private val words: Set<String> = BIP39_ENGLISH.toSet()
    private val separators = Regex("[^a-z]+")
    private val numberedWord = Regex("""(?<![0-9])([0-9]{1,2})\s*[.):-]?\s*([A-Za-z]+)""")

    // Why: a list's numbers, commas, new lines and the "+" of a QR's link all split words, whatever people paste.
    fun split(input: String): List<String> = input
        .lowercase()
        .split(separators)
        .filter { it.isNotEmpty() }

    // Why: only the first four letters count, so a word mistyped after them still reads as the one it was.
    fun wordOf(typed: String): String? {
        val token = split(typed).singleOrNull() ?: return null
        return wordByPrefix[token.take(PREFIX_LENGTH)]
    }

    fun isWord(text: String): Boolean = text in words

    // Why: a field is marked wrong only once no word can come out of it, not while it is still being typed.
    fun isDeadEnd(typed: String): Boolean {
        if (typed.isBlank()) return false
        return wordOf(typed) == null && suggestionsFor(typed).isEmpty()
    }

    fun suggestionsFor(typed: String): List<String> {
        val token = split(typed).singleOrNull() ?: return emptyList()
        return BIP39_ENGLISH.filter { it.startsWith(token) }.take(SUGGESTION_LIMIT)
    }

    // Why: the server hashes the full words joined by single spaces, so prefixes are expanded before sending.
    fun normalize(input: String): String? {
        val words = split(input).map { wordOf(it) ?: return null }
        return words.takeIf { it.size == COUNT }?.joinToString(" ")
    }

    /*
     * Why: a pasted text may hold more than the words, as the web's downloaded file does (a heading, a sentence after
     * them), and a heading's words can be list words too ("split", "code", "recovery" read as "record"). A list
     * numbered 1 to 12 is read by its numbers. Any other text by a run of 12 whole words of the list, the last one
     * since a heading comes before the words, and only then by a run that needs prefixes.
     */
    fun findCodeIn(input: String): List<String>? {
        findTypedWordsIn(input)?.let { words -> return words.takeIf { it.all(::isWord) } }
        val windows = split(input).windowed(COUNT)
        return windows.lastOrNull { window -> window.all(::isWord) }
            ?: windows
                .firstOrNull { window -> window.all { wordOf(it) != null } }
                ?.map { wordOf(it).orEmpty() }
    }

    /*
     * Why: a numbered list with a mistyped word is still the person's 12 words, so it fills the fields by number and
     * the wrong one is marked in its own field. Only the first word given each number counts, so a date or a count
     * further down ("Saved on 12 March") can't replace a listed word.
     */
    fun findTypedWordsIn(input: String): List<String>? {
        val wordByNumber = mutableMapOf<Int, String>()
        numberedWord.findAll(input.lowercase()).forEach { match ->
            wordByNumber.getOrPut(match.groupValues[1].toInt()) { match.groupValues[2] }
        }
        return (1..COUNT).map { number ->
            val typed = wordByNumber[number] ?: return null
            wordOf(typed) ?: typed
        }
    }

    // Why: a code stored before the words (12 base32 characters) isn't shown as if it were words.
    fun wordsOf(code: String): List<String>? = normalize(code)?.split(" ")
}
