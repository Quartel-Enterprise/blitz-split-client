package com.quare.blitzsplit.android.designsystem.component

/*
 * Why: the screen a group's row opened it from. Home's cards and Groups' rows show the same groups, so a key per origin
 * keeps a tab switch between them from flying a group's avatar from one to the other; the group's header takes the
 * origin of the screen under it.
 */
enum class GroupSharedOrigin { HOME, GROUPS }
