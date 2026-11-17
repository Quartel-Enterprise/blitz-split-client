package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import com.quare.blitzsplit.core.model.AvatarColor

/*
 * Why: the group's avatar in a top bar, a button that opens its photo big, as the profile's avatar opens its own; the
 * touch target keeps the 48 dp minimum around the smaller badge. The group's screen and the bill share it.
 */
@Composable
fun GroupPhotoButton(
    initials: String,
    color: AvatarColor,
    photoUrl: String?,
    size: Dp,
    clickLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeModifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(GroupAvatarShape)
            .clickable(
                onClickLabel = clickLabel,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        GroupBadge(
            initials = initials,
            color = color,
            photoUrl = photoUrl,
            size = size,
            modifier = badgeModifier,
        )
    }
}
