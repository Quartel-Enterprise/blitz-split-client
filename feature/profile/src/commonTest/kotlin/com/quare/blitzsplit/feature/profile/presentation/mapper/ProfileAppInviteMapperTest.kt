package com.quare.blitzsplit.feature.profile.presentation.mapper

import com.quare.blitzsplit.core.model.StoreListing
import com.quare.blitzsplit.core.profile.domain.model.AppInvite
import com.quare.blitzsplit.core.profile.domain.model.ClientPlatform
import com.quare.blitzsplit.core.profile.domain.model.PhoneUse
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileAppInvite
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileAppInviteStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileAppInviteMapperTest {
    @Test
    fun `GIVEN an invite neither dismissed nor in use WHEN mapping it THEN the row is highlighted`() {
        // Given
        val invite = invite()

        // When
        val row = toProfileAppInvite(invite)

        // Then
        assertEquals(
            ProfileAppInvite(
                status = ProfileAppInviteStatus.ACTIVE,
                link = AppInvite.WEB_LINK,
                usesIphone = false,
            ),
            row,
        )
    }

    @Test
    fun `GIVEN a dismissed invite and no phone use WHEN mapping it THEN the row is plain`() {
        // Given
        val invite = invite(isDismissed = true)

        // When
        val row = toProfileAppInvite(invite)

        // Then
        assertEquals(
            ProfileAppInvite(
                status = ProfileAppInviteStatus.DISMISSED,
                link = AppInvite.WEB_LINK,
                usesIphone = false,
            ),
            row,
        )
    }

    @Test
    fun `GIVEN the account on the Android app WHEN mapping the invite THEN the row says it is in use on a phone`() {
        // Given
        val invite = invite(phoneUse = phoneUse(ClientPlatform.ANDROID))

        // When
        val row = toProfileAppInvite(invite)

        // Then
        assertEquals(
            ProfileAppInvite(
                status = ProfileAppInviteStatus.IN_USE,
                link = AppInvite.WEB_LINK,
                usesIphone = false,
            ),
            row,
        )
    }

    @Test
    fun `GIVEN a dismissed invite and the account on a phone WHEN mapping it THEN the phone use wins`() {
        // Given
        val invite = invite(
            isDismissed = true,
            phoneUse = phoneUse(ClientPlatform.ANDROID),
        )

        // When
        val row = toProfileAppInvite(invite)

        // Then
        assertEquals(ProfileAppInviteStatus.IN_USE, row.status)
    }

    @Test
    fun `GIVEN the account on the iPhone app WHEN mapping the invite THEN the row says iPhone`() {
        // Given
        val invite = invite(phoneUse = phoneUse(ClientPlatform.IOS))

        // When
        val row = toProfileAppInvite(invite)

        // Then
        assertEquals(
            ProfileAppInvite(
                status = ProfileAppInviteStatus.IN_USE,
                link = AppInvite.WEB_LINK,
                usesIphone = true,
            ),
            row,
        )
    }

    @Test
    fun `GIVEN the account on both Android and the iPhone WHEN mapping the invite THEN the row says iPhone`() {
        // Given
        val invite = invite(phoneUse = phoneUse(ClientPlatform.ANDROID, ClientPlatform.IOS))

        // When
        val row = toProfileAppInvite(invite)

        // Then
        assertTrue(row.usesIphone)
    }

    private fun invite(
        isDismissed: Boolean = false,
        phoneUse: PhoneUse? = null,
    ): AppInvite = AppInvite(
        link = AppInvite.WEB_LINK,
        stores = StoreListing.BOTH,
        isDismissed = isDismissed,
        phoneUse = phoneUse,
    )

    private fun phoneUse(vararg platforms: ClientPlatform): PhoneUse = PhoneUse(
        platforms = platforms.toSet(),
        sinceMillis = 1_760_270_400_000L,
    )
}
