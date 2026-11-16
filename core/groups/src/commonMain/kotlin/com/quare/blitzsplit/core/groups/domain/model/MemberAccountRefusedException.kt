package com.quare.blitzsplit.core.groups.domain.model

/*
 * Why: the server refused the members' upload over an account it no longer lets this user link (deleted, or no group
 * shared with them any more); nothing of the upload went in.
 */
class MemberAccountRefusedException : Exception("member_account_refused")
