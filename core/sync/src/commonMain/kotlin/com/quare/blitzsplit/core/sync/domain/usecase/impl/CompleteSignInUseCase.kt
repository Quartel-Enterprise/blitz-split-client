package com.quare.blitzsplit.core.sync.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.profile.domain.repository.ProfileRepository
import com.quare.blitzsplit.core.sync.domain.model.SignInOutcome
import com.quare.blitzsplit.core.sync.domain.usecase.CompleteSignIn
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.first

class CompleteSignInUseCase(
    private val accountRepository: AccountRepository,
    private val profileRepository: ProfileRepository,
) : CompleteSignIn {
    override suspend fun invoke(): Result<SignInOutcome> = suspendRunCatching {
        accountRepository.getCurrentUserOrNull()?.let { user ->
            profileRepository.downloadProfile(user.userId).getOrThrow()
        }
        val hasProfile = profileRepository.observeProfile().first() != null
        if (hasProfile) SignInOutcome.PROFILE_RESTORED else SignInOutcome.NEEDS_PROFILE
    }
}
