package com.quare.blitzsplit.feature.groups.presentation.mapper

// Why: a stack of faces shows up to three, the rest as "+N": a group's members on its row, who paid a bill on Início.
internal const val MAX_FACES = 3

internal fun extraFacesOf(count: Int): Int = (count - MAX_FACES).coerceAtLeast(0)
