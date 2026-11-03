package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.quare.blitzsplit.android.designsystem.R

/*
 * Why: every "Rachar conta" button (the home's FABs, the rail's, the group's) draws the legacy app's receipt with a
 * plus, more specific than a bare plus.
 */
@Composable
fun splitBillIcon(): ImageVector = ImageVector.vectorResource(R.drawable.ic_split_bill)
