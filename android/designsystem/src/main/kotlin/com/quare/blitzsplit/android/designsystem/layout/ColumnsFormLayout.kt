package com.quare.blitzsplit.android.designsystem.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.DialogCard

private val singleColumnMaxWidth = 592.dp
private val columnSpacing = 20.dp

// Why: what the top bar (or the dialog's header and actions) of Rachar conta, Dividir and Divisão padrão hold.
class FormChrome(
    val title: String,
    // Why: "Apê 302 · para as despesas novas" under Divisão padrão's title; null for none.
    val subtitle: String?,
    val navigationIcon: ImageVector,
    val navigationLabel: String,
    val onNavigate: () -> Unit,
    val confirmLabel: String,
    val isConfirmEnabled: Boolean,
    val onConfirm: () -> Unit,
    val cancelLabel: String,
    // Why: Rachar conta's dialog closes from its header's end, Dividir's goes back from its start, as the canvas has.
    val isNavigationAtEndInDialog: Boolean,
)

/*
 * Why: the canvas opens Rachar conta, Dividir and Divisão padrão full screen on a phone, lying down and on the
 * tabletop, in two columns when lying down; as a dialog (where the caller says so) they sit over the group, two
 * columns wide on a tablet lying down.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnsFormLayout(
    chrome: FormChrome,
    isDialog: Boolean,
    snackbarHostState: SnackbarHostState,
    first: @Composable ColumnScope.() -> Unit,
    second: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val layout = rememberDeviceLayout()
    val usesTwoColumns = if (isDialog) layout.isTablet && layout.isLandscape else layout.isLandscape
    if (isDialog) {
        DialogCard(
            isWide = usesTwoColumns,
            spacing = columnSpacing,
            snackbarHostState = snackbarHostState,
            modifier = modifier,
            cardModifier = Modifier.imePadding(),
        ) {
            DialogHeader(chrome)
            FormColumnsLayout(
                usesTwoColumns = usesTwoColumns,
                contentPadding = PaddingValues(0.dp),
                first = first,
                second = second,
                modifier = Modifier.weight(
                    weight = 1f,
                    fill = false,
                ),
            )
            DialogActionsRow(chrome)
        }
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { FormTitleColumn(chrome) },
                    navigationIcon = {
                        IconButton(onClick = chrome.onNavigate) {
                            Icon(
                                imageVector = chrome.navigationIcon,
                                contentDescription = chrome.navigationLabel,
                            )
                        }
                    },
                    actions = {
                        Button(
                            onClick = chrome.onConfirm,
                            enabled = chrome.isConfirmEnabled,
                            modifier = Modifier.padding(end = 12.dp),
                        ) { Text(chrome.confirmLabel) }
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding(),
                contentAlignment = Alignment.TopCenter,
            ) {
                FormColumnsLayout(
                    usesTwoColumns = usesTwoColumns,
                    contentPadding = if (usesTwoColumns) {
                        PaddingValues(
                            start = 56.dp,
                            end = 24.dp,
                            top = 4.dp,
                            bottom = 16.dp,
                        )
                    } else {
                        PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 24.dp,
                        )
                    },
                    first = first,
                    second = second,
                    modifier = if (usesTwoColumns) Modifier else Modifier.widthIn(max = singleColumnMaxWidth),
                )
            }
        }
    }
}

@Composable
private fun FormColumnsLayout(
    usesTwoColumns: Boolean,
    contentPadding: PaddingValues,
    first: @Composable ColumnScope.() -> Unit,
    second: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    if (usesTwoColumns) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(columnSpacing),
                content = first,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = second,
            )
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(columnSpacing),
        ) {
            first()
            second()
        }
    }
}

@Composable
private fun DialogHeader(chrome: FormChrome) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!chrome.isNavigationAtEndInDialog) {
            NavigationButton(chrome)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chrome.title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            chrome.subtitle?.let { FormSubtitleText(it) }
        }
        if (chrome.isNavigationAtEndInDialog) {
            NavigationButton(chrome)
        }
    }
}

@Composable
private fun NavigationButton(chrome: FormChrome) {
    IconButton(onClick = chrome.onNavigate) {
        Icon(
            imageVector = chrome.navigationIcon,
            contentDescription = chrome.navigationLabel,
        )
    }
}

@Composable
private fun DialogActionsRow(chrome: FormChrome) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.End,
        ),
    ) {
        TextButton(onClick = chrome.onNavigate) { Text(chrome.cancelLabel) }
        Button(
            onClick = chrome.onConfirm,
            enabled = chrome.isConfirmEnabled,
        ) { Text(chrome.confirmLabel) }
    }
}

@Composable
private fun FormTitleColumn(chrome: FormChrome) {
    Column {
        Text(chrome.title)
        chrome.subtitle?.let { FormSubtitleText(it) }
    }
}

@Composable
private fun FormSubtitleText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
