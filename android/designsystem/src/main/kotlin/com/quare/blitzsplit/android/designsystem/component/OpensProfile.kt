package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.R

/*
 * Why: a person's face, name or row that opens their profile (beta feedback 18), Contatos' or Perfil for this
 * device's user, while onClick is set; TalkBack says "Abrir perfil" for the tap. Someone with none (no longer in the
 * group) gets null and stays plain content, never a disabled button.
 */
@Composable
fun Modifier.opensProfile(onClick: (() -> Unit)?): Modifier = clickableWhen(
    isClickable = onClick != null,
    onClickLabel = stringResource(R.string.designsystem_open_profile),
    onClick = { onClick?.invoke() },
)
