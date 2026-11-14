@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.model.FaceStack
import com.quare.blitzsplit.feature.groups.presentation.model.HomePeopleLayout

// Why: FaceStack's two bubbles, the same rule Android and Apple draw a stack of faces by.
fun getStackedFaceCount(peopleCount: Int): Int = FaceStack.faceCountOf(peopleCount)

fun getStackedExtraCount(peopleCount: Int): Int = FaceStack.extraCountOf(peopleCount)

class JsHomePeopleLayout(
    val showsRows: Boolean,
    val columns: Int,
)

// Why: Início's "Saldos com pessoas" as rows or faces, by the section's width in CSS px and the count.
fun getHomePeopleLayout(
    peopleCount: Int,
    sectionWidth: Double,
): JsHomePeopleLayout {
    val layout = HomePeopleLayout.of(peopleCount, sectionWidth.toFloat())
    return JsHomePeopleLayout(
        showsRows = layout.showsRows,
        columns = layout.columns,
    )
}
