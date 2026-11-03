package com.quare.blitzsplit.android.designsystem.layout

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable

/*
 * Why: every bottom sheet of the app opens fully expanded and closes by dragging down, never resting half way, which
 * Material's deprecated `rememberModalBottomSheetState(skipPartiallyExpanded = true)` said in one flag.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberFullSheetState(confirmValueChange: (SheetValue) -> Boolean = { true }): SheetState =
    rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        confirmValueChange = confirmValueChange,
    )
