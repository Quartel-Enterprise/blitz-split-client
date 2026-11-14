package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: "New color", the picker "+" opens: a gradient (hue, then saturation and brightness) and the same color as a hex
 * code and as RGB, each following the others. A code being typed doesn't move the gradient until it is a whole color;
 * a gray keeps the hue the gradient was on, so the hue slider never jumps.
 */
data class ColorDraft(
    val hue: Double,
    val saturation: Double,
    val value: Double,
    val hexText: String,
    val redText: String,
    val greenText: String,
    val blueText: String,
) {
    /*
     * Why: what "Add" saves and the preview shows: the code as typed, exactly, else the gradient's color while the code
     * is half typed. The hex field always holds the whole color the gradient or the RGB fields last set.
     */
    val color: CustomColor
        get() = CustomColor.fromHexOrNull(hexText) ?: CustomColor.fromHsv(hue, saturation, value)

    // Why: "Enter a code like #3A7BD5" only once the code can't become one: a character that isn't hex, or too many.
    val isHexInvalid: Boolean
        get() {
            val digits = hexText.trim().removePrefix("#")
            return digits.length > HEX_DIGITS || !digits.all { it.isHexDigit() }
        }

    // Why: "Each value goes from 0 to 255", once one of them is past it.
    val isRgbInvalid: Boolean
        get() = channelTexts.any { text -> text.toIntOrNull()?.let { it > CHANNEL_MAX } == true }

    // Why: "Add" waits for whole codes, so a half-typed one never saves the color it is replacing.
    val canSave: Boolean
        get() = CustomColor.fromHexOrNull(hexText) != null && channelTexts.all { it.toIntOrNull() in 0..CHANNEL_MAX }

    private val channelTexts: List<String>
        get() = listOf(redText, greenText, blueText)

    fun withHsv(
        hue: Double,
        saturation: Double,
        value: Double,
    ): ColorDraft = of(
        hue = hue.coerceIn(0.0, MAX_HUE),
        saturation = saturation.coerceIn(0.0, 1.0),
        value = value.coerceIn(0.0, 1.0),
    )

    // Why: what's typed stays as typed (up to "#" and six digits); a whole code moves the gradient and the RGB fields.
    fun withHexText(text: String): ColorDraft {
        val typed = text.trim().take(MAX_HEX_LENGTH)
        val color = CustomColor.fromHexOrNull(typed) ?: return copy(hexText = typed)
        return createFollowing(color).copy(hexText = typed)
    }

    // Why: digits only, three at most; three whole channels move the gradient and the hex code.
    fun withChannelText(
        channel: ColorChannel,
        text: String,
    ): ColorDraft {
        val digits = text.filter { it.isDigit() }.take(MAX_CHANNEL_DIGITS)
        val typed = when (channel) {
            ColorChannel.RED -> copy(redText = digits)
            ColorChannel.GREEN -> copy(greenText = digits)
            ColorChannel.BLUE -> copy(blueText = digits)
        }
        val (red, green, blue) = typed.channelTexts.map { it.toIntOrNull() ?: return typed }
        val color = CustomColor.fromRgbOrNull(red, green, blue) ?: return typed
        return typed.createFollowing(color).copy(
            redText = typed.redText,
            greenText = typed.greenText,
            blueText = typed.blueText,
        )
    }

    // Why: a typed color, placed on the gradient; a gray or a black keeps the hue (and a black the saturation) shown.
    private fun createFollowing(color: CustomColor): ColorDraft {
        val hsv = color.toHsv()
        val isGray = hsv.saturation == 0.0 || hsv.value == 0.0
        return of(
            hue = if (isGray) hue else hsv.hue,
            saturation = if (hsv.value == 0.0) saturation else hsv.saturation,
            value = hsv.value,
        ).copy(hexText = color.hex)
    }

    enum class ColorChannel {
        RED,
        GREEN,
        BLUE,
    }

    companion object {
        private const val HEX_DIGITS = 6
        private const val MAX_HEX_LENGTH = HEX_DIGITS + 1
        private const val MAX_CHANNEL_DIGITS = 3
        private const val CHANNEL_MAX = 255
        private const val MAX_HUE = 360.0

        // Why: the picker opens on the category's color, so a new one starts near the one it replaces.
        fun createStartingAt(tint: CategoryTint): ColorDraft {
            val color = when (tint) {
                is CategoryTint.Named -> CustomColor(tint.color.baseRgb)
                is CategoryTint.Custom -> tint.color
            }
            val hsv = color.toHsv()
            return of(hsv.hue, hsv.saturation, hsv.value)
        }

        private fun of(
            hue: Double,
            saturation: Double,
            value: Double,
        ): ColorDraft {
            val color = CustomColor.fromHsv(hue, saturation, value)
            return ColorDraft(
                hue = hue,
                saturation = saturation,
                value = value,
                hexText = color.hex,
                redText = color.red.toString(),
                greenText = color.green.toString(),
                blueText = color.blue.toString(),
            )
        }
    }
}
