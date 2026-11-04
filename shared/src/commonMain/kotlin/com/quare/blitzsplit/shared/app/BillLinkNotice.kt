package com.quare.blitzsplit.shared.app

// Why: why a comment's link opened nothing, which each platform says in its words, with "Ir para o início".
enum class BillLinkNotice {
    // Why: "Você não está neste grupo": a group the person isn't in, or a device with no profile yet.
    NOT_IN_GROUP,

    // Why: the bill isn't on the device and the server couldn't be asked: "Sem internet. Abra o link de novo quando reconectar."
    OFFLINE,
}

// Why: the notice shown, null for none, in a holder the platforms' watchers take.
data class BillLinkNoticeState(
    val notice: BillLinkNotice?,
)
