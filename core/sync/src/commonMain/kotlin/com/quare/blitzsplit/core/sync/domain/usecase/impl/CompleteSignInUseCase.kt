package com.quare.blitzsplit.core.sync.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.profile.domain.repository.ProfileRepository
import com.quare.blitzsplit.core.sync.FirstDownload
import com.quare.blitzsplit.core.sync.domain.model.SignInOutcome
import com.quare.blitzsplit.core.sync.domain.usecase.CompleteSignIn
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.first

class CompleteSignInUseCase(
    private val accountRepository: AccountRepository,
    private val profileRepository: ProfileRepository,
    private val firstDownload: FirstDownload,
) : CompleteSignIn {
    /*
     * Why: a device with no profile yet that gets one from the server is filled by its next sync, so the first download
     * starts before the profile reaches Room, and ends here when no profile came.
     */
    override suspend fun invoke(): Result<SignInOutcome> {
        var isNewDevice = false
        return suspendRunCatching {
            isNewDevice = profileRepository.observeProfile().first() == null
            if (isNewDevice) firstDownload.start()
            accountRepository.getCurrentUserOrNull()?.let { user ->
                profileRepository.downloadProfile(user.userId).getOrThrow()
            }
            val hasProfile = profileRepository.observeProfile().first() != null
            if (hasProfile) SignInOutcome.PROFILE_RESTORED else SignInOutcome.NEEDS_PROFILE
        }.also { result ->
            if (isNewDevice && result.getOrNull() != SignInOutcome.PROFILE_RESTORED) firstDownload.end()
        }
    }
}
