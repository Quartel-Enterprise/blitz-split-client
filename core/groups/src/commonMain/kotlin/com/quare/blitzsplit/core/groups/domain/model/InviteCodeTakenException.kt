package com.quare.blitzsplit.core.groups.domain.model

// Why: the server refused a new group's invite code, which another group already has; nothing of the upload went in.
class InviteCodeTakenException : Exception("invite_code_taken")
