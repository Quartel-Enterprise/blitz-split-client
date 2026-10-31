package com.quare.blitzsplit.core.groups.domain.model

// Why: the server refused to change the invite link, since only the group's creator may; nothing changed there.
class NotGroupCreatorException : Exception("not_group_creator")
