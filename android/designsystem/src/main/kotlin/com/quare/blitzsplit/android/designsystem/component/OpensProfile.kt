package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.quare.blitzsplit.android.designsystem.R

/*
 * Why: a person's face, name or row that opens their profile (beta feedback 18), Contatos' or Perfil for this
 * device's user, only while they have one; TalkBack says "Abrir perfil" for the tap. Someone no longer in the group
 * stays plain content, never a disabled button.
 */
@Composable
fun Modifier.opensProfileWhen(
    isClickable: Boolean,
    onClick: () -> Unit,
): Modifier = if (isClickable) {
    clickable(
        role = Role.Button,
        onClickLabel = stringResource(R.string.designsystem_open_profile),
        onClick = onClick,
    )
} else {
    this
}
