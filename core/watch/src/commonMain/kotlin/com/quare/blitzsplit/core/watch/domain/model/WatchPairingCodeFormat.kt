package com.quare.blitzsplit.core.watch.domain.model

// Why: the code as typed or shown, "K7P 4QX" or "k7p4qx": case and spaces never matter, as the server reads it.
object WatchPairingCodeFormat {
    const val LENGTH = 6

    // Why: only A to Z and 0 to 9 as the server makes them; another script's letter or digit could never match.
    fun normalize(input: String): String = input
        .uppercase()
        .filter { it in 'A'..'Z' || it in '0'..'9' }
        .take(LENGTH)

    fun isComplete(input: String): Boolean = normalize(input).length == LENGTH
}
