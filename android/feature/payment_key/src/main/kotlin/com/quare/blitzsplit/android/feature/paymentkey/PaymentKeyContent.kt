package com.quare.blitzsplit.android.feature.paymentkey

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.layout.AdaptiveScreen
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.PaymentKeyError
import com.quare.blitzsplit.core.model.PaymentKeyFormat
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.feature.paymentkey.presentation.model.PaymentKeyUiEvent
import com.quare.blitzsplit.feature.paymentkey.presentation.model.PaymentKeyUiState
import com.quare.blitzsplit.android.designsystem.R as DesignR

private val singleColumnMaxWidth = 592.dp
private val dialogWidth = 560.dp
private val phoneLandscapeStartColumnWidth = 300.dp

@Composable
fun PaymentKeyContent(
    state: PaymentKeyUiState,
    field: TextFieldValue,
    // Why: a tablet draws the key as a 560dp dialog over the profile, so its layout is always one column.
    isDialog: Boolean,
    onFieldChange: (TextFieldValue) -> Unit,
    onPasteClick: () -> Unit,
    onEvent: (PaymentKeyUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val body = PaymentKeyBody(
        state = state,
        field = field,
        onFieldChange = onFieldChange,
        onPasteClick = onPasteClick,
        onEvent = onEvent,
    )
    if (isDialog) {
        PaymentKeyDialogSurface(
            body = body,
            modifier = modifier,
        )
    } else {
        PaymentKeyFullScreenScaffold(
            body = body,
            modifier = modifier,
        )
    }
}

private class PaymentKeyBody(
    val state: PaymentKeyUiState,
    val field: TextFieldValue,
    val onFieldChange: (TextFieldValue) -> Unit,
    val onPasteClick: () -> Unit,
    val onEvent: (PaymentKeyUiEvent) -> Unit,
)

@Composable
private fun PaymentKeyFullScreenScaffold(
    body: PaymentKeyBody,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            PaymentKeyTopBar(
                state = body.state,
                onEvent = body.onEvent,
                isDialog = false,
            )
        },
    ) { padding ->
        AdaptiveScreen(modifier = Modifier.fillMaxSize().padding(padding).imePadding()) { layout ->
            when {
                // Why: a saved key fills the form once read, so the form doesn't flash empty first.
                !body.state.isLoaded -> Unit

                layout.usesTwoColumns -> PaymentKeyTwoColumnLayout(
                    body = body,
                    isBookPosture = layout.isTablet,
                )

                else -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = singleColumnMaxWidth)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 32.dp,
                            ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) { PaymentKeyFormColumn(body = body) }
                }
            }
        }
    }
}

@Composable
private fun PaymentKeyDialogSurface(
    body: PaymentKeyBody,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .widthIn(max = dialogWidth)
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column {
            PaymentKeyTopBar(
                state = body.state,
                onEvent = body.onEvent,
                isDialog = true,
            )
            if (body.state.isLoaded) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 24.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) { PaymentKeyFormColumn(body = body) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentKeyTopBar(
    state: PaymentKeyUiState,
    onEvent: (PaymentKeyUiEvent) -> Unit,
    isDialog: Boolean,
) {
    TopAppBar(
        title = { Text(stringResource(state.titleRes)) },
        navigationIcon = {
            IconButton(onClick = { onEvent(PaymentKeyUiEvent.OnBackClicked) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(DesignR.string.designsystem_back),
                )
            }
        },
        actions = {
            Button(
                onClick = { onEvent(PaymentKeyUiEvent.OnSaveClicked) },
                enabled = state.canSave,
                modifier = Modifier.padding(end = 12.dp),
            ) { Text(stringResource(R.string.payment_key_save)) }
        },
        windowInsets = if (isDialog) WindowInsets(0) else TopAppBarDefaults.windowInsets,
        colors = if (isDialog) {
            TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        } else {
            TopAppBarDefaults.topAppBarColors()
        },
    )
}

@Composable
private fun ColumnScope.PaymentKeyFormColumn(body: PaymentKeyBody) {
    val state = body.state
    val isPix = state.method == PaymentMethod.PIX
    Text(
        text = stringResource(if (isPix) R.string.payment_key_pix_intro else R.string.payment_key_bitcoin_intro),
        modifier = Modifier.padding(horizontal = 8.dp),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    PaymentKeyTypeGrid(
        state = state,
        columns = if (isPix) 4 else 2,
        isCompact = false,
        onEvent = body.onEvent,
    )
    PaymentKeyFieldSection(
        state = state,
        field = body.field,
        onFieldChange = body.onFieldChange,
        onPasteClick = body.onPasteClick,
        onEvent = body.onEvent,
        modifier = Modifier.padding(
            start = 8.dp,
            end = 8.dp,
            top = 12.dp,
        ),
    )
    if (state.showsCpfNotice) CpfNoticeCard()
    if (!isPix) AcceptedFormatsCard(type = state.type)
    DefaultKeySwitchRow(
        state = state,
        onEvent = body.onEvent,
    )
    if (state.isEditing) DeleteKeyButton(onEvent = body.onEvent)
    PaymentKeyFooter(
        method = state.method,
        modifier = Modifier.padding(
            start = 8.dp,
            end = 8.dp,
            top = 8.dp,
        ),
    )
}

/*
 * Why: lying down, the kind of key goes left and the field right. On an unfolded foldable as a book the two
 * columns are equal, so the 72dp gap (the 24dp hinge plus 24dp on each side) sits on the hinge.
 */
@Composable
private fun PaymentKeyTwoColumnLayout(
    body: PaymentKeyBody,
    isBookPosture: Boolean,
) {
    val state = body.state
    val isPix = state.method == PaymentMethod.PIX
    Row(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = if (isBookPosture) 32.dp else 24.dp),
        horizontalArrangement = Arrangement.spacedBy(if (isBookPosture) 72.dp else 40.dp),
    ) {
        Column(
            modifier = Modifier
                .then(if (isBookPosture) Modifier.weight(1f) else Modifier.width(phoneLandscapeStartColumnWidth))
                .verticalScroll(rememberScrollState())
                .padding(vertical = if (isBookPosture) 24.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val headingStyle = if (isBookPosture) {
                MaterialTheme.typography.headlineLarge
            } else {
                MaterialTheme.typography.headlineSmall
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(
                        if (isPix) R.string.payment_key_pix_question else R.string.payment_key_bitcoin_intro,
                    ),
                    style = headingStyle,
                    fontWeight = FontWeight.Bold,
                )
                if (isPix) {
                    Text(
                        text = stringResource(R.string.payment_key_pix_explanation),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            PaymentKeyTypeGrid(
                state = state,
                columns = if (isPix) 2 else 1,
                isCompact = !isBookPosture,
                onEvent = body.onEvent,
            )
            if (!isPix) AcceptedFormatsCard(type = state.type)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = if (isBookPosture) 24.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PaymentKeyFieldSection(
                state = state,
                field = body.field,
                onFieldChange = body.onFieldChange,
                onPasteClick = body.onPasteClick,
                onEvent = body.onEvent,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (state.showsCpfNotice) CpfNoticeCard()
            DefaultKeySwitchRow(
                state = state,
                onEvent = body.onEvent,
            )
            if (state.isEditing) DeleteKeyButton(onEvent = body.onEvent)
            PaymentKeyFooter(
                method = state.method,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    }
}

private fun createPreviewState(
    method: PaymentMethod,
    type: PaymentKeyType,
    input: String,
    format: PaymentKeyFormat?,
    error: PaymentKeyError?,
    isEditing: Boolean = false,
): PaymentKeyUiState = PaymentKeyUiState(
    method = method,
    isEditing = isEditing,
    isLoaded = true,
    type = type,
    input = input,
    format = format,
    error = error,
    hasSwitchedNetwork = false,
    isDefault = error == null,
    currentDefaultType = if (method == PaymentMethod.PIX) PaymentKeyType.PHONE else null,
    isDefaultLocked = false,
    isSaving = false,
    isDeleteConfirmationShown = false,
)

@Composable
private fun PaymentKeyPreviewContent(
    state: PaymentKeyUiState,
    isDialog: Boolean = false,
) {
    PaymentKeyContent(
        state = state,
        field = TextFieldValue(
            text = state.input,
            selection = TextRange(state.input.length),
        ),
        isDialog = isDialog,
        onFieldChange = {},
        onPasteClick = {},
        onEvent = {},
    )
}

@DevicePreviews
@Composable
private fun PaymentKeyPixPreview() {
    BlitzSplitTheme {
        PaymentKeyPreviewContent(
            state = createPreviewState(
                method = PaymentMethod.PIX,
                type = PaymentKeyType.CPF,
                input = "123.456.789-09",
                format = PaymentKeyFormat.CPF,
                error = null,
            ),
        )
    }
}

@DevicePreviews
@Composable
private fun PaymentKeyBitcoinErrorPreview() {
    BlitzSplitTheme {
        PaymentKeyPreviewContent(
            state = createPreviewState(
                method = PaymentMethod.BITCOIN,
                type = PaymentKeyType.ONCHAIN,
                input = "tb1qw508d6qejxtdg4y5r3zarvary0c5xw7kxpjzsx",
                format = null,
                error = PaymentKeyError.TESTNET_ADDRESS,
                isEditing = true,
            ),
        )
    }
}

@Preview(
    name = "Tablet dialog",
    device = "spec:width=1280dp,height=800dp,orientation=landscape",
)
@Composable
private fun PaymentKeyDialogPreview() {
    BlitzSplitTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)),
            contentAlignment = Alignment.Center,
        ) {
            PaymentKeyPreviewContent(
                state = createPreviewState(
                    method = PaymentMethod.BITCOIN,
                    type = PaymentKeyType.LIGHTNING,
                    input = "pierre@carteira.app",
                    format = PaymentKeyFormat.LIGHTNING_ADDRESS,
                    error = null,
                ),
                isDialog = true,
            )
        }
    }
}
