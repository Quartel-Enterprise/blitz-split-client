package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.core.model.AvatarColor

/*
 * Why: the home tabs' top bar action: the profile's avatar, which opens the Perfil. A name always has an initial, so
 * an empty one is a profile not read yet: a shimmer, never an empty badge in the default color.
 */
@Composable
fun ProfileAvatarButton(
    initial: String,
    color: AvatarColor,
    photoUrl: String?,
    contentDescription: String,
    onClick: () -> Unit,
) {
    if (initial.isEmpty() && photoUrl == null) {
        ShimmerBox(modifier = Modifier.size(48.dp)) {
            ShimmerAvatarSkeleton(
                size = 40.dp,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        return
    }
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
