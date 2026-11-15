package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.quare.blitzsplit.core.model.AvatarColor

/*
 * Why: a home tab's top bar, its title on a wide screen and the profile's avatar. Across the screen on a phone
 * upright; beside a pane it sits over the list column only (no insets of its own, the screen's Scaffold pads them),
 * so what is beside the list starts at the top.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTabTopBar(
    title: String,
    avatarInitial: String,
    avatarColor: AvatarColor,
    photoUrl: String?,
    profileDescription: String,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
) {
    TopAppBar(
        title = { HomeTabTitle(title) },
        actions = {
            ProfileAvatarButton(
                initial = avatarInitial,
                color = avatarColor,
                photoUrl = photoUrl,
                contentDescription = profileDescription,
                onClick = onProfileClick,
            )
        },
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        windowInsets = windowInsets,
    )
}
