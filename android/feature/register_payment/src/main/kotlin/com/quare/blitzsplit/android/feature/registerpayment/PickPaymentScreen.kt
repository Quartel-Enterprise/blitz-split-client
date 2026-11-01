package com.quare.blitzsplit.android.feature.registerpayment

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.DialogCard
import com.quare.blitzsplit.android.designsystem.component.GroupBadge
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentPerson
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PickPaymentGroup
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PickPaymentPair
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PickPaymentUiEvent
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PickPaymentUiState
import com.quare.blitzsplit.feature.registerpayment.presentation.viewmodel.PickPaymentViewModel
import org.koin.compose.viewmodel.koinViewModel
import com.quare.blitzsplit.android.designsystem.R as DesignR

private val pairFaceSize = 32.dp

/*
 * Why: "Quem pagou a quem?" (Route.PickPayment), what Início's "Registrar pagamento" asks first, since it belongs to
 * no group: every open pair of every group, by group, the most recent activity first. A pair opens Registrar
 * pagamento in this sheet's place. Like that form, a bottom sheet on a phone and a dialog on a tablet.
 */
@Composable
fun PickPaymentScreen(
    isDialog: Boolean,
    viewModel: PickPaymentViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler { viewModel.onEvent(PickPaymentUiEvent.OnDismissed) }
    PickPaymentContent(
        state = state,
        isDialog = isDialog,
        onEvent = viewModel::onEvent,
    )
}

@Composable
fun PickPaymentContent(
    state: PickPaymentUiState,
    isDialog: Boolean,
    onEvent: (PickPaymentUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Why: nothing until Room is read, so "Ninguém deve nada" never flashes before the pairs.
    if (!state.isLoaded) return
    if (isDialog) {
        DialogCard(
            isWide = false,
            spacing = 16.dp,
            snackbarHostState = remember { SnackbarHostState() },
            modifier = modifier,
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = { onEvent(PickPaymentUiEvent.OnDismissed) },
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.register_payment_close),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(
                        weight = 1f,
                        fill = false,
                    ).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) { PickPaymentList(state, onEvent) }
        }
    } else {
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 24.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) { PickPaymentList(state, onEvent) }
    }
}

@Composable
private fun ColumnScope.PickPaymentList(
    state: PickPaymentUiState,
    onEvent: (PickPaymentUiEvent) -> Unit,
) {
    Text(
        text = stringResource(R.string.register_payment_pick_title),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { heading() },
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    if (state.showsEmpty) {
        PickPaymentEmptyMessage()
        return
    }
    state.groups.forEach { group ->
        PickPaymentGroupSection(
            group = group,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun PickPaymentEmptyMessage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.register_payment_pick_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.register_payment_pick_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// Why: the group's badge and name, then its pairs joined as one list, as the canvas draws a list of rows.
@Composable
private fun PickPaymentGroupSection(
    group: PickPaymentGroup,
    onEvent: (PickPaymentUiEvent) -> Unit,
) {
    val money = rememberMoneyFormatter(group.currency)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GroupBadge(
                initials = group.initials,
                color = group.color,
                size = 32.dp,
            )
            Text(
                text = group.name,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            group.pairs.forEachIndexed { index, pair ->
                PickPaymentPairRow(
                    pair = pair,
                    money = money,
                    shape = listShapeOf(
                        isFirst = index == 0,
                        isLast = index == group.pairs.lastIndex,
                    ),
                    onClick = {
                        onEvent(
                            PickPaymentUiEvent.OnPairClicked(
                                groupId = group.groupId,
                                fromMemberId = pair.fromMemberId,
                                toMemberId = pair.toMemberId,
                            ),
                        )
                    },
                )
            }
        }
    }
}

// Why: "Ana → Você" over "Ana te deve R$ 162,30", the two faces before them.
@Composable
private fun PickPaymentPairRow(
    pair: PickPaymentPair,
    money: MoneyFormatter,
    shape: Shape,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 12.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PersonFaceBadge(
                    person = pair.from,
                    size = pairFaceSize,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.padding(horizontal = 2.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PersonFaceBadge(
                    person = pair.to,
                    size = pairFaceSize,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        R.string.register_payment_pick_pair,
                        pairNameOf(pair.from),
                        pairNameOf(pair.to),
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = pairDebtTextOf(pair, money),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Why: the list's ends round off, its inner corners stay small, as Material 3 Expressive's segmented lists.
@Composable
private fun listShapeOf(
    isFirst: Boolean,
    isLast: Boolean,
): Shape {
    val outer = MaterialTheme.shapes.large
    val inner = MaterialTheme.shapes.extraSmall
    return when {
        isFirst && isLast -> outer

        isFirst -> outer.copy(
            bottomStart = inner.bottomStart,
            bottomEnd = inner.bottomEnd,
        )

        isLast -> outer.copy(
            topStart = inner.topStart,
            topEnd = inner.topEnd,
        )

        else -> inner
    }
}

@Composable
private fun pairNameOf(person: PaymentPerson?): String =
    if (person?.isMe == true) stringResource(DesignR.string.designsystem_you) else nameOf(person)

// Why: "Ana te deve R$ 162,30", "Você deve R$ 118,00 a Bruno" or "Diego deve R$ 64,00 a Bruno", as the form says it.
@Composable
private fun pairDebtTextOf(
    pair: PickPaymentPair,
    money: MoneyFormatter,
): String {
    val amount = money.toText(pair.amountMinor)
    return when {
        pair.to?.isMe == true -> stringResource(R.string.register_payment_debt_received, nameOf(pair.from), amount)
        pair.from?.isMe == true -> stringResource(R.string.register_payment_debt_paid, nameOf(pair.to), amount)
        else -> stringResource(R.string.register_payment_debt_between, nameOf(pair.from), nameOf(pair.to), amount)
    }
}

private fun createPreviewPerson(
    id: String,
    name: String,
    color: AvatarColor,
    isMe: Boolean = false,
): PaymentPerson = PaymentPerson(
    id = id,
    name = name,
    initials = name.take(2).uppercase(),
    avatarColor = color,
    isMe = isMe,
)

@DevicePreviews
@Composable
private fun PickPaymentContentPreview() {
    val me = createPreviewPerson("me", "Pierre", AvatarColor.BLUE, isMe = true)
    BlitzSplitTheme {
        PickPaymentContent(
            state = PickPaymentUiState(
                isLoaded = true,
                groups = listOf(
                    PickPaymentGroup(
                        groupId = "1",
                        name = "Viagem Floripa",
                        initials = "VF",
                        color = AvatarColor.PEACH,
                        currency = GroupCurrency.BRL,
                        pairs = listOf(
                            PickPaymentPair(
                                from = createPreviewPerson("ana", "Ana", AvatarColor.GREEN),
                                to = me,
                                fromMemberId = "ana",
                                toMemberId = "me",
                                amountMinor = 16_230L,
                            ),
                            PickPaymentPair(
                                from = createPreviewPerson("diego", "Diego", AvatarColor.LILAC),
                                to = createPreviewPerson("bruno", "Bruno", AvatarColor.PEACH),
                                fromMemberId = "diego",
                                toMemberId = "bruno",
                                amountMinor = 6_400L,
                            ),
                        ),
                    ),
                ),
            ),
            isDialog = false,
            onEvent = {},
        )
    }
}
