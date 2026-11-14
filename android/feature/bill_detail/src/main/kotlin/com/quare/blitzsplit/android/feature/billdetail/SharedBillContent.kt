package com.quare.blitzsplit.android.feature.billdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.InfoNoteBanner
import com.quare.blitzsplit.android.designsystem.component.LinkProblemMessage
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberDateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import com.quare.blitzsplit.android.designsystem.layout.rememberUsesFormColumns
import com.quare.blitzsplit.feature.billdetail.presentation.model.SharedBillStatus
import com.quare.blitzsplit.feature.billdetail.presentation.model.SharedBillUiEvent
import com.quare.blitzsplit.feature.billdetail.presentation.model.SharedBillUiState
import com.quare.blitzsplit.android.designsystem.R as DesignR

private val problemMaxWidth = 560.dp

/*
 * Why: a bill's link page full screen at every size, since it opens over wherever the app is and belongs to no group
 * on screen: Conta's columns (one upright, two lying down and on a book, each on its side of the hinge) with the comment field
 * pinned at the bottom upright and under the comments lying down, or one centered column saying why it doesn't open.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SharedBillContent(
    state: SharedBillUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (SharedBillUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Why: a book keeps one column on each side of its hinge whichever way it is held; elsewhere two only lying down.
    val usesTwoColumns = rememberFoldLayout().bookSplit != null || rememberUsesFormColumns(isDialog = false)
    val fieldColor = MaterialTheme.colorScheme.surfaceContainerHighest
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { onEvent(SharedBillUiEvent.OnCloseClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(DesignR.string.designsystem_back),
                        )
                    }
                },
                actions = {
                    SharedBillTopBarAction(
                        state = state,
                        onEvent = onEvent,
                    )
                },
            )
        },
        bottomBar = {
            if (!usesTwoColumns && state.showsComposer) {
                BillComposerBar { composerModifier ->
                    SharedBillComposerBox(
                        state = state,
                        fieldColor = fieldColor,
                        onEvent = onEvent,
                        modifier = composerModifier,
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (state.status) {
            SharedBillStatus.LOADING,
            SharedBillStatus.OPEN,
            -> {
                val money = rememberMoneyFormatter(state.currency)
                val dates = rememberDateTimeTextFormatter()
                val composer: (@Composable () -> Unit)? = if (state.showsComposer) {
                    {
                        SharedBillComposerBox(
                            state = state,
                            fieldColor = fieldColor,
                            onEvent = onEvent,
                        )
                    }
                } else {
                    null
                }
                BillFullScreenColumn(
                    padding = padding,
                    usesTwoColumns = usesTwoColumns,
                    isLoaded = state.status == SharedBillStatus.OPEN,
                    banner = null,
                    first = {
                        SharedBillBillSection(
                            state = state,
                            wrapsAttachments = usesTwoColumns,
                            money = money,
                            dates = dates,
                            onEvent = onEvent,
                        )
                    },
                    second = {
                        SharedBillPeopleSection(
                            state = state,
                            money = money,
                            dates = dates,
                            onEvent = onEvent,
                        )
                    },
                    composer = composer,
                )
            }

            SharedBillStatus.NOT_FOUND,
            SharedBillStatus.MEMBERS_ONLY,
            SharedBillStatus.FAILED,
            -> SharedBillProblemBox(
                state = state,
                padding = padding,
                onEvent = onEvent,
            )
        }
    }
}

// Why: "Abrir no app" for a member with the bill on this device, "Entrar" while signing in may help.
@Composable
private fun SharedBillTopBarAction(
    state: SharedBillUiState,
    onEvent: (SharedBillUiEvent) -> Unit,
) {
    if (state.canOpenInApp) {
        TextButton(onClick = { onEvent(SharedBillUiEvent.OnOpenInAppClicked) }) {
            Text(stringResource(R.string.shared_bill_open_in_app))
        }
    }
    if (state.showsSignIn) {
        FilledTonalButton(
            onClick = { onEvent(SharedBillUiEvent.OnSignInClicked) },
            modifier = Modifier.padding(
                start = 4.dp,
                end = 12.dp,
            ),
        ) {
            Text(stringResource(R.string.shared_bill_sign_in))
        }
    }
}

/*
 * Why: the page's first column: "Esta conta é do grupo X" over the bill, then its attachments, with no "Sua parte"
 * (the page shows nobody's balance) and no "Anexos" at all for a bill with none, as nothing is added here.
 */
@Composable
private fun SharedBillBillSection(
    state: SharedBillUiState,
    wrapsAttachments: Boolean,
    money: MoneyFormatter,
    dates: DateTimeTextFormatter,
    onEvent: (SharedBillUiEvent) -> Unit,
) {
    InfoNoteBanner(
        text = stringResource(R.string.shared_bill_group_banner, state.groupName),
        modifier = Modifier.fillMaxWidth(),
    )
    BillSummaryColumn(
        category = state.category,
        categoryParent = state.categoryParent,
        description = state.description,
        spentAtText = spentAtTextOf(state.spentAt, state.spentDay, dates),
        totalText = money.toText(state.totalMinor),
        payers = state.payers,
        onLonePayerClick = null,
    )
    if (state.attachments.isNotEmpty()) {
        BillAttachmentsSection(
            attachments = state.attachments,
            countText = null,
            wraps = wrapsAttachments,
            dates = dates,
            addPhoto = null,
            onAttachmentClick = { onEvent(SharedBillUiEvent.OnAttachmentClicked(it)) },
        )
    }
}

/*
 * Why: the page's second column: each one's part and the comments, with no Histórico. Nobody opens a profile from
 * here, the heart, Editar and Apagar wait while the server can't be reached (`canChangeComments`), and "Responder"
 * shows only with a composer to answer in.
 */
@Composable
private fun SharedBillPeopleSection(
    state: SharedBillUiState,
    money: MoneyFormatter,
    dates: DateTimeTextFormatter,
    onEvent: (SharedBillUiEvent) -> Unit,
) {
    val comments = remember(onEvent, state.canChangeComments, state.showsComposer) {
        BillCommentCallbacks(
            canChange = state.canChangeComments,
            onPersonClick = null,
            onReplyClick = if (state.showsComposer) {
                { onEvent(SharedBillUiEvent.OnReplyClicked(it)) }
            } else {
                null
            },
            onLikeClick = { onEvent(SharedBillUiEvent.OnCommentLikeClicked(it)) },
            onLikesClick = { onEvent(SharedBillUiEvent.OnCommentLikesClicked(it)) },
            onShareClick = { onEvent(SharedBillUiEvent.OnCommentShareClicked(it)) },
            onEditClick = { onEvent(SharedBillUiEvent.OnCommentEditClicked(it)) },
            onDeleteClick = { onEvent(SharedBillUiEvent.OnCommentDeleteClicked(it)) },
            onFocusedCommentShown = { onEvent(SharedBillUiEvent.OnFocusedCommentShown) },
        )
    }
    BillSplitSection(
        splitMode = state.splitMode,
        participantCount = state.participantCount,
        people = state.people,
        money = money,
        onPersonClick = null,
    )
    BillCommentsSection(
        threads = state.threads,
        isLoading = false,
        commentCount = state.commentCount,
        showsNoComments = state.showsNoComments,
        replyTargetId = state.replyTarget?.commentId,
        focusedCommentId = state.focusedCommentId,
        dates = dates,
        callbacks = comments,
    )
    if (state.showsSignInToComment) {
        SignInToCommentCard(onSignInClick = { onEvent(SharedBillUiEvent.OnSignInClicked) })
    }
}

// Why: "Quer comentar? Entre com uma conta. Não precisa estar no grupo." in the composer's place while signed out.
@Composable
private fun SignInToCommentCard(onSignInClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = groupedRowShapeOf(
            index = 0,
            count = 1,
        ),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.shared_bill_sign_in_to_comment_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onSignInClick) {
                Text(stringResource(R.string.shared_bill_sign_in_to_comment))
            }
        }
    }
}

// Why: the page's composer goes straight to the server, with nobody's face: nobody is this device's user on the page.
@Composable
private fun SharedBillComposerBox(
    state: SharedBillUiState,
    fieldColor: Color,
    onEvent: (SharedBillUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    BillCommentComposerBox(
        composer = BillComposer(
            draft = state.commentDraft,
            replyTarget = state.replyTarget,
            editingCommentId = state.editingCommentId,
            canSend = state.canSendComment,
            onDraftChange = { onEvent(SharedBillUiEvent.OnCommentChanged(it)) },
            onSendClick = { onEvent(SharedBillUiEvent.OnSendCommentClicked) },
            onReplyCancel = { onEvent(SharedBillUiEvent.OnReplyCancelled) },
            onEditCancel = { onEvent(SharedBillUiEvent.OnCommentEditCancelled) },
        ),
        me = null,
        fieldColor = fieldColor,
        modifier = modifier,
    )
}

/*
 * Why: a link that doesn't open, in one centered column at every size as a profile's link page: why, what may help
 * ("Entrar", "Tentar de novo") and "Ir para o Bora Rachar", the way out to the app under the page.
 */
@Composable
private fun SharedBillProblemBox(
    state: SharedBillUiState,
    padding: PaddingValues,
    onEvent: (SharedBillUiEvent) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .consumeWindowInsets(padding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = problemMaxWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 24.dp,
                    vertical = 32.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (state.status) {
                SharedBillStatus.NOT_FOUND -> LinkProblemMessage(
                    icon = Icons.Outlined.LinkOff,
                    title = stringResource(R.string.shared_bill_not_found_title),
                    body = stringResource(R.string.shared_bill_not_found_body),
                )

                // Why: signing in helps until the device has a Google or Apple account; after that, only an invite.
                SharedBillStatus.MEMBERS_ONLY -> {
                    LinkProblemMessage(
                        icon = Icons.Outlined.Lock,
                        title = stringResource(R.string.shared_bill_members_only_title),
                        body = stringResource(
                            if (state.showsSignIn) {
                                R.string.shared_bill_members_only_body
                            } else {
                                R.string.shared_bill_members_only_outside_body
                            },
                        ),
                    )
                    if (state.showsSignIn) {
                        Button(onClick = { onEvent(SharedBillUiEvent.OnSignInClicked) }) {
                            Text(stringResource(R.string.shared_bill_sign_in))
                        }
                    }
                }

                SharedBillStatus.FAILED -> {
                    LinkProblemMessage(
                        icon = Icons.Outlined.CloudOff,
                        title = stringResource(R.string.shared_bill_failed_title),
                        body = if (state.isOnline) null else stringResource(R.string.shared_bill_offline),
                    )
                    Button(onClick = { onEvent(SharedBillUiEvent.OnRetryClicked) }) {
                        Text(stringResource(R.string.shared_bill_retry))
                    }
                }

                SharedBillStatus.LOADING,
                SharedBillStatus.OPEN,
                -> Unit
            }
            TextButton(onClick = { onEvent(SharedBillUiEvent.OnCloseClicked) }) {
                Text(stringResource(R.string.shared_bill_go_to_app))
            }
        }
    }
}
