package com.quare.blitzsplit.android.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.quare.blitzsplit.android.designsystem.component.DialogSystemBarsEffect
import com.quare.blitzsplit.android.designsystem.component.SearchBarField
import com.quare.blitzsplit.android.designsystem.layout.AdaptiveSheet
import com.quare.blitzsplit.android.designsystem.layout.isPhone
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiEvent
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiState
import com.quare.blitzsplit.android.designsystem.R as DesignR

/*
 * Why: "Language" as Bible Planner's language sheet: a row per option with its chip and a radio, the pick closing it.
 * With eleven languages the sheet filled a phone's screen (Pierre's feedback, November 2026), so a phone, upright or
 * lying down, opens it as a screen of its own; wider windows keep the dialog, off the hinge as the profile's other
 * dialogs. Both search the languages by any name the row shows.
 */
@Composable
internal fun LanguageSheet(
    state: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    val names = languageSearchNames()
    val options = state.findLanguageOptions { names.getValue(it) }
    val onDismiss = { onEvent(ProfileUiEvent.OnDialogDismissed) }
    if (rememberFoldLayout().formFactor.isPhone) {
        LanguageScreen(
            state = state,
            options = options,
            onDismiss = onDismiss,
            onEvent = onEvent,
        )
    } else {
        AdaptiveSheet(
            title = stringResource(R.string.profile_language),
            onDismissRequest = onDismiss,
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
                LanguageSearchContent(
                    state = state,
                    options = options,
                    onEvent = onEvent,
                )
            }
        }
    }
}

// Why: a screen over the profile, its bar's back closing it as the system's back does, the list scrolling under it.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageScreen(
    state: ProfileUiState,
    options: List<AppLanguage>,
    onDismiss: () -> Unit,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        DialogSystemBarsEffect(isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f)
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.profile_language)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(DesignR.string.designsystem_back),
                            )
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 24.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LanguageSearchContent(
                    state = state,
                    options = options,
                    onEvent = onEvent,
                )
            }
        }
    }
}

// Why: "Search languages" over the rows it leaves, or a line saying none matched.
@Composable
private fun LanguageSearchContent(
    state: ProfileUiState,
    options: List<AppLanguage>,
    onEvent: (ProfileUiEvent) -> Unit,
) {
    SearchBarField(
        query = state.languageQuery,
        placeholder = stringResource(R.string.profile_language_search),
        clearLabel = stringResource(R.string.profile_language_search_clear),
        onQueryChange = { onEvent(ProfileUiEvent.OnLanguageQueryChanged(it)) },
        modifier = Modifier.fillMaxWidth(),
    )
    if (options.isEmpty()) {
        Text(
            text = stringResource(R.string.profile_language_no_results, state.languageQuery.trim()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    } else {
        LanguageList(
            selected = state.language,
            options = options,
            onSelect = { onEvent(ProfileUiEvent.OnLanguageSelected(it)) },
        )
    }
}

// Why: every name a row shows, which the search looks in: its own, the app language's, System's "Device language".
@Composable
private fun languageSearchNames(): Map<AppLanguage, List<String>> = AppLanguage.entries.associateWith { language ->
    listOfNotNull(
        languageName(language),
        translatedLanguageName(language),
        if (language == AppLanguage.SYSTEM) stringResource(R.string.profile_language_system_detail) else null,
    )
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
    val name = languageName(language)
    // Why: the translated name says it again in the app's language, unless both read the same ("Italiano").
    val detail = if (language == AppLanguage.SYSTEM) {
        stringResource(R.string.profile_language_system_detail)
    } else {
        translatedLanguageName(language).takeUnless { it == name }
    }
    ListItem(
        headlineContent = {
            // Why: in its own locale, so TalkBack reads "日本語" as Japanese and the font draws its glyphs as Japan's.
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(localeList = language.tag?.let { LocaleList(it) })) { append(name) }
                },
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            )
        },
        supportingContent = detail?.let { { Text(it) } },
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

/*
 * Why: the language's code ("EN"; "BR" and "PT", "419" and "ES" for the two of each) as Bible Planner's chip, a phone
 * for System.
 */
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
        val code = language.chipCode
        if (code == null) {
            Icon(
                imageVector = Icons.Outlined.Smartphone,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(20.dp),
            )
        } else {
            // Why: "419" is wider than the two-letter codes, so at a large font scale it shrinks to fit the circle.
            Text(
                text = code,
                modifier = Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = content,
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 8.sp,
                    maxFontSize = MaterialTheme.typography.labelLarge.fontSize,
                ),
            )
        }
    }
}

/*
 * Why: each language in its own language, the way its speakers look for it (Pierre's call, beta feedback), the region
 * spelled out for the ones written for a region: "Português (Brasil)", "Español (España)".
 */
@Composable
internal fun languageName(language: AppLanguage): String = when (language) {
    AppLanguage.SYSTEM -> stringResource(R.string.profile_language_system)
    AppLanguage.ENGLISH -> stringResource(R.string.profile_language_native_english)
    AppLanguage.PORTUGUESE -> stringResource(R.string.profile_language_native_portuguese)
    AppLanguage.PORTUGUESE_PORTUGAL -> stringResource(R.string.profile_language_native_portuguese_portugal)
    AppLanguage.SPANISH -> stringResource(R.string.profile_language_native_spanish)
    AppLanguage.SPANISH_SPAIN -> stringResource(R.string.profile_language_native_spanish_spain)
    AppLanguage.FRENCH -> stringResource(R.string.profile_language_native_french)
    AppLanguage.GERMAN -> stringResource(R.string.profile_language_native_german)
    AppLanguage.ITALIAN -> stringResource(R.string.profile_language_native_italian)
    AppLanguage.POLISH -> stringResource(R.string.profile_language_native_polish)
    AppLanguage.DUTCH -> stringResource(R.string.profile_language_native_dutch)
    AppLanguage.JAPANESE -> stringResource(R.string.profile_language_native_japanese)
    AppLanguage.TURKISH -> stringResource(R.string.profile_language_native_turkish)
    AppLanguage.INDONESIAN -> stringResource(R.string.profile_language_native_indonesian)
    AppLanguage.RUSSIAN -> stringResource(R.string.profile_language_native_russian)
    AppLanguage.UKRAINIAN -> stringResource(R.string.profile_language_native_ukrainian)
    AppLanguage.KOREAN -> stringResource(R.string.profile_language_native_korean)
    AppLanguage.HINDI -> stringResource(R.string.profile_language_native_hindi)
    AppLanguage.ARABIC -> stringResource(R.string.profile_language_native_arabic)
    AppLanguage.CHINESE_SIMPLIFIED -> stringResource(R.string.profile_language_native_chinese_simplified)
    AppLanguage.HEBREW -> stringResource(R.string.profile_language_native_hebrew)
    AppLanguage.THAI -> stringResource(R.string.profile_language_native_thai)
    AppLanguage.VIETNAMESE -> stringResource(R.string.profile_language_native_vietnamese)
    AppLanguage.SWEDISH -> stringResource(R.string.profile_language_native_swedish)
    AppLanguage.CZECH -> stringResource(R.string.profile_language_native_czech)
    AppLanguage.ROMANIAN -> stringResource(R.string.profile_language_native_romanian)
    AppLanguage.BENGALI -> stringResource(R.string.profile_language_native_bengali)
    AppLanguage.MALAY -> stringResource(R.string.profile_language_native_malay)
    AppLanguage.GREEK -> stringResource(R.string.profile_language_native_greek)
    AppLanguage.CROATIAN -> stringResource(R.string.profile_language_native_croatian)
    AppLanguage.HUNGARIAN -> stringResource(R.string.profile_language_native_hungarian)
    AppLanguage.URDU -> stringResource(R.string.profile_language_native_urdu)
    AppLanguage.FILIPINO -> stringResource(R.string.profile_language_native_filipino)
    AppLanguage.SERBIAN -> stringResource(R.string.profile_language_native_serbian)
    AppLanguage.SLOVAK -> stringResource(R.string.profile_language_native_slovak)
    AppLanguage.PERSIAN -> stringResource(R.string.profile_language_native_persian)
    AppLanguage.TAMIL -> stringResource(R.string.profile_language_native_tamil)
    AppLanguage.NORWEGIAN -> stringResource(R.string.profile_language_native_norwegian)
    AppLanguage.TELUGU -> stringResource(R.string.profile_language_native_telugu)
    AppLanguage.BULGARIAN -> stringResource(R.string.profile_language_native_bulgarian)
    AppLanguage.MARATHI -> stringResource(R.string.profile_language_native_marathi)
    AppLanguage.SWAHILI -> stringResource(R.string.profile_language_native_swahili)
    AppLanguage.CHINESE_TRADITIONAL -> stringResource(R.string.profile_language_native_chinese_traditional)
}

// Why: the same language in the app's language, "Portuguese (Brazil)", under its own name.
@Composable
private fun translatedLanguageName(language: AppLanguage): String = when (language) {
    AppLanguage.SYSTEM -> stringResource(R.string.profile_language_system)
    AppLanguage.ENGLISH -> stringResource(R.string.profile_language_english)
    AppLanguage.PORTUGUESE -> stringResource(R.string.profile_language_portuguese)
    AppLanguage.PORTUGUESE_PORTUGAL -> stringResource(R.string.profile_language_portuguese_portugal)
    AppLanguage.SPANISH -> stringResource(R.string.profile_language_spanish)
    AppLanguage.SPANISH_SPAIN -> stringResource(R.string.profile_language_spanish_spain)
    AppLanguage.FRENCH -> stringResource(R.string.profile_language_french)
    AppLanguage.GERMAN -> stringResource(R.string.profile_language_german)
    AppLanguage.ITALIAN -> stringResource(R.string.profile_language_italian)
    AppLanguage.POLISH -> stringResource(R.string.profile_language_polish)
    AppLanguage.DUTCH -> stringResource(R.string.profile_language_dutch)
    AppLanguage.JAPANESE -> stringResource(R.string.profile_language_japanese)
    AppLanguage.TURKISH -> stringResource(R.string.profile_language_turkish)
    AppLanguage.INDONESIAN -> stringResource(R.string.profile_language_indonesian)
    AppLanguage.RUSSIAN -> stringResource(R.string.profile_language_russian)
    AppLanguage.UKRAINIAN -> stringResource(R.string.profile_language_ukrainian)
    AppLanguage.KOREAN -> stringResource(R.string.profile_language_korean)
    AppLanguage.HINDI -> stringResource(R.string.profile_language_hindi)
    AppLanguage.ARABIC -> stringResource(R.string.profile_language_arabic)
    AppLanguage.CHINESE_SIMPLIFIED -> stringResource(R.string.profile_language_chinese_simplified)
    AppLanguage.HEBREW -> stringResource(R.string.profile_language_hebrew)
    AppLanguage.THAI -> stringResource(R.string.profile_language_thai)
    AppLanguage.VIETNAMESE -> stringResource(R.string.profile_language_vietnamese)
    AppLanguage.SWEDISH -> stringResource(R.string.profile_language_swedish)
    AppLanguage.CZECH -> stringResource(R.string.profile_language_czech)
    AppLanguage.ROMANIAN -> stringResource(R.string.profile_language_romanian)
    AppLanguage.BENGALI -> stringResource(R.string.profile_language_bengali)
    AppLanguage.MALAY -> stringResource(R.string.profile_language_malay)
    AppLanguage.GREEK -> stringResource(R.string.profile_language_greek)
    AppLanguage.CROATIAN -> stringResource(R.string.profile_language_croatian)
    AppLanguage.HUNGARIAN -> stringResource(R.string.profile_language_hungarian)
    AppLanguage.URDU -> stringResource(R.string.profile_language_urdu)
    AppLanguage.FILIPINO -> stringResource(R.string.profile_language_filipino)
    AppLanguage.SERBIAN -> stringResource(R.string.profile_language_serbian)
    AppLanguage.SLOVAK -> stringResource(R.string.profile_language_slovak)
    AppLanguage.PERSIAN -> stringResource(R.string.profile_language_persian)
    AppLanguage.TAMIL -> stringResource(R.string.profile_language_tamil)
    AppLanguage.NORWEGIAN -> stringResource(R.string.profile_language_norwegian)
    AppLanguage.TELUGU -> stringResource(R.string.profile_language_telugu)
    AppLanguage.BULGARIAN -> stringResource(R.string.profile_language_bulgarian)
    AppLanguage.MARATHI -> stringResource(R.string.profile_language_marathi)
    AppLanguage.SWAHILI -> stringResource(R.string.profile_language_swahili)
    AppLanguage.CHINESE_TRADITIONAL -> stringResource(R.string.profile_language_chinese_traditional)
}
