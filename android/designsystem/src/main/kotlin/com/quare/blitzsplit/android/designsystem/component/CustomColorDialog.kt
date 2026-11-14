package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.android.designsystem.layout.DialogPlacement
import com.quare.blitzsplit.android.designsystem.theme.rgbOf
import com.quare.blitzsplit.core.expenses.customcolor.CustomColorEvent
import com.quare.blitzsplit.core.expenses.domain.model.ColorDraft
import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import kotlin.math.roundToInt

private const val FULL_TURN = 360f

/*
 * Why: "New color", which "+" opens: a gradient to pick from (saturation and brightness in the square, the hue below it),
 * or the color typed as a hex or an RGB code, each following the others. The ViewModel holds the draft, so the fields
 * and the gradient never disagree; "Add" waits for whole codes.
 */
@Composable
internal fun CustomColorDialog(
    draft: ColorDraft,
    onEvent: (CustomColorEvent) -> Unit,
    placement: DialogPlacement,
) {
    FoldAlertDialog(
        onDismissRequest = { onEvent(CustomColorEvent.OnDraftDismissed) },
        title = stringResource(R.string.designsystem_custom_color_title),
        placement = placement,
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ShadeBox(
                    draft = draft,
                    onEvent = onEvent,
                )
                HueBar(
                    draft = draft,
                    onEvent = onEvent,
                )
                CodeSection(
                    draft = draft,
                    onEvent = onEvent,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onEvent(CustomColorEvent.OnSaveClicked) },
                enabled = draft.canSave,
            ) { Text(stringResource(R.string.designsystem_custom_color_save)) }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(CustomColorEvent.OnDraftDismissed) }) {
                Text(stringResource(R.string.designsystem_confirm_cancel))
            }
        },
    )
}

// Why: saturation left to right, brightness bottom to top, on the hue picked below.
@Composable
private fun ShadeBox(
    draft: ColorDraft,
    onEvent: (CustomColorEvent) -> Unit,
) {
    val hueColor = rgbOf(CustomColor.fromHsv(draft.hue, 1.0, 1.0).rgb)
    val label = stringResource(R.string.designsystem_custom_color_shade)
    val state = "${(draft.saturation * 100).roundToInt()}%, ${(draft.value * 100).roundToInt()}%"

    fun pick(
        offset: Offset,
        size: IntSize,
    ) {
        if (size.width == 0 || size.height == 0) return
        onEvent(
            CustomColorEvent.OnHsvChanged(
                hue = draft.hue,
                saturation = (offset.x / size.width).toDouble().coerceIn(0.0, 1.0),
                value = (1 - offset.y / size.height).toDouble().coerceIn(0.0, 1.0),
            ),
        )
    }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(MaterialTheme.shapes.medium)
            .semantics {
                contentDescription = label
                stateDescription = state
            }.pointerInput(draft.hue) { detectTapGestures { pick(it, size) } }
            .pointerInput(draft.hue) { detectDragGestures { change, _ -> pick(change.position, size) } },
    ) {
        drawRect(Brush.horizontalGradient(listOf(Color.White, hueColor)))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        val center = Offset(
            x = (draft.saturation * size.width).toFloat(),
            y = ((1 - draft.value) * size.height).toFloat(),
        )
        drawCircle(
            Color.White,
            radius = 10.dp.toPx(),
            center = center,
            style = Stroke(3.dp.toPx()),
        )
        drawCircle(
            Color.Black,
            radius = 12.dp.toPx(),
            center = center,
            style = Stroke(1.dp.toPx()),
        )
    }
}

@Composable
private fun HueBar(
    draft: ColorDraft,
    onEvent: (CustomColorEvent) -> Unit,
) {
    val hues = (0..6).map { rgbOf(CustomColor.fromHsv(it * 60.0, 1.0, 1.0).rgb) }
    val label = stringResource(R.string.designsystem_custom_color_hue)
    val state = "${draft.hue.roundToInt()}°"

    fun pick(
        x: Float,
        width: Int,
    ) {
        if (width == 0) return
        onEvent(
            CustomColorEvent.OnHsvChanged(
                hue = (x / width * FULL_TURN).toDouble().coerceIn(0.0, FULL_TURN.toDouble()),
                saturation = draft.saturation,
                value = draft.value,
            ),
        )
    }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(CircleShape)
            .semantics {
                contentDescription = label
                stateDescription = state
            }.pointerInput(draft.saturation, draft.value) { detectTapGestures { pick(it.x, size.width) } }
            .pointerInput(draft.saturation, draft.value) {
                detectDragGestures { change, _ -> pick(change.position.x, size.width) }
            },
    ) {
        drawRect(Brush.horizontalGradient(hues))
        val x = (draft.hue / FULL_TURN * size.width).toFloat().coerceIn(0f, size.width)
        drawCircle(
            Color.White,
            radius = size.height / 2 - 2.dp.toPx(),
            center = Offset(x, size.height / 2),
            style = Stroke(3.dp.toPx()),
        )
    }
}

// Why: the color as the person would type it from elsewhere: a hex code, or red, green and blue from 0 to 255.
@Composable
private fun CodeSection(
    draft: ColorDraft,
    onEvent: (CustomColorEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(rgbOf(draft.color.rgb))
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            )
            OutlinedTextField(
                value = draft.hexText,
                onValueChange = { onEvent(CustomColorEvent.OnHexChanged(it)) },
                label = { Text(stringResource(R.string.designsystem_custom_color_hex)) },
                isError = draft.isHexInvalid,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Ascii,
                    autoCorrectEnabled = false,
                ),
                supportingText = if (draft.isHexInvalid) {
                    { Text(stringResource(R.string.designsystem_custom_color_hex_error)) }
                } else {
                    null
                },
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChannelField(
                text = draft.redText,
                label = stringResource(R.string.designsystem_category_color_red),
                isError = draft.isRgbInvalid,
                onChange = { onEvent(CustomColorEvent.OnChannelChanged(ColorDraft.ColorChannel.RED, it)) },
                modifier = Modifier.weight(1f),
            )
            ChannelField(
                text = draft.greenText,
                label = stringResource(R.string.designsystem_category_color_green),
                isError = draft.isRgbInvalid,
                onChange = { onEvent(CustomColorEvent.OnChannelChanged(ColorDraft.ColorChannel.GREEN, it)) },
                modifier = Modifier.weight(1f),
            )
            ChannelField(
                text = draft.blueText,
                label = stringResource(R.string.designsystem_category_color_blue),
                isError = draft.isRgbInvalid,
                onChange = { onEvent(CustomColorEvent.OnChannelChanged(ColorDraft.ColorChannel.BLUE, it)) },
                modifier = Modifier.weight(1f),
            )
        }
        if (draft.isRgbInvalid) {
            Text(
                text = stringResource(R.string.designsystem_custom_color_rgb_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ChannelField(
    text: String,
    label: String,
    isError: Boolean,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = text,
        onValueChange = onChange,
        label = { Text(label, maxLines = 1) },
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}
