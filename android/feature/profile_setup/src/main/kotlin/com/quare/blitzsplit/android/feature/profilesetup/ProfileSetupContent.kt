package com.quare.blitzsplit.android.feature.profilesetup

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.layout.AdaptiveScreen
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.android.designsystem.theme.LocalIsDarkTheme
import com.quare.blitzsplit.android.designsystem.theme.toContainerColor
import com.quare.blitzsplit.android.designsystem.theme.toContentColor
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.feature.profilesetup.presentation.model.NameError
import com.quare.blitzsplit.feature.profilesetup.presentation.model.ProfileSetupUiEvent
import com.quare.blitzsplit.feature.profilesetup.presentation.model.ProfileSetupUiState
import com.quare.blitzsplit.android.designsystem.R as DesignR

private val singleColumnMaxWidth = 560.dp
private val twoColumnsMaxWidth = 920.dp
private val tabletAvatarCardWidth = 340.dp

@Composable
fun ProfileSetupContent(
    state: ProfileSetupUiState,
    onEvent: (ProfileSetupUiEvent) -> Unit,
    onAddPhotoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        AdaptiveScreen(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding(),
        ) { layout ->
            when {
                layout.isTablet && layout.isLandscape -> ProfileSetupTabletLandscapeLayout(
                    state = state,
                    onEvent = onEvent,
                    onAddPhotoClick = onAddPhotoClick,
                )

                layout.isLandscape -> ProfileSetupPhoneLandscapeLayout(
                    state = state,
                    onEvent = onEvent,
                    onAddPhotoClick = onAddPhotoClick,
                )

                else -> ProfileSetupSingleColumn(
                    state = state,
                    onEvent = onEvent,
                    onAddPhotoClick = onAddPhotoClick,
                )
            }
        }
    }
}

@Composable
private fun ProfileSetupSingleColumn(
    state: ProfileSetupUiState,
    onEvent: (ProfileSetupUiEvent) -> Unit,
    onAddPhotoClick: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(modifier = Modifier.widthIn(max = singleColumnMaxWidth).fillMaxSize()) {
            BackButton(
                onEvent = onEvent,
                modifier = Modifier.padding(8.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProfileSetupTitle(isLarge = false)
                AvatarSection(
                    state = state,
                    onEvent = onEvent,
                    onAddPhotoClick = onAddPhotoClick,
                    avatarSize = 112.dp,
                    swatchSpacing = 16.dp,
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                )
                NameField(
                    state = state,
                    onEvent = onEvent,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SavedLocallyLabel()
                ContinueButton(
                    state = state,
                    onEvent = onEvent,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// Why: little height lying down, so the avatar goes left and the name with "Continuar" right.
@Composable
private fun ProfileSetupPhoneLandscapeLayout(
    state: ProfileSetupUiState,
    onEvent: (ProfileSetupUiEvent) -> Unit,
    onAddPhotoClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = 32.dp,
                end = 32.dp,
                top = 8.dp,
                bottom = 16.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(48.dp),
    ) {
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            BackButton(
                onEvent = onEvent,
                modifier = Modifier.offset(x = (-12).dp),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center,
            ) {
                AvatarSection(
                    state = state,
                    onEvent = onEvent,
                    onAddPhotoClick = onAddPhotoClick,
                    avatarSize = 104.dp,
                    swatchSpacing = 10.dp,
                )
            }
        }
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ProfileSetupTitle(isLarge = false)
                NameField(
                    state = state,
                    onEvent = onEvent,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            ProfileSetupFooterRow(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}

@Composable
private fun ProfileSetupTabletLandscapeLayout(
    state: ProfileSetupUiState,
    onEvent: (ProfileSetupUiEvent) -> Unit,
    onAddPhotoClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        BackButton(
            onEvent = onEvent,
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 8.dp,
            ),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 48.dp,
                    end = 48.dp,
                    bottom = 24.dp,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier.widthIn(max = twoColumnsMaxWidth),
                horizontalArrangement = Arrangement.spacedBy(64.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.width(tabletAvatarCardWidth),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                ) {
                    AvatarSection(
                        state = state,
                        onEvent = onEvent,
                        onAddPhotoClick = onAddPhotoClick,
                        avatarSize = 144.dp,
                        swatchSpacing = 12.dp,
                        modifier = Modifier.padding(
                            horizontal = 24.dp,
                            vertical = 40.dp,
                        ),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ProfileSetupTitle(isLarge = true)
                    NameField(
                        state = state,
                        onEvent = onEvent,
                        modifier = Modifier.padding(top = 22.dp),
                    )
                    ProfileSetupFooterRow(
                        state = state,
                        onEvent = onEvent,
                        modifier = Modifier.padding(top = 28.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BackButton(
    onEvent: (ProfileSetupUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = { onEvent(ProfileSetupUiEvent.OnBackClicked) },
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = stringResource(DesignR.string.designsystem_back),
        )
    }
}

@Composable
private fun ProfileSetupTitle(isLarge: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.profile_setup_title),
            style = if (isLarge) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.profile_setup_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AvatarSection(
    state: ProfileSetupUiState,
    onEvent: (ProfileSetupUiEvent) -> Unit,
    onAddPhotoClick: () -> Unit,
    avatarSize: Dp,
    swatchSpacing: Dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AvatarWithPhotoButton(
                state = state,
                avatarSize = avatarSize,
                onAddPhotoClick = onAddPhotoClick,
            )
            if (state.showsPhotoError) {
                Text(
                    text = stringResource(R.string.profile_setup_photo_error),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.profile_setup_avatar_color),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AvatarColorRow(
                selected = state.avatarColor,
                colors = state.avatarColors,
                spacing = swatchSpacing,
                onEvent = onEvent,
            )
        }
    }
}

// Why: the camera sits on the avatar's corner, as in the design, and the photo shows in the avatar itself.
@Composable
private fun AvatarWithPhotoButton(
    state: ProfileSetupUiState,
    avatarSize: Dp,
    onAddPhotoClick: () -> Unit,
) {
    Box(modifier = Modifier.size(avatarSize)) {
        AvatarBadge(
            initial = state.avatarInitial,
            color = state.avatarColor,
            size = avatarSize,
            photoUrl = state.photoUrl,
        )
        if (state.isPreparingPhoto) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        FilledIconButton(
            onClick = onAddPhotoClick,
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
                imageVector = Icons.Outlined.PhotoCamera,
                contentDescription = stringResource(R.string.profile_setup_add_photo),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun NameField(
    state: ProfileSetupUiState,
    onEvent: (ProfileSetupUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = state.name,
        onValueChange = { onEvent(ProfileSetupUiEvent.OnNameChanged(it)) },
        modifier = modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.profile_setup_name_label)) },
        placeholder = { Text(stringResource(R.string.profile_setup_name_placeholder)) },
        isError = state.nameError != null,
        singleLine = true,
        supportingText = {
            val hint = when (state.nameError) {
                NameError.TOO_SHORT -> stringResource(R.string.profile_setup_name_too_short, state.nameMinLength)
                NameError.TOO_LONG -> stringResource(R.string.profile_setup_name_too_long, state.nameMaxLength)
                null -> stringResource(R.string.profile_setup_name_hint)
            }
            Text(hint)
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onEvent(ProfileSetupUiEvent.OnContinueClicked) }),
    )
}

@Composable
private fun SavedLocallyLabel(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.PhoneAndroid,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(R.string.profile_setup_saved_locally),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProfileSetupFooterRow(
    state: ProfileSetupUiState,
    onEvent: (ProfileSetupUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SavedLocallyLabel(modifier = Modifier.weight(1f))
        ContinueButton(
            state = state,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun ContinueButton(
    state: ProfileSetupUiState,
    onEvent: (ProfileSetupUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = { onEvent(ProfileSetupUiEvent.OnContinueClicked) },
        enabled = state.canContinue && !state.isSaving,
        modifier = modifier.height(56.dp),
    ) {
        Text(
            text = stringResource(R.string.profile_setup_continue),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun AvatarColorRow(
    selected: AvatarColor,
    colors: List<AvatarColor>,
    spacing: Dp,
    onEvent: (ProfileSetupUiEvent) -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    Row(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
    ) {
        colors.forEach { color ->
            val isSelected = color == selected
            val label = stringResource(color.labelRes)
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .semantics { contentDescription = label }
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onEvent(ProfileSetupUiEvent.OnAvatarColorSelected(color)) },
                    ),
                shape = CircleShape,
                color = color.toContainerColor(isDark),
                contentColor = color.toContentColor(isDark),
                border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSelected) Icon(Icons.Outlined.Check, contentDescription = null)
                }
            }
        }
    }
}

@get:StringRes
private val AvatarColor.labelRes: Int
    get() = when (this) {
        AvatarColor.BLUE -> R.string.profile_setup_color_blue
        AvatarColor.PEACH -> R.string.profile_setup_color_peach
        AvatarColor.GREEN -> R.string.profile_setup_color_green
        AvatarColor.LILAC -> R.string.profile_setup_color_lilac
        AvatarColor.SAND -> R.string.profile_setup_color_sand
    }

@DevicePreviews
@Composable
private fun ProfileSetupPreview() {
    BlitzSplitTheme {
        ProfileSetupContent(
            state = ProfileSetupUiState(
                name = "Pierre",
                avatarColor = AvatarColor.BLUE,
                photo = null,
                isPreparingPhoto = false,
                showsPhotoError = false,
                nameError = null,
                canContinue = true,
                isSaving = false,
            ),
            onEvent = {},
            onAddPhotoClick = {},
        )
    }
}

@DevicePreviews
@Composable
private fun ProfileSetupInvalidNamePreview() {
    BlitzSplitTheme {
        ProfileSetupContent(
            state = ProfileSetupUiState(
                name = "P",
                avatarColor = AvatarColor.PEACH,
                photo = null,
                isPreparingPhoto = false,
                showsPhotoError = true,
                nameError = NameError.TOO_SHORT,
                canContinue = false,
                isSaving = false,
            ),
            onEvent = {},
            onAddPhotoClick = {},
        )
    }
}
