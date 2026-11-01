package com.quare.blitzsplit.core.contacts.domain.model

// Why: "Adicionar a um grupo" left a guest out of a group where another member already has their name.
class MemberNameTakenException(
    name: String,
) : IllegalStateException("A member of the group is already called $name")
