package com.quare.blitzsplit.android.feature.paymentkey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.feature.paymentkey.presentation.model.PaymentKeyUiEvent
import com.quare.blitzsplit.feature.paymentkey.presentation.model.PaymentKeyUiState

@Composable
internal fun PaymentKeyFieldSection(
    state: PaymentKeyUiState,
    field: TextFieldValue,
    onFieldChange: (TextFieldValue) -> Unit,
    onPasteClick: () -> Unit,
    onEvent: (PaymentKeyUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isBitcoin = state.method == PaymentMethod.BITCOIN
    var wasFocused by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        OutlinedTextField(
            value = field,
            onValueChange = { value -> onFieldChange(if (isBitcoin) value.toSingleLine() else value) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focus ->
                    if (wasFocused && !focus.isFocused) onEvent(PaymentKeyUiEvent.OnInputFocusLost)
                    wasFocused = focus.isFocused
                },
            label = { Text(stringResource(state.type.fieldLabelRes)) },
            isError = state.error != null,
            singleLine = !isBitcoin,
            minLines = if (isBitcoin) 2 else 1,
            textStyle = if (isBitcoin) {
                LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                )
            } else {
                LocalTextStyle.current.copy(fontSize = if (state.type == PaymentKeyType.RANDOM) 14.sp else 17.sp)
            },
            trailingIcon = if (isBitcoin) {
                null
            } else {
                {
                    IconButton(onClick = onPasteClick) {
                        Icon(
                            imageVector = Icons.Outlined.ContentPaste,
                            contentDescription = stringResource(R.string.payment_key_paste),
                        )
                    }
                }
            },
            // Why: the Bitcoin field wraps over several lines, but a key has no line break: Done saves, as on iOS.
            keyboardOptions = state.type.keyboardOptions.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onEvent(PaymentKeyUiEvent.OnSaveClicked) }),
        )
        FieldHelperMessage(state = state)
        if (state.hasSwitchedNetwork) {
            HelperLine(
                icon = Icons.Outlined.SwapHoriz,
                iconTint = MaterialTheme.colorScheme.primary,
                text = stringResource(
                    if (state.type == PaymentKeyType.ONCHAIN) {
                        R.string.payment_key_switched_to_onchain
                    } else {
                        R.string.payment_key_switched_to_lightning
                    },
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isBitcoin) {
            OutlinedButton(
                onClick = onPasteClick,
                modifier = Modifier.padding(top = 12.dp),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 16.dp,
                ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentPaste,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.payment_key_paste),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun FieldHelperMessage(state: PaymentKeyUiState) {
    val error = state.error
    val format = state.format
    when {
        error != null -> HelperLine(
            icon = Icons.Outlined.ErrorOutline,
            iconTint = MaterialTheme.colorScheme.error,
            text = stringResource(error.messageRes),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )

        format != null -> HelperLine(
            icon = Icons.Outlined.Check,
            iconTint = MaterialTheme.colorScheme.primary,
            text = stringResource(format.validRes),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        else -> HelperLine(
            icon = null,
            iconTint = Color.Unspecified,
            text = stringResource(state.type.hintRes),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HelperLine(
    icon: ImageVector?,
    iconTint: Color,
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(
            start = 16.dp,
            end = 16.dp,
            top = 6.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(16.dp),
                tint = iconTint,
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color,
        )
    }
}

// Why: a hardware Enter or a pasted line break would otherwise land inside the key.
private fun TextFieldValue.toSingleLine(): TextFieldValue {
    if ('\n' !in text) return this
    val cursor = selection.end - text.take(selection.end).count { it == '\n' }
    return TextFieldValue(
        text = text.replace("\n", ""),
        selection = TextRange(cursor),
    )
}
