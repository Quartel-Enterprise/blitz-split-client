package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.quare.blitzsplit.android.designsystem.R

// Why: the brand badge (the welcome's shapes, the tablet pitch, the Flip cover) draws the app's logo in its tile's color.
@Composable
fun logoIcon(): ImageVector = ImageVector.vectorResource(R.drawable.ic_logo)
