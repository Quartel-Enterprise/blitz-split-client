package com.quare.blitzsplit.android.storescreenshots

// The Play devices Google Play's art draws (PlayArtCaptures): the capture folder and Robolectric's qualifiers.
internal enum class PlayArtDevice(
    val directory: String,
    val qualifiers: String,
) {
    PHONE("playphone", "w412dp-h892dp-port-xxhdpi"),
    TABLET_7("playtablet7", "w960dp-h600dp-land-xhdpi"),
    TABLET_10("playtablet10", "w1280dp-h800dp-land-xhdpi"),
}
