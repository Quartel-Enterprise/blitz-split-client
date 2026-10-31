package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.core.model.AvatarColor

// Why: the photo button sits on the avatar's corner, as in the design, and the photo shows in the avatar itself.
@Composable
fun AvatarWithPhotoButton(
    initial: String,
    color: AvatarColor,
    photoUrl: String?,
    isPreparingPhoto: Boolean,
    icon: ImageVector,
    contentDescription: String,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(size)) {
        AvatarBadge(
            initial = initial,
            color = color,
            size = size,
            photoUrl = photoUrl,
        )
        if (isPreparingPhoto) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(
                    x = 4.dp,
                    y = 4.dp,
                ).size(44.dp)
                .border(3.dp, MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
