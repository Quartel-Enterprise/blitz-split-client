package com.quare.blitzsplit.android.designsystem.layout

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

// Why: the frames of the canvas (phone 412×915, tablet 1280×800), upright, lying down and dark.
@Preview(
    name = "Phone",
    device = "spec:width=412dp,height=915dp",
)
@Preview(
    name = "Phone dark",
    device = "spec:width=412dp,height=915dp",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Preview(
    name = "Phone landscape",
    device = "spec:width=915dp,height=412dp,orientation=landscape",
)
@Preview(
    name = "Tablet",
    device = "spec:width=1280dp,height=800dp,orientation=landscape",
)
@Preview(
    name = "Tablet portrait",
    device = "spec:width=800dp,height=1280dp",
)
annotation class DevicePreviews
