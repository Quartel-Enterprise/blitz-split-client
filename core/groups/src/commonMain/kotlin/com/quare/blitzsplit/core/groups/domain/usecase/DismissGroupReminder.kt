package com.quare.blitzsplit.core.groups.domain.usecase

// Why: "Dispensar" on the group's reminder banner: this device stops showing that occurrence's, the next one's comes back.
fun interface DismissGroupReminder {
    suspend operator fun invoke(
        groupId: String,
        occurrenceAt: Long,
    ): Result<Unit>
}
