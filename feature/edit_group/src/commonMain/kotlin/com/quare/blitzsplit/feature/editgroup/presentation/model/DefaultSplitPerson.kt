package com.quare.blitzsplit.feature.editgroup.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

// Why: a member of the group as "Divisão padrão" lists them, with what each way to split holds for them.
data class DefaultSplitPerson(
    val id: String,
    val name: String,
    val initials: String,
    val avatarColor: AvatarColor,
    // Why: "Pierre (você)".
    val isMe: Boolean,
    // Why: joined after the default was saved ("Carla entrou no grupo"), until someone saves it again.
    val isNew: Boolean,
    // Why: "Igual"'s "Quem entra na divisão".
    val isIncluded: Boolean,
    // Why: "Partes"' stepper.
    val parts: Long,
    // Why: "Porcentagem"'s field as typed, a whole number.
    val percentText: String,
    // Why: "Pela renda"'s monthly income as typed, "9.000,00", which only this device keeps.
    val incomeText: String,
)
