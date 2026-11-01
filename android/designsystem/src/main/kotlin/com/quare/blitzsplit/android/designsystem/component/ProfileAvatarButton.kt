package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.quare.blitzsplit.core.model.AvatarColor

// Why: the home tabs' top bar action: the profile's avatar, which opens the Perfil.
@Composable
fun ProfileAvatarButton(
    initial: String,
    color: AvatarColor,
    photoUrl: String?,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.semantics { this.contentDescription = contentDescription },
    ) {
        AvatarBadge(
            initial = initial,
            color = color,
            photoUrl = photoUrl,
        )
    }
}
