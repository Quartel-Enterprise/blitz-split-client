package com.quare.blitzsplit.core.model

/*
 * Why: every stack of faces in the app (a group's members, who paid a bill, who is in a split) shows at most two
 * bubbles, the same on the three UIs (beta feedback): one or two people show their faces; three or more show the first
 * face and a second bubble "+N" for the rest (three people: a face and "+2").
 */
object FaceStack {
    const val MAX_BUBBLES = 2

    // Why: how many of the first people show their face.
    fun faceCountOf(peopleCount: Int): Int = when {
        peopleCount <= 0 -> 0
        peopleCount <= MAX_BUBBLES -> peopleCount
        else -> 1
    }

    // Why: the "+N" bubble's N, 0 for no bubble.
    fun extraCountOf(peopleCount: Int): Int = (peopleCount - faceCountOf(peopleCount)).coerceAtLeast(0)
}

// Why: the first people whose faces a stack shows; the rest is FaceStack.extraCountOf.
fun <T> List<T>.toStackedFaces(): List<T> = take(FaceStack.faceCountOf(size))
