@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.model.FaceStack
import com.quare.blitzsplit.feature.groups.presentation.model.HomePeopleLayout

/*
 * Why: FaceStack's bubbles, the same rule Android and Apple draw a stack of faces by: ROOMY on a screen's own
 * header or body, COMPACT in a list's row or a form's field.
 */
fun getStackedFaceCount(
    peopleCount: Int,
    isRoomy: Boolean,
): Int = stackOf(isRoomy).faceCountOf(peopleCount)

fun getStackedExtraCount(
    peopleCount: Int,
    isRoomy: Boolean,
): Int = stackOf(isRoomy).extraCountOf(peopleCount)

private fun stackOf(isRoomy: Boolean): FaceStack = if (isRoomy) FaceStack.ROOMY else FaceStack.COMPACT

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
