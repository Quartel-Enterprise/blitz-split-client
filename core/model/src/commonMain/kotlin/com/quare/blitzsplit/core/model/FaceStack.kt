package com.quare.blitzsplit.core.model

/*
 * Why: how many bubbles a stack of faces shows, the same on the three UIs (Pierre's call). Up to maxBubbles people all
 * show their faces; more than that show one face fewer and a last bubble "+N" for the rest, so "+N" never stands for
 * one person alone.
 */
enum class FaceStack(
    val maxBubbles: Int,
) {
    /*
     * Why: a stack in a list's row or card or in a form's field, which share their line with a name or an amount (beta
     * feedback: more read squeezed there). Three people: a face and "+2".
     */
    COMPACT(2),

    /*
     * Why: a stack on a screen's own header or body, alone on its line or beside a sentence free to wrap: under a
     * group's name, on the invite, on the Bill. Five people: the five faces; six: four faces and "+2".
     */
    ROOMY(5),
    ;

    // Why: how many of the first people show their face.
    fun faceCountOf(peopleCount: Int): Int = when {
        peopleCount <= 0 -> 0
        peopleCount <= maxBubbles -> peopleCount
        else -> maxBubbles - 1
    }

    // Why: the "+N" bubble's N, 0 for no bubble.
    fun extraCountOf(peopleCount: Int): Int = (peopleCount - faceCountOf(peopleCount)).coerceAtLeast(0)
}

// Why: the first people whose faces a stack shows; the rest is FaceStack.extraCountOf.
fun <T> List<T>.toStackedFaces(stack: FaceStack): List<T> = take(stack.faceCountOf(size))
