package com.quare.blitzsplit.android.storescreenshots

// The Play devices Google Play's art draws (PlayArtCaptures): the capture folder, Robolectric's qualifiers and the
// compose.mjs devices that are laid out from the capture (ART_DEVICES narrows the captures to the ones asked for).
internal enum class PlayArtDevice(
    val directory: String,
    val qualifiers: String,
    val artDevices: List<String>,
) {
    PHONE("playphone", "w412dp-h892dp-port-xxhdpi", listOf("play_phone")),
    TABLET_7("playtablet7", "w960dp-h600dp-land-xhdpi", listOf("play_tablet_7")),
    TABLET_10("playtablet10", "w1280dp-h800dp-land-xhdpi", listOf("play_tablet_10")),

    // A Pixel Fold's inner screen, unfolded: 864x720 dp (1.2:1) at 3x, which the 1:1 and the 4:3 art both draw.
    FOLDABLE_UNFOLDED("playfoldable", "w864dp-h720dp-land-xxhdpi", listOf("play_foldable_1x1", "play_foldable_4x3")),

    // Its outer screen, folded, 17.4:9: a narrow phone.
    FOLDABLE_FOLDED("playfoldablefolded", "w412dp-h800dp-port-xxhdpi", listOf("play_foldable_outer")),
}
