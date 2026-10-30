package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState

private const val SPLIT_FACE_COUNT = 3
private val outerRadius = 20.dp
private val innerRadius = 6.dp
private val splitFaceSize = 26.dp

// Why: the controls: who paid, when, how it splits, and the offline note under them.
@Composable
internal fun BillDetailsSection(
    state: SplitBillUiState,
    money: MoneyFormatter,
    dates: DateTimeTextFormatter,
    onWhenClick: () -> Unit,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        BillOptionRow(
            label = stringResource(R.string.split_bill_payer),
            // Why: an edited bill keeps whoever paid it after they left the group.
            value = state.payer?.let { personNameOf(it) }
                ?: stringResource(R.string.split_bill_former_member)
                    .takeIf { state.isPayerFormerMember }
                    .orEmpty(),
            subtitle = null,
            shape = rowShapeOf(index = 0),
            onClick = { onEvent(SplitBillUiEvent.OnPayerClicked) },
        ) {
            val payer = state.payer
            if (payer == null) {
                LeadingIcon { Icon(Icons.Outlined.Person, contentDescription = null) }
            } else {
                AvatarBadge(
                    initial = payer.initials,
                    color = payer.avatarColor,
                )
            }
        }
        BillOptionRow(
            label = stringResource(R.string.split_bill_when),
            value = spentAtTextOf(
                spentAt = state.spentAt,
                spentDay = state.spentDay,
                dates = dates,
            ),
            subtitle = null,
            shape = rowShapeOf(index = 1),
            onClick = onWhenClick,
        ) {
            LeadingIcon { Icon(Icons.Outlined.Event, contentDescription = null) }
        }
        BillOptionRow(
            label = stringResource(R.string.split_bill_split),
            value = splitTitleOf(state.split),
            subtitle = splitSubtitleOrNull(
                split = state.split,
                money = money,
            ),
            shape = rowShapeOf(index = 2),
            onClick = { onEvent(SplitBillUiEvent.OnSplitClicked) },
        ) {
            Row(
                modifier = Modifier.width(40.dp),
                horizontalArrangement = Arrangement.spacedBy((-14).dp),
            ) {
                state.split.included.take(SPLIT_FACE_COUNT).forEach { person ->
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape)
                            .padding(2.dp),
                    ) {
                        AvatarBadge(
                            initial = person.initials,
                            color = person.avatarColor,
                            size = splitFaceSize,
                        )
                    }
                }
            }
        }
    }
    if (state.showsOfflineNotice) {
        OfflineNoteRow()
    }
}

private fun rowShapeOf(index: Int): Shape {
    val lastIndex = 2
    return RoundedCornerShape(
        topStart = if (index == 0) outerRadius else innerRadius,
        topEnd = if (index == 0) outerRadius else innerRadius,
        bottomStart = if (index == lastIndex) outerRadius else innerRadius,
        bottomEnd = if (index == lastIndex) outerRadius else innerRadius,
    )
}

@Composable
private fun BillOptionRow(
    label: String,
    value: String,
    subtitle: String?,
    shape: Shape,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 64.dp)
                .padding(
                    start = 16.dp,
                    end = 12.dp,
                    top = 8.dp,
                    bottom = 8.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.clearAndSetSemantics {}) { leading() }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LeadingIcon(icon: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

// Why: saving works offline; the note says where the bill waits, with the home's pending dot.
@Composable
private fun OfflineNoteRow() {
    Row(
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(8.dp)
                .background(MaterialTheme.colorScheme.tertiary, CircleShape),
        )
        Text(
            text = stringResource(R.string.split_bill_offline),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
