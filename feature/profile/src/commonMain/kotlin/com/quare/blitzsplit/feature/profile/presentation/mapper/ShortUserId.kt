package com.quare.blitzsplit.feature.profile.presentation.mapper

private const val HEAD_LENGTH = 8
private const val TAIL_LENGTH = 4

/*
 * Why: "User ID" shows the id shortened, its start and its end, so it fits a phone's line beside the copy button and
 * still matches the whole id the row copies; an id too short to shorten shows whole.
 */
internal fun String.toShortUserId(): String = if (length <= HEAD_LENGTH + TAIL_LENGTH + 1) {
    this
} else {
    "${take(HEAD_LENGTH)}…${takeLast(TAIL_LENGTH)}"
}
