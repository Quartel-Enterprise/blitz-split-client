package com.quare.blitzsplit.android.designsystem.component

/*
 * Why: the screen a group's row or a bill's row opened it from. Home's cards and Groups' rows show the same groups, and
 * Home and "All expenses" the same bills, so a key per origin keeps a tab switch between them from flying a group's
 * avatar or a bill's title from one to the other; the group's or the bill's screen takes the origin of the screen
 * under it.
 */
enum class SharedOrigin { HOME, GROUPS, ALL_EXPENSES }
