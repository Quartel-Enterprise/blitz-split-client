package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.layout.LocalFormBleed
import com.quare.blitzsplit.android.designsystem.layout.horizontalBleed
import kotlinx.coroutines.flow.first

/*
 * Why: a single choice among more options than fit side by side (Dividir's six modes, beta feedback): a row of pill
 * chips that scrolls sideways, the picked one filled and always scrolled into sight. Each chip is a radio button to a
 * screen reader, the row their group. It reaches into the form's side margins (`LocalFormBleed`), so the chips scroll
 * to the column's edge rather than being cut short of it (beta feedback), the first one still lined up with the form.
 */
@Composable
fun ChoiceChipRow(
    labels: List<String>,
    selectedIndex: Int,
    groupLabel: String,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val bleed = LocalFormBleed.current
    val layoutDirection = LocalLayoutDirection.current
    val listState = rememberLazyListState()
    LaunchedEffect(selectedIndex) {
        if (selectedIndex < 0) return@LaunchedEffect
        // Why: waits for the first layout, so a chip already in sight never scrolls the ones before it away.
        val layout = snapshotFlow { listState.layoutInfo }.first { it.visibleItemsInfo.isNotEmpty() }
        val item = layout.visibleItemsInfo.firstOrNull { it.index == selectedIndex }
        val isFullyVisible = item != null && item.offset >= 0 && item.offset + item.size <= layout.viewportEndOffset
        if (!isFullyVisible) listState.animateScrollToItem(selectedIndex)
    }
    LazyRow(
        state = listState,
        modifier = modifier
            .horizontalBleed(bleed)
            .fillMaxWidth()
            .selectableGroup()
            .semantics { contentDescription = groupLabel },
        contentPadding = PaddingValues(
            start = contentPadding.calculateStartPadding(layoutDirection) + bleed.start,
            top = contentPadding.calculateTopPadding(),
            end = contentPadding.calculateEndPadding(layoutDirection) + bleed.end,
            bottom = contentPadding.calculateBottomPadding(),
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(labels) { index, label ->
            val isSelected = index == selectedIndex
            FilterChip(
                selected = isSelected,
                onClick = { onClick(index) },
                label = {
                    Text(
                        text = label,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                    )
                },
                modifier = Modifier
                    .heightIn(min = height)
                    .semantics { role = Role.RadioButton },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
                border = null,
            )
        }
    }
}
