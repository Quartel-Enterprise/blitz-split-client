package com.quare.blitzsplit.feature.billdetail.presentation.viewmodel

/*
 * Why: "Editar" puts a comment's text in the composer, in Conta and on a bill's link page alike; what was typed before,
 * and the reply it answered, come back once the edit ends.
 */
internal class CommentComposerEdit {
    private var before = CommentComposer(
        editingCommentId = null,
        draft = "",
        replyTarget = null,
    )

    // Why: a second Editar while editing keeps what was typed before the first.
    fun start(
        commentId: String,
        body: String,
        composer: CommentComposer,
    ): CommentComposer {
        if (composer.editingCommentId == null) before = composer
        return CommentComposer(
            editingCommentId = commentId,
            draft = body,
            replyTarget = null,
        )
    }

    fun stop(): CommentComposer = before.also {
        before = CommentComposer(
            editingCommentId = null,
            draft = "",
            replyTarget = null,
        )
    }
}
