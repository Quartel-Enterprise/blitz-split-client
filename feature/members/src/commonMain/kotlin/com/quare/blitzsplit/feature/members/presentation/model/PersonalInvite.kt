package com.quare.blitzsplit.feature.members.presentation.model

// Why: the "Convite pessoal para Carla" sheet, a link that joins straight in as that member and works once.
data class PersonalInvite(
    val member: MembersMember,
    // Why: the whole link, which copying and sharing send, with the code of whoever shares it.
    val link: String,
    // Why: "blitzsplit.com/g/7Hk2pQ8x/c3Rt9z", the link as the sheet shows it.
    val displayLink: String,
    // Why: "Copiado" for two seconds after "Copiar".
    val isLinkCopied: Boolean,
)
