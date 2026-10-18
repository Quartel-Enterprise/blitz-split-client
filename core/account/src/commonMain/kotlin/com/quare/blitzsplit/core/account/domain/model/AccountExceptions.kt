package com.quare.blitzsplit.core.account.domain.model

class InvalidRecoveryCodeException : Exception("invalid_code")

class TooManyRecoveryAttemptsException : Exception("too_many_attempts")

class IdentityAlreadyLinkedException : Exception("identity_already_exists")

class SocialAccountHasNoRecoveryCodeException : Exception("has_social_account")
