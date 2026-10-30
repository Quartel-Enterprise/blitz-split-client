package com.quare.blitzsplit.android.feature.groupdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.ConnectedButtonRow
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiState

private const val BALANCES_TAB_INDEX = 1
private const val MEMBERS_TAB_INDEX = 2
private val faceSize = 32.dp
private val faceOverlap = (-8).dp

// Why: the members' faces under the name, "+2" for the rest, and who hasn't joined; tapping them opens Membros.
@Composable
internal fun GroupFacesRow(
    state: GroupDetailUiState,
    onEvent: (GroupDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                onClickLabel = stringResource(R.string.group_detail_see_members),
                role = Role.Button,
                onClick = { onEvent(GroupDetailUiEvent.OnMembersClicked) },
            ).heightIn(min = 48.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.clearAndSetSemantics {},
            horizontalArrangement = Arrangement.spacedBy(faceOverlap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            state.faces.forEach { face ->
                RingedBox {
                    AvatarBadge(
                        initial = face.initials,
                        color = face.color,
                        size = faceSize,
                    )
                }
            }
            if (state.extraMemberCount > 0) {
                RingedBox {
                    Box(
                        modifier = Modifier
                            .size(faceSize)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.group_detail_extra_members, state.extraMemberCount),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (state.notJoinedCount > 0) {
            Text(
                text = pluralStringResource(
                    R.plurals.group_detail_not_joined,
                    state.notJoinedCount,
                    state.notJoinedCount,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Why: the faces overlap, each ringed in the background so its neighbor's edge reads.
@Composable
private fun RingedBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .padding(2.dp),
    ) { content() }
}

@Composable
internal fun GroupTabsRow(
    onEvent: (GroupDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    ConnectedButtonRow(
        labels = listOf(
            stringResource(R.string.group_detail_tab_expenses),
            stringResource(R.string.group_detail_tab_balances),
            stringResource(R.string.group_detail_tab_members),
        ),
        selectedIndex = 0,
        groupLabel = stringResource(R.string.group_detail_tabs),
        onClick = { index ->
            when (index) {
                BALANCES_TAB_INDEX -> onEvent(GroupDetailUiEvent.OnBalancesTabClicked)
                MEMBERS_TAB_INDEX -> onEvent(GroupDetailUiEvent.OnMembersClicked)
            }
        },
        modifier = modifier,
    )
}
