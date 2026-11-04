package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

/*
 * Why: a group's or a person's name that opens them only while there is something to open; otherwise plain content,
 * never a disabled button TalkBack would announce.
 */
fun Modifier.clickableWhen(
    isClickable: Boolean,
    onClickLabel: String? = null,
    onClick: () -> Unit,
): Modifier = if (isClickable) {
    clickable(
        role = Role.Button,
        onClickLabel = onClickLabel,
        onClick = onClick,
    )
} else {
    this
}
