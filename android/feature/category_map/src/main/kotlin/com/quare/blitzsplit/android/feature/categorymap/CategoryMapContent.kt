package com.quare.blitzsplit.android.feature.categorymap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.layout.FormFactor
import com.quare.blitzsplit.android.designsystem.layout.TwoHalvesLayout
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import com.quare.blitzsplit.feature.categorymap.presentation.model.CategoryMapUiEvent
import com.quare.blitzsplit.feature.categorymap.presentation.model.CategoryMapUiState
import com.quare.blitzsplit.feature.categorymap.presentation.model.MapSection

private val singleColumnMaxWidth = 560.dp
private val landscapeListWidth = 320.dp
private val tabletListWidth = 360.dp
private val tabletContainerWidth = 920.dp
private val itemGap = 12.dp
private val rowGap = 2.dp

/*
 * Why: "Mapa de categorias" (9.1): one column of cards on a phone, centered at 560dp upright on a tablet and a
 * foldable; lying down, on a tablet lying down (in 920dp) and on a book (split on the hinge), the categories on the
 * left and the terms of the one picked on the right. "Novo termo" and "Editar termo" open over it (TermSheet).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryMapContent(
    state: CategoryMapUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (CategoryMapUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    /*
     * Why: a failed save or delete keeps the category's form open over the map, so its snackbar shows in the form,
     * and only there rather than once more under its scrim.
     */
    val idleSnackbarHostState = remember { SnackbarHostState() }
    val isCategoryFormOpen = state.categoryForm != null
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.category_map_title)) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(CategoryMapUiEvent.OnBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.category_map_back),
                        )
                    }
                },
                // Why: adding and restoring take no change until the map is read (canEditMap).
                actions = {
                    if (state.canEditMap) {
                        MapActionsMenu(
                            canCreateCategory = state.canCreateCategory,
                            onEvent = onEvent,
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(if (isCategoryFormOpen) idleSnackbarHostState else snackbarHostState) },
    ) { padding ->
        val formFactor = rememberFoldLayout().formFactor
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (formFactor) {
                FormFactor.PhoneLandscape -> SideBySideLayout(
                    state = state,
                    listWidth = landscapeListWidth,
                    onEvent = onEvent,
                    modifier = Modifier.padding(
                        start = 24.dp,
                        end = 24.dp,
                    ),
                )

                FormFactor.TabletLandscape -> SideBySideLayout(
                    state = state,
                    listWidth = tabletListWidth,
                    onEvent = onEvent,
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .width(tabletContainerWidth),
                )

                FormFactor.Book, FormFactor.Tabletop -> TwoHalvesLayout(
                    start = {
                        MapListColumn(
                            state = state,
                            showsCards = false,
                            onEvent = onEvent,
                        )
                    },
                    end = {
                        MapDetailColumn(
                            state = state,
                            onEvent = onEvent,
                        )
                    },
                )

                else -> MapListColumn(
                    state = state,
                    showsCards = true,
                    onEvent = onEvent,
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .widthIn(max = singleColumnMaxWidth),
                )
            }
        }
    }
    state.termForm?.let {
        TermSheet(
            state = state,
            onEvent = onEvent,
        )
    }
    if (isCategoryFormOpen) {
        CategoryFormDialog(
            state = state,
            snackbarHostState = snackbarHostState,
            onEvent = onEvent,
        )
    }
    if (state.isRestoreConfirmOpen) {
        RestoreMapDialog(
            state = state,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun MapActionsMenu(
    canCreateCategory: Boolean,
    onEvent: (CategoryMapUiEvent) -> Unit,
) {
    var isMenuOpen by rememberSaveable { mutableStateOf(false) }
    IconButton(onClick = {
        onEvent(
            CategoryMapUiEvent.OnAddTermClicked(
                categoryId = null,
                text = "",
            ),
        )
    }) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = stringResource(R.string.category_map_add),
        )
    }
    Box {
        IconButton(onClick = { isMenuOpen = true }) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.category_map_more),
            )
        }
        DropdownMenu(
            expanded = isMenuOpen,
            onDismissRequest = { isMenuOpen = false },
        ) {
            // Why: a category goes in a group (9.4), so with none on the device there is nowhere to create one.
            if (canCreateCategory) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.category_map_new_category)) },
                    onClick = {
                        isMenuOpen = false
                        onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.category_map_restore)) },
                onClick = {
                    isMenuOpen = false
                    onEvent(CategoryMapUiEvent.OnRestoreClicked)
                },
            )
        }
    }
}

@Composable
private fun SideBySideLayout(
    state: CategoryMapUiState,
    listWidth: Dp,
    onEvent: (CategoryMapUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        MapListColumn(
            state = state,
            showsCards = false,
            onEvent = onEvent,
            modifier = Modifier
                .width(listWidth)
                .fillMaxHeight(),
        )
        MapDetailColumn(
            state = state,
            onEvent = onEvent,
            modifier = Modifier.weight(1f),
        )
    }
}

/*
 * Why: the search, the notices, then what the search found, the empty map, or the categories: as cards with their
 * terms in one column, or as a list to pick from beside the terms.
 */
@Composable
private fun MapListColumn(
    state: CategoryMapUiState,
    showsCards: Boolean,
    onEvent: (CategoryMapUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 24.dp - itemGap,
        ),
    ) {
        gapItem(key = "search") {
            MapSearchField(
                state = state,
                onEvent = onEvent,
            )
        }
        mapNotices(
            state = state,
            onEvent = onEvent,
        )
        when {
            state.isSearching -> mapSearchResults(
                state = state,
                onEvent = onEvent,
            )

            !state.isLoaded -> gapItem(key = "skeleton") { CategoryMapSkeleton(showsCards = showsCards) }

            state.isEmpty -> gapItem(key = "empty") { EmptyMapContent(onEvent = onEvent) }

            showsCards -> mapCards(
                state = state,
                onEvent = onEvent,
            )

            else -> mapCategoryList(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}

/*
 * Why: a card's rows sit 2 dp apart and everything else 12, which one arrangement for the whole list can't say, so
 * each item brings the gap under it and every row of a long card is an item of its own.
 */
private fun LazyListScope.gapItem(
    key: String,
    contentType: String = key,
    content: @Composable () -> Unit,
) {
    item(
        key = key,
        contentType = contentType,
    ) {
        Box(modifier = Modifier.padding(bottom = itemGap)) { content() }
    }
}

@Composable
private fun MapDetailColumn(
    state: CategoryMapUiState,
    onEvent: (CategoryMapUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val section = state.selectedSection
    Column(modifier = modifier.fillMaxSize()) {
        when {
            !state.isLoaded -> CategoryMapSkeleton(showsCards = true)

            state.isEmpty -> EmptyMapContent(onEvent = onEvent)

            section != null -> SectionDetailPane(
                section = state.findSelectedSection(),
                state = state,
                onEvent = onEvent,
            )
        }
    }
}

private fun CategoryMapUiState.findSelectedSection(): MapSection = selectedSection ?: sections.first()

private fun LazyListScope.mapNotices(
    state: CategoryMapUiState,
    onEvent: (CategoryMapUiEvent) -> Unit,
) {
    if (state.showsOfflineNotice) gapItem(key = "offline") { OfflineQueueBanner(count = state.map.pendingCount) }
    if (state.showsDeviceOnlyNotice && !state.isSearching) {
        gapItem(key = "device-only") { DeviceOnlyCard(onEvent = onEvent) }
    }
    if (state.isLoaded && !state.isSearching && !state.isEmpty) {
        gapItem(key = "intro") { MapIntroText(isOffline = !state.isOnline) }
    }
}

private fun LazyListScope.mapCards(
    state: CategoryMapUiState,
    onEvent: (CategoryMapUiEvent) -> Unit,
) {
    state.sections.forEach { section ->
        gapItem(
            key = "card-${section.category.id}",
            contentType = "card",
        ) {
            SectionCard(
                section = section,
                state = state,
                onEvent = onEvent,
            )
        }
    }
}

private fun LazyListScope.mapCategoryList(
    state: CategoryMapUiState,
    onEvent: (CategoryMapUiEvent) -> Unit,
) {
    val selectedId = state.selectedSection?.category?.id
    val sections = state.sections
    itemsIndexed(
        items = sections,
        key = { _, section -> "category-${section.category.id}" },
        contentType = { _, _ -> "category" },
    ) { index, section ->
        Box(modifier = Modifier.padding(bottom = if (index == sections.lastIndex) itemGap else rowGap)) {
            SectionListRow(
                section = section,
                isSelected = section.category.id == selectedId,
                index = index,
                count = sections.size,
                onEvent = onEvent,
            )
        }
    }
}

private fun LazyListScope.mapSearchResults(
    state: CategoryMapUiState,
    onEvent: (CategoryMapUiEvent) -> Unit,
) {
    if (state.showsNoResult) {
        gapItem(key = "no-result") {
            NoTermFoundContent(
                query = state.query.trim(),
                onEvent = onEvent,
            )
        }
        return
    }
    val results = state.searchResults
    item(
        key = "results-title",
        contentType = "results-title",
    ) {
        SearchResultsTitle(
            count = results.size,
            query = state.query.trim(),
        )
    }
    itemsIndexed(
        items = results,
        key = { _, result -> "result-${result.term.key}" },
        contentType = { _, _ -> "result" },
    ) { index, result ->
        val isLast = index == results.lastIndex
        Box(modifier = Modifier.padding(bottom = if (isLast && results.size == 1) itemGap else rowGap)) {
            SearchResultRow(
                result = result,
                index = index,
                count = results.size,
                onEvent = onEvent,
            )
        }
    }
    if (results.size > 1) gapItem(key = "longest-wins") { LongestTermWinsText() }
}
