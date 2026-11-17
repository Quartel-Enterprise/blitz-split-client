package com.quare.blitzsplit.core.groups.domain.model

// Why: how often a group's "Lembrete de acerto" fires, by the code the server keeps in groups.reminder_kind.
enum class ReminderKind(
    val code: String,
) {
    WEEKLY("weekly"),
    MONTHLY("monthly"),
    ;

    companion object {
        fun fromCodeOrNull(code: String?): ReminderKind? = entries.firstOrNull { it.code == code }
    }
}
