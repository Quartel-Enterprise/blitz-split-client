package com.quare.blitzsplit.core.expenses.export

data class ExportMember(
    val id: String,
    // Why: null for someone who left the group, whom the export calls by `GroupExportWords.formerMember`.
    val name: String?,
    val isMe: Boolean,
    val hasJoined: Boolean,
)
