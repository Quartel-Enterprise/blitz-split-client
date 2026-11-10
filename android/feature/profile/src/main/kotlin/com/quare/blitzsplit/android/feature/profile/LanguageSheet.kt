package com.quare.blitzsplit.android.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.layout.AdaptiveSheet
import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiEvent
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiState

/*
 * Why: "Language" as Bible Planner's language sheet: a row per option with its chip and a radio, the pick closing it,
 * in the app's short sheet, as the profile's other dialogs keep off the hinge.
 */
@Composable
internal fun LanguageSheet(
    state: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    AdaptiveSheet(
        title = stringResource(R.string.profile_language),
        onDismissRequest = { onEvent(ProfileUiEvent.OnDialogDismissed) },
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 24.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.profile_language),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            LanguageList(
                selected = state.language,
                options = state.languageOptions,
                onSelect = { onEvent(ProfileUiEvent.OnLanguageSelected(it)) },
            )
        }
    }
}

@Composable
private fun LanguageList(
    selected: AppLanguage,
    options: List<AppLanguage>,
    onSelect: (AppLanguage) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = Color.Transparent,
    ) {
        Column(modifier = Modifier.selectableGroup()) {
            options.forEachIndexed { index, language ->
                if (index > 0) HorizontalDivider()
                LanguageRow(
                    language = language,
                    isSelected = language == selected,
                    onClick = { onSelect(language) },
                )
            }
        }
    }
}

@Composable
private fun LanguageRow(
    language: AppLanguage,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    ListItem(
        headlineContent = {
            Text(
                text = languageName(language),
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            )
        },
        supportingContent = if (language == AppLanguage.SYSTEM) {
            { Text(stringResource(R.string.profile_language_system_detail)) }
        } else {
            null
        },
        leadingContent = {
            LanguageChip(
                language = language,
                isSelected = isSelected,
            )
        },
        trailingContent = {
            RadioButton(
                selected = isSelected,
                onClick = null,
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = if (isSelected) primary.copy(alpha = 0.12f) else Color.Transparent,
        ),
        modifier = Modifier.selectable(
            selected = isSelected,
            role = Role.RadioButton,
            onClick = onClick,
        ),
    )
}

// Why: the language's code ("EN", "PT", "ES") as Bible Planner's chip, the phone for System.
@Composable
private fun LanguageChip(
    language: AppLanguage,
    isSelected: Boolean,
) {
    val container = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val content = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        val code = language.tag?.substringBefore('-')?.uppercase()
        if (code == null) {
            Icon(
                imageVector = Icons.Outlined.Smartphone,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Text(
                text = code,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = content,
            )
        }
    }
}

// Why: each language in the app's language, "Português (BR)" saying the region it is written for.
@Composable
internal fun languageName(language: AppLanguage): String = when (language) {
    AppLanguage.SYSTEM -> stringResource(R.string.profile_language_system)
    AppLanguage.ENGLISH -> stringResource(R.string.profile_language_english)
    AppLanguage.PORTUGUESE -> stringResource(R.string.profile_language_portuguese)
    AppLanguage.SPANISH -> stringResource(R.string.profile_language_spanish)
    AppLanguage.FRENCH -> stringResource(R.string.profile_language_french)
    AppLanguage.GERMAN -> stringResource(R.string.profile_language_german)
    AppLanguage.ITALIAN -> stringResource(R.string.profile_language_italian)
    AppLanguage.POLISH -> stringResource(R.string.profile_language_polish)
    AppLanguage.DUTCH -> stringResource(R.string.profile_language_dutch)
    AppLanguage.JAPANESE -> stringResource(R.string.profile_language_japanese)
    AppLanguage.TURKISH -> stringResource(R.string.profile_language_turkish)
}
