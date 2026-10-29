package com.quare.blitzsplit.core.groups.domain.model

/*
 * Why: a letter per character, as the server's char_length counts them, so an emoji outside the basic plane (two
 * UTF-16 halves) counts once and is never split into a broken glyph.
 */
internal fun String.takeLetters(count: Int): String {
    var end = 0
    repeat(count) {
        if (end < length) end += if (this[end].isHighSurrogate() && end + 1 < length) 2 else 1
    }
    return substring(0, end)
}

internal val String.letterCount: Int
    get() = indices.count { index -> !(this[index].isLowSurrogate() && index > 0 && this[index - 1].isHighSurrogate()) }
