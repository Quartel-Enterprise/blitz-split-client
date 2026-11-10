package com.quare.blitzsplit.android.designsystem.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLocale

private const val JAPANESE = "ja"
private val unspacedLanguages = setOf(JAPANESE)

/*
 * Why: a sentence the code joins from pieces ("Ana" + "changed the category", two sentences of a detail) puts a space
 * between them, but Japanese writes them with none ("Anaがカテゴリを変更しました") and ends a sentence with "。", not
 * ".". Same rule as the web's `wordGap` and `fullStop`.
 */
@Composable
fun wordGapUi(): String = if (isUnspacedUi()) "" else " "

@Composable
fun fullStopUi(): String = if (isUnspacedUi()) "。" else "."

@Composable
private fun isUnspacedUi(): Boolean = LocalLocale.current.platformLocale.language in unspacedLanguages
